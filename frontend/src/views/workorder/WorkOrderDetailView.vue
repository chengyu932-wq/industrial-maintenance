<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '../../api/workorder'
import * as inventoryApi from '../../api/inventory'
import * as maintenanceApi from '../../api/maintenance'
import * as knowledgeApi from '../../api/knowledge'
import { useAuthStore } from '../../stores/auth'
import { priorityLabel, priorityType, workOrderStatusLabel, workOrderStatusType, workOrderStep } from '../../utils/workOrderStatus'
import { issueStatusLabel } from '../../utils/inventoryStatus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = shallowRef(true)
const actionLoading = shallowRef(false)
const detail = shallowRef<any>(null)
const maintenanceDetail = shallowRef<any>(null)
const engineers = shallowRef<any[]>([])
const recommendations = shallowRef<any[]>([])
const similarOrders = shallowRef<any[]>([])
const attachments = shallowRef<any[]>([])
const attachmentInput = ref<HTMLInputElement>()
const attachmentType = shallowRef('REPAIR')
const similarLoading = shallowRef(false)
const warehouseOptions = shallowRef<any[]>([])
const spareOptions = shallowRef<any[]>([])
const dispatchDialog = reactive<any>({ visible: false, engineerId: null, teamId: null, reason: '' })
const recordDialog = reactive({ visible: false })
const cancelDialog = reactive<any>({ visible: false, reason: '', equipmentTargetStatus: 'RUNNING' })
const record = reactive<any>({ inspectionProcess: '', rootCause: '', repairAction: '', repairResult: '', laborHours: 0, downtimeMinutes: 0, repairable: true })
const spareDialog = reactive<any>({ visible: false, warehouseId: null, sparePartId: null, qty: 1, remark: '' })
const returnDialog = reactive<any>({ visible: false, issue: null, qty: 1, remark: '' })
const order = computed(() => detail.value?.workOrder)
const isMaintenance = computed(() => order.value?.workOrderType === 'MAINTENANCE')
const canProcess = computed(() => auth.hasPermission('workorder:process') && order.value?.assignedEngineerId === auth.currentUser?.userId)
const canIssueSpare = computed(() => !isMaintenance.value && auth.hasPermission('inventory:issue') && canProcess.value && order.value?.status === 'PROCESSING')
const activeStep = computed(() => workOrderStep(order.value?.status))
const isExceptionState = computed(() => ['SUSPENDED', 'CANCELLED'].includes(order.value?.status))
const slaState = computed(() => {
  if (isMaintenance.value || !order.value?.slaRuleId) return null
  const now = Date.now(); const response = order.value.slaResponseDeadline ? new Date(order.value.slaResponseDeadline).getTime() : null
  const resolve = order.value.slaResolveDeadline ? new Date(order.value.slaResolveDeadline).getTime() : null
  return { responseOverdue: !order.value.acceptedAt && response !== null && now > response, resolveOverdue: !['COMPLETED','CANCELLED'].includes(order.value.status) && resolve !== null && now > resolve }
})

async function load(showLoading = true) {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) return
  if (showLoading) loading.value = true
  try {
    const fresh = (await api.detail(id)).data
    if (!showLoading) { detail.value = null; await nextTick() }
    detail.value = fresh
    attachments.value = (await api.attachments(id)).data
    attachmentType.value = fresh.workOrder?.status === 'PENDING_ACCEPT' ? 'ACCEPTANCE' : fresh.workOrder?.status === 'PENDING_ASSIGN' ? 'FAULT' : 'REPAIR'
    maintenanceDetail.value = fresh.workOrder?.workOrderType === 'MAINTENANCE' ? (await maintenanceApi.workOrder(id)).data : null
    if (fresh.workOrder?.workOrderType === 'REPAIR' && auth.hasPermission('workorder:similar')) await loadSimilar(fresh.workOrder.id)
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '工单详情加载失败') }
  finally { if (showLoading) loading.value = false }
}
async function loadSimilar(id:number){similarLoading.value=true;try{similarOrders.value=(await api.similar(id)).data}catch(e:any){similarOrders.value=[];ElMessage.error(e.response?.data?.message||'历史相似工单加载失败')}finally{similarLoading.value=false}}
async function exportPdf(){actionLoading.value=true;try{const blob=await api.exportPdf(order.value.id);const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=`${order.value.workOrderNo}.pdf`;link.click();URL.revokeObjectURL(url);ElMessage.success('工单 PDF 已导出')}catch(e:any){ElMessage.error(e.response?.data?.message||'PDF 导出失败')}finally{actionLoading.value=false}}
async function uploadAttachment(event:Event){const input=event.target as HTMLInputElement;const file=input.files?.[0];input.value='';if(!file)return;actionLoading.value=true;try{await api.uploadAttachment(order.value.id,attachmentType.value,file);ElMessage.success('工单附件已上传');attachments.value=(await api.attachments(order.value.id)).data}catch(e:any){ElMessage.error(e.response?.data?.message||'附件上传失败')}finally{actionLoading.value=false}}
async function downloadAttachment(item:any){try{const blob=await api.downloadAttachment(order.value.id,item.id);const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=item.fileName;link.click();URL.revokeObjectURL(url)}catch(e:any){ElMessage.error(e.response?.data?.message||'附件下载失败')}}
async function createKnowledge(){actionLoading.value=true;try{const response=await knowledgeApi.createFromWorkOrder(order.value.id);ElMessage.success('知识草稿已按维修记录预填');await router.push({path:'/knowledge',query:{article:response.data}})}catch(e:any){ElMessage.error(e.response?.data?.message||'知识草稿创建失败')}finally{actionLoading.value=false}}
async function perform(action: () => Promise<any>, message: string) {
  if (actionLoading.value) return false
  actionLoading.value = true
  try { await action(); ElMessage.success(message); await load(false); return true }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败'); return false }
  finally { actionLoading.value = false }
}
async function openDispatch() {
  actionLoading.value = true
  try { const requests:any[]=[api.engineers(order.value.id)];if(!isMaintenance.value)requests.push(api.dispatchCandidates(order.value.id));const result=await Promise.all(requests);engineers.value=result[0].data;recommendations.value=result[1]?.data||[]; Object.assign(dispatchDialog, { visible: true, engineerId: null, teamId: null, reason: '' }) }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '工程师列表加载失败') }
  finally { actionLoading.value = false }
}
function engineerChanged(id: number) { const item = engineers.value.find(i => i.id === id); dispatchDialog.teamId = item?.teamId || null }
function selectRecommendation(item:any){dispatchDialog.engineerId=item.engineerId;dispatchDialog.teamId=item.teamId;dispatchDialog.reason=`采纳智能推荐（综合评分 ${Number(item.totalScore*100).toFixed(1)}）`}
async function assign() { if (!dispatchDialog.engineerId) return ElMessage.warning('请选择工程师'); if (await perform(() => api.assign(order.value.id, { engineerId: dispatchDialog.engineerId, teamId: dispatchDialog.teamId, reason: dispatchDialog.reason }), '派单成功')) dispatchDialog.visible = false }
async function reasonAction(kind: 'suspend' | 'resume') {
  try { const { value } = await ElMessageBox.prompt(kind === 'suspend' ? '请输入挂起原因' : '请输入恢复处理说明', kind === 'suspend' ? '挂起工单' : '恢复工单', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空', type: 'warning' }); await perform(() => api[kind](order.value.id, { reason: value }), kind === 'suspend' ? '工单已挂起' : '工单已恢复处理') }
  catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败') }
}
function openRecord() { Object.assign(record, detail.value.repairRecord || { inspectionProcess: '', rootCause: '', repairAction: '', repairResult: '', laborHours: 0, downtimeMinutes: 0, repairable: true }); recordDialog.visible = true }
async function saveRecord() { if (!record.inspectionProcess.trim() || !record.rootCause.trim() || !record.repairAction.trim() || !record.repairResult.trim()) return ElMessage.warning('请完整填写维修过程'); if (await perform(() => api.saveRepairRecord(order.value.id, record), '维修记录已保存')) recordDialog.visible = false }
async function saveMaintenance(){const completed=maintenanceDetail.value.items.filter((x:any)=>x.result);if(!completed.length)return ElMessage.warning('请至少填写一个检查结果');await perform(()=>maintenanceApi.saveExecution(order.value.id,completed.map((x:any)=>({planItemId:x.planItemId,result:x.result,measuredValue:x.measuredValue,remark:x.remark}))),'保养检查结果已保存')}
async function submit() { try { await ElMessageBox.confirm(isMaintenance.value?'确认必检项目已完成并提交验收？':'确认维修记录完整并提交验收？提交后将等待报修人或主管验收。', '提交验收', { type: 'warning' }); await perform(() => api.submitAcceptance(order.value.id), '已提交验收') } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '提交失败') } }
async function pass() { try { const { value } = await ElMessageBox.prompt('可填写验收意见', '验收通过', { inputValue: isMaintenance.value?'检查项目执行完整':'试运行正常', type: 'success' }); await perform(() => api.pass(order.value.id, { remark: value }), isMaintenance.value?'保养验收通过':'验收通过，设备已恢复运行') } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '验收失败') } }
async function reject() { try { const { value } = await ElMessageBox.prompt('请输入验收不通过原因', isMaintenance.value?'退回整改':'退回维修', { inputPattern: /\S+/, inputErrorMessage: '退回原因不能为空', type: 'warning' }); await perform(() => api.reject(order.value.id, { reason: value }), '已退回工程师继续处理') } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '退回失败') } }
async function cancelOrder() { if (!cancelDialog.reason.trim()) return ElMessage.warning('请输入取消原因'); if (await perform(() => api.cancel(order.value.id, { reason: cancelDialog.reason, equipmentTargetStatus: cancelDialog.equipmentTargetStatus }), '工单已取消')) cancelDialog.visible = false }
async function openSpareDialog() { try { const [w,p]=await Promise.all([inventoryApi.warehouses({status:'ENABLED',page:1,size:100}),inventoryApi.spareParts({status:'ENABLED',page:1,size:100})]);warehouseOptions.value=w.data.records;spareOptions.value=p.data.records;Object.assign(spareDialog,{visible:true,warehouseId:null,sparePartId:null,qty:1,remark:''}) } catch(e:any){ElMessage.error(e.response?.data?.message||'领用选项加载失败')} }
async function issueSpare(){if(!spareDialog.warehouseId||!spareDialog.sparePartId||spareDialog.qty<=0)return ElMessage.warning('请选择仓库、备件并填写领用数量');if(await perform(()=>api.issueSpare(order.value.id,{warehouseId:spareDialog.warehouseId,sparePartId:spareDialog.sparePartId,qty:spareDialog.qty,remark:spareDialog.remark}),'备件领用成功，库存已扣减'))spareDialog.visible=false}
function openReturnDialog(issue:any){Object.assign(returnDialog,{visible:true,issue,qty:Number(issue.usedQty),remark:''})}
async function returnSpare(){if(!returnDialog.remark.trim())return ElMessage.warning('请输入退库原因');if(returnDialog.qty<=0||returnDialog.qty>Number(returnDialog.issue.usedQty))return ElMessage.warning('退库数量不能超过尚可退数量');if(await perform(()=>api.returnSpare(order.value.id,returnDialog.issue.id,{qty:returnDialog.qty,remark:returnDialog.remark}),'退库成功，库存已恢复'))returnDialog.visible=false}
const actionLabels: any = { CREATE: '创建工单', ASSIGN: '人工派单', ACCEPT: '工程师接单', START: '开始处理', SUSPEND: '挂起', RESUME: '恢复', SUBMIT: '提交验收', ACCEPT_PASS: '验收通过', ACCEPT_RETURN: '验收退回', CANCEL: '取消' }
function actionLabel(action:string){return action==='START'?(isMaintenance.value?'开始保养':'开始维修'):(actionLabels[action]||action)}
onMounted(load)
watch(()=>route.params.id,()=>load())
</script>

<template>
  <section class="module-page" v-loading="loading">
    <header class="module-header">
      <div><p class="eyebrow">WORK ORDER DETAIL</p><h1>{{ order?.workOrderNo || '工单详情' }}</h1><p v-if="order">{{ order.equipmentName }} · {{ order.workshopName }}</p></div>
      <div class="header-actions"><el-button @click="router.push('/work-orders')">返回列表</el-button><el-button v-if="order" :loading="actionLoading" @click="exportPdf">导出 PDF</el-button><el-button v-if="order?.status === 'PENDING_ASSIGN'" v-permission="'workorder:assign'" type="primary" :loading="actionLoading" @click="openDispatch">{{isMaintenance?'人工派单':'智能推荐派单'}}</el-button><el-button v-if="['PENDING_ASSIGN', 'ASSIGNED'].includes(order?.status)" v-permission="'workorder:cancel'" type="danger" plain :disabled="actionLoading" @click="cancelDialog.visible = true">取消工单</el-button></div>
    </header>

    <template v-if="order">
      <el-card shadow="never" class="summary-card">
        <div class="summary-grid">
          <div class="summary-item"><small>当前状态</small><el-tag :type="workOrderStatusType(order.status)">{{ workOrderStatusLabel(order.status) }}</el-tag></div>
          <div class="summary-item"><small>工单类型</small><el-tag :type="isMaintenance?'success':'primary'" effect="plain">{{ isMaintenance ? '保养工单' : '维修工单' }}</el-tag></div>
          <div class="summary-item"><small>设备</small><strong :title="`${order.equipmentNo} · ${order.equipmentName}`">{{ order.equipmentName }}</strong></div>
          <div class="summary-item"><small>当前负责人</small><strong>{{ order.assignedEngineerName || '等待主管派单' }}</strong></div>
          <div class="summary-item"><small>创建时间</small><strong>{{ order.createdAt || detail.repairRequest?.reportedAt || '-' }}</strong></div>
          <div class="summary-item"><small>下一步</small><strong>{{ order.status === 'PENDING_ASSIGN' ? '主管派单' : order.status === 'ASSIGNED' ? `工程师开始${isMaintenance?'保养':'维修'}` : order.status === 'PROCESSING' ? '提交验收' : order.status === 'PENDING_ACCEPT' ? '主管验收' : order.status === 'SUSPENDED' ? '工程师恢复处理' : '流程已结束' }}</strong></div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header><div class="card-heading"><div class="card-title"><strong>处理进度</strong><small>主流程进度；异常状态单独标识</small></div><el-tag v-if="isExceptionState" :type="workOrderStatusType(order.status)">{{ workOrderStatusLabel(order.status) }}</el-tag></div></template>
        <div class="workflow-steps"><el-steps :active="activeStep" finish-status="success" align-center><el-step title="待派单"/><el-step title="已派单"/><el-step title="处理中"/><el-step title="待验收"/><el-step title="已完成"/></el-steps></div>
      </el-card>

      <el-card v-if="!isMaintenance" shadow="never" class="sla-card">
        <template #header><div class="card-heading"><div class="card-title"><strong>SLA 时限</strong><small>从工单创建时间起连续计时，挂起不暂停</small></div><el-tag v-if="slaState" :type="slaState.responseOverdue||slaState.resolveOverdue?'danger':'success'">{{slaState.responseOverdue||slaState.resolveOverdue?'已超时':'计时中'}}</el-tag></div></template>
        <el-descriptions :column="2" border><el-descriptions-item label="响应截止">{{order.slaResponseDeadline||'未匹配规则'}}</el-descriptions-item><el-descriptions-item label="实际响应">{{order.acceptedAt||'尚未接单'}}</el-descriptions-item><el-descriptions-item label="解决截止">{{order.slaResolveDeadline||'未匹配规则'}}</el-descriptions-item><el-descriptions-item label="实际解决">{{order.completedAt||'尚未完成'}}</el-descriptions-item></el-descriptions>
      </el-card>

      <el-card v-if="canProcess && ['ASSIGNED', 'PROCESSING', 'SUSPENDED'].includes(order.status)" shadow="never" class="action-card">
        <template #header><div class="card-title"><strong>{{isMaintenance?'保养执行':'维修处理'}}</strong><small>仅显示当前状态允许的操作</small></div></template>
        <div class="workflow-actions"><el-button v-if="order.status === 'ASSIGNED' && !order.acceptedAt" :loading="actionLoading" @click="perform(() => api.acceptResponse(order.id), '接单成功')">确认接单</el-button><el-button v-if="order.status === 'ASSIGNED'" type="primary" :loading="actionLoading" @click="perform(() => api.start(order.id), `已开始${isMaintenance?'保养':'维修'}`)">开始{{isMaintenance?'保养':'维修'}}</el-button><el-button v-if="order.status === 'PROCESSING'&&!isMaintenance" :disabled="actionLoading" @click="openRecord">填写维修记录</el-button><el-button v-if="order.status === 'PROCESSING'&&isMaintenance" :loading="actionLoading" @click="saveMaintenance">保存检查结果</el-button><el-button v-if="canIssueSpare" type="primary" plain :disabled="actionLoading" @click="openSpareDialog">领用备件</el-button><el-button v-if="order.status === 'PROCESSING'" type="warning" plain :disabled="actionLoading" @click="reasonAction('suspend')">挂起</el-button><el-button v-if="order.status === 'SUSPENDED'" type="primary" :loading="actionLoading" @click="reasonAction('resume')">恢复处理</el-button><el-button v-if="order.status === 'PROCESSING'" type="primary" :loading="actionLoading" @click="submit">提交验收</el-button></div>
      </el-card>

      <el-card v-if="order.status === 'PENDING_ACCEPT' && auth.hasPermission('workorder:accept')" shadow="never" class="action-card">
        <template #header><div class="card-title"><strong>{{isMaintenance?'保养验收':'维修验收'}}</strong><small>核对执行结果与检查记录</small></div></template>
        <p>{{isMaintenance?'验收通过后形成设备保养履历；退回时工单重新进入处理中。':'验收通过后工单完成，设备恢复运行；退回维修必须填写原因。'}}</p>
        <div class="workflow-actions"><el-button type="primary" :loading="actionLoading" @click="pass">验收通过</el-button><el-button type="danger" plain :disabled="actionLoading" @click="reject">{{isMaintenance?'退回整改':'退回维修'}}</el-button></div>
      </el-card>

      <el-card v-if="!isMaintenance&&order.status==='COMPLETED'&&auth.hasPermission('knowledge:create')" shadow="never" class="action-card"><template #header><div class="card-title"><strong>沉淀维修知识</strong><small>将本次故障现象、根因、处理方法、结果和实际备件生成草稿</small></div></template><p>工单已完成。知识不会自动发布，创建草稿后需人工检查并提交主管审核。</p><el-button type="primary" :loading="actionLoading" @click="createKnowledge">创建知识草稿</el-button></el-card>

      <el-card v-if="!isMaintenance&&auth.hasPermission('workorder:similar')" shadow="never"><template #header><div class="card-heading"><div class="card-title"><strong>历史相似工单</strong><small>基于故障描述的 TF-IDF 与余弦相似度，仅供维修经验参考</small></div><el-tag effect="plain">Top {{similarOrders.length}}</el-tag></div></template><div v-loading="similarLoading"><div v-if="similarOrders.length" class="similar-list"><div v-for="item in similarOrders" :key="item.workOrderId" class="similar-item"><span class="similar-score">{{(item.similarity*100).toFixed(1)}}<small>%</small></span><span class="similar-copy"><strong>{{item.workOrderNo}} · {{item.equipmentName}}</strong><span>{{item.faultDescription}}</span><small>原因：{{item.rootCause||'未记录'}} · 处理：{{item.repairAction||'未记录'}} · 结果：{{item.repairResult||'未记录'}}</small><small>备件：{{item.spareSummary||'未使用备件'}}</small></span><el-button link type="primary" @click="router.push(`/work-orders/${item.workOrderId}`)">查看工单</el-button></div></div><el-empty v-else description="暂无正相似度的历史完成维修工单" :image-size="72"/></div></el-card>

      <div class="detail-grid">
        <div class="section-stack">
          <el-card v-if="!isMaintenance" shadow="never"><template #header><div class="card-title"><strong>报修信息</strong><small>故障现象与来源</small></div></template><el-descriptions :column="2" border><el-descriptions-item label="报修人">{{ order.reporterName }}</el-descriptions-item><el-descriptions-item label="报修时间">{{ detail.repairRequest?.reportedAt || '-' }}</el-descriptions-item><el-descriptions-item label="故障描述" :span="2">{{ detail.repairRequest?.faultDescription || '-' }}</el-descriptions-item></el-descriptions></el-card>
          <el-card v-else shadow="never"><template #header><div class="card-title"><strong>保养计划</strong><small>计划来源与逐项检查要求</small></div></template><el-descriptions :column="2" border><el-descriptions-item label="计划编号">{{maintenanceDetail?.planNo}}</el-descriptions-item><el-descriptions-item label="计划名称">{{maintenanceDetail?.planName}}</el-descriptions-item></el-descriptions><div class="maintenance-checklist"><div v-for="item in maintenanceDetail?.items" :key="item.planItemId" class="maintenance-check"><div class="maintenance-check-title"><strong>{{item.itemName}} <em v-if="item.required">必检</em></strong><small>{{item.standardDescription||'无额外检查标准'}}</small></div><el-select v-model="item.result" :disabled="order.status!=='PROCESSING'||!canProcess" placeholder="检查结果"><el-option label="正常" value="NORMAL"/><el-option label="异常" value="ABNORMAL"/></el-select><el-input v-model="item.measuredValue" :disabled="order.status!=='PROCESSING'||!canProcess" placeholder="实测值（可选）"/><el-input v-model="item.remark" :disabled="order.status!=='PROCESSING'||!canProcess" placeholder="执行说明（可选）"/></div></div></el-card>
          <el-card shadow="never"><template #header><div class="card-heading"><div class="card-title"><strong>工单附件</strong><small>故障、维修与验收现场资料，单文件不超过 10MB</small></div><div v-if="auth.hasPermission('workorder:process')||auth.hasPermission('workorder:accept')"><input ref="attachmentInput" type="file" accept=".png,.jpg,.jpeg,.pdf,.doc,.docx,.xls,.xlsx,.txt" hidden @change="uploadAttachment"><el-select v-model="attachmentType" size="small" style="width:110px"><el-option label="故障资料" value="FAULT"/><el-option label="维修资料" value="REPAIR"/><el-option label="验收资料" value="ACCEPTANCE"/></el-select><el-button size="small" :loading="actionLoading" @click="attachmentInput?.click()">上传附件</el-button></div></div></template><el-table v-if="attachments.length" :data="attachments" size="small"><el-table-column prop="category" label="阶段" width="90"><template #default="{row}">{{row.category==='FAULT'?'故障':row.category==='REPAIR'?'维修':'验收'}}</template></el-table-column><el-table-column prop="fileName" label="文件名" min-width="180"/><el-table-column prop="uploaderName" label="上传人" width="100"/><el-table-column prop="createdAt" label="上传时间" width="168"/><el-table-column label="操作" width="70"><template #default="{row}"><el-button link type="primary" @click="downloadAttachment(row)">下载</el-button></template></el-table-column></el-table><el-empty v-else class="detail-empty" description="暂无工单附件" :image-size="64"/></el-card>
          <el-card shadow="never"><template #header><div class="card-title"><strong>设备与责任信息</strong><small>定位设备及当前处理人员</small></div></template><el-descriptions :column="2" border><el-descriptions-item label="设备编号">{{ order.equipmentNo }}</el-descriptions-item><el-descriptions-item label="设备名称">{{ order.equipmentName }}</el-descriptions-item><el-descriptions-item label="所属车间">{{ order.workshopName || '-' }}</el-descriptions-item><el-descriptions-item label="负责班组">{{ order.assignedTeamName || '-' }}</el-descriptions-item><el-descriptions-item label="维修工程师">{{ order.assignedEngineerName || '待派单' }}</el-descriptions-item><el-descriptions-item label="退回次数">{{ order.acceptanceReturnCount }}</el-descriptions-item></el-descriptions></el-card>
          <el-card v-if="!isMaintenance" shadow="never"><template #header><div class="card-title"><strong>维修过程</strong><small>排查、根因、措施与结果</small></div></template><el-descriptions v-if="detail.repairRecord" :column="1" border><el-descriptions-item label="排查过程">{{ detail.repairRecord.inspectionProcess || '-' }}</el-descriptions-item><el-descriptions-item label="根因分析">{{ detail.repairRecord.rootCause || '-' }}</el-descriptions-item><el-descriptions-item label="处理措施">{{ detail.repairRecord.repairAction || '-' }}</el-descriptions-item><el-descriptions-item label="维修结果">{{ detail.repairRecord.repairResult || '-' }}</el-descriptions-item><el-descriptions-item label="维修耗时">{{ detail.repairRecord.laborHours }} 小时 · 停机 {{ detail.repairRecord.downtimeMinutes }} 分钟</el-descriptions-item></el-descriptions><el-empty v-else class="detail-empty" description="暂无维修记录" :image-size="72"/></el-card>
          <el-card v-if="!isMaintenance" shadow="never"><template #header><div class="card-heading"><div class="card-title"><strong>备件使用</strong><small>领用、退库和实际使用记录</small></div><el-button v-if="canIssueSpare" link type="primary" @click="openSpareDialog">领用备件</el-button></div></template><el-table v-if="detail.spares?.length" :data="detail.spares" size="small"><el-table-column prop="spareNo" label="编号" min-width="110"/><el-table-column prop="spareName" label="备件" min-width="120"/><el-table-column prop="specification" label="规格" min-width="110"/><el-table-column prop="warehouseName" label="仓库" min-width="110"/><el-table-column prop="issuedQty" label="领用" width="70" align="right"/><el-table-column prop="returnedQty" label="退回" width="70" align="right"/><el-table-column prop="usedQty" label="实际使用" width="80" align="right"/><el-table-column label="状态" width="90"><template #default="{row}"><el-tag size="small" :type="row.status==='RETURNED'?'info':row.status==='PARTIAL_RETURN'?'warning':'success'">{{issueStatusLabel(row.status)}}</el-tag></template></el-table-column><el-table-column v-if="canIssueSpare&&auth.hasPermission('inventory:return')" label="操作" width="70"><template #default="{row}"><el-button v-if="Number(row.usedQty)>0" link type="primary" @click="openReturnDialog(row)">退库</el-button></template></el-table-column></el-table><el-empty v-else class="detail-empty" description="本工单尚未领用备件" :image-size="72"/></el-card>
        </div>
        <el-card shadow="never"><template #header><div class="card-title"><strong>流转记录</strong><small>完整记录状态变化与操作人</small></div></template><el-timeline v-if="detail.flows?.length"><el-timeline-item v-for="item in detail.flows" :key="item.id" :timestamp="item.createdAt" placement="top" :type="item.toStatus === 'COMPLETED' ? 'success' : item.toStatus === 'CANCELLED' ? 'info' : 'primary'"><strong>{{ actionLabel(item.action) }}</strong><p>{{ item.fromStatus ? workOrderStatusLabel(item.fromStatus) : '流程开始' }} → {{ workOrderStatusLabel(item.toStatus) }}</p><span class="timeline-meta">{{ item.operatorName || `用户 ${item.operatorId}` }} · {{ item.remark || '无备注' }}</span></el-timeline-item></el-timeline><el-empty v-else description="暂无流转记录" :image-size="72"/></el-card>
      </div>
    </template>

    <el-dialog v-model="dispatchDialog.visible" :title="isMaintenance?'人工派单':'智能派单推荐'" width="760px"><el-alert v-if="order" :title="`${order.workOrderNo} · ${order.equipmentName}`" :description="`${order.workshopName || '位置未填写'} · ${priorityLabel(order.priority)} · ${detail.repairRequest?.faultDescription || '暂无故障描述'}`" type="info" :closable="false"/><div v-if="!isMaintenance" class="recommendation-section"><div class="recommendation-heading"><strong>Top {{recommendations.length}} 推荐</strong><span>系统只提供解释性排序，最终由主管确认</span></div><div v-if="recommendations.length" class="recommendation-list"><button v-for="(item,index) in recommendations" :key="item.engineerId" type="button" class="recommendation-item" :class="{selected:dispatchDialog.engineerId===item.engineerId}" @click="selectRecommendation(item)"><span class="recommendation-rank">{{index+1}}</span><span class="recommendation-person"><strong>{{item.engineerName}}</strong><small>{{item.teamName||'未分组'}} · {{item.currentActiveOrders}} 张在处</small></span><span class="recommendation-score">{{Number(item.totalScore*100).toFixed(1)}}<small>综合分</small></span><span class="recommendation-detail">技能 {{Number(item.skillScore*100).toFixed(0)}} · 负载 {{Number(item.loadScore*100).toFixed(0)}} · 班组 {{Number(item.teamScore*100).toFixed(0)}} · 区域 {{Number(item.areaScore*100).toFixed(0)}}<small>{{item.recommendationReasons.join('；')}}</small></span></button></div><el-empty v-else description="当前没有符合条件的可派工程师" :image-size="64"/></div><el-divider v-if="!isMaintenance" content-position="left">主管最终选择</el-divider><el-form label-width="90px" class="dialog-form"><el-form-item label="维修工程师" required><el-select v-model="dispatchDialog.engineerId" filterable style="width:100%" @change="engineerChanged"><el-option v-for="item in engineers" :key="item.id" :label="`${item.realName} · ${item.teamName || '未分组'}`" :value="item.id"/></el-select><small class="manual-choice-tip">可选择推荐外的其他合法工程师</small></el-form-item><el-form-item label="负责班组"><el-input :model-value="engineers.find(i => i.id === dispatchDialog.engineerId)?.teamName || '选择工程师后自动确定'" disabled/></el-form-item><el-form-item label="派单说明"><el-input v-model="dispatchDialog.reason" type="textarea" :rows="2"/></el-form-item></el-form><template #footer><el-button @click="dispatchDialog.visible = false">取消</el-button><el-button type="primary" :loading="actionLoading" @click="assign">确认派单</el-button></template></el-dialog>
    <el-dialog v-model="recordDialog.visible" title="维修过程记录" width="680px"><el-form label-width="90px"><el-form-item label="排查过程" required><el-input v-model="record.inspectionProcess" type="textarea" :rows="2"/></el-form-item><el-form-item label="根因分析" required><el-input v-model="record.rootCause" type="textarea" :rows="2"/></el-form-item><el-form-item label="处理措施" required><el-input v-model="record.repairAction" type="textarea" :rows="2"/></el-form-item><el-form-item label="维修结果" required><el-input v-model="record.repairResult" type="textarea" :rows="2"/></el-form-item><el-form-item label="维修工时"><el-input-number v-model="record.laborHours" :min="0" :precision="2"/></el-form-item><el-form-item label="停机分钟"><el-input-number v-model="record.downtimeMinutes" :min="0"/></el-form-item><el-form-item label="是否修复"><el-switch v-model="record.repairable" active-text="已修复" inactive-text="无法修复"/></el-form-item></el-form><template #footer><el-button @click="recordDialog.visible = false">取消</el-button><el-button type="primary" :loading="actionLoading" @click="saveRecord">保存记录</el-button></template></el-dialog>
    <el-dialog v-model="cancelDialog.visible" :title="`取消${isMaintenance?'保养':'维修'}工单`" width="520px"><el-alert :title="isMaintenance?'取消保养工单不会改变设备状态。':'取消不会自动恢复设备状态，请明确选择后续状态。'" type="warning" :closable="false"/><el-form label-width="100px" class="dialog-form"><el-form-item label="取消原因" required><el-input v-model="cancelDialog.reason" type="textarea" :rows="3"/></el-form-item><el-form-item v-if="!isMaintenance" label="设备状态" required><el-radio-group v-model="cancelDialog.equipmentTargetStatus"><el-radio value="RUNNING">确认正常，恢复运行</el-radio><el-radio value="STOPPED">故障仍在，设备停用</el-radio></el-radio-group></el-form-item></el-form><template #footer><el-button @click="cancelDialog.visible = false">返回</el-button><el-button type="danger" :loading="actionLoading" @click="cancelOrder">确认取消</el-button></template></el-dialog>
    <el-dialog v-model="spareDialog.visible" title="领用维修备件" width="560px"><el-alert title="提交后立即扣减所选仓库库存，并生成工单领用记录和库存流水。" type="info" :closable="false"/><el-form label-width="90px" class="dialog-form"><el-form-item label="领用仓库" required><el-select v-model="spareDialog.warehouseId" style="width:100%"><el-option v-for="w in warehouseOptions" :key="w.id" :label="`${w.warehouseNo} · ${w.warehouseName}`" :value="w.id"/></el-select></el-form-item><el-form-item label="备件" required><el-select v-model="spareDialog.sparePartId" filterable style="width:100%"><el-option v-for="p in spareOptions" :key="p.id" :label="`${p.spareNo} · ${p.spareName} · ${p.specification||'无规格'}`" :value="p.id"/></el-select></el-form-item><el-form-item label="领用数量" required><el-input-number v-model="spareDialog.qty" :min="0.01" :precision="2" style="width:100%"/></el-form-item><el-form-item label="领用说明"><el-input v-model="spareDialog.remark" type="textarea" :rows="2"/></el-form-item></el-form><template #footer><el-button @click="spareDialog.visible=false">取消</el-button><el-button type="primary" :loading="actionLoading" @click="issueSpare">确认领用</el-button></template></el-dialog>
    <el-dialog v-model="returnDialog.visible" title="退回未使用备件" width="520px"><el-alert v-if="returnDialog.issue" :title="`${returnDialog.issue.spareName} · 尚可退 ${returnDialog.issue.usedQty} ${returnDialog.issue.unit}`" type="warning" :closable="false"/><el-form label-width="90px" class="dialog-form"><el-form-item label="退库数量" required><el-input-number v-model="returnDialog.qty" :min="0.01" :max="Number(returnDialog.issue?.usedQty||0)" :precision="2" style="width:100%"/></el-form-item><el-form-item label="退库原因" required><el-input v-model="returnDialog.remark" type="textarea" :rows="3"/></el-form-item></el-form><template #footer><el-button @click="returnDialog.visible=false">取消</el-button><el-button type="primary" :loading="actionLoading" @click="returnSpare">确认退库</el-button></template></el-dialog>
  </section>
</template>

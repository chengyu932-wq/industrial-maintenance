<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '../../api/workorder'
import { useAuthStore } from '../../stores/auth'
import { priorityLabel, priorityType, workOrderStatusLabel, workOrderStatusType, workOrderStep } from '../../utils/workOrderStatus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const loading = shallowRef(true)
const actionLoading = shallowRef(false)
const detail = shallowRef<any>(null)
const engineers = shallowRef<any[]>([])
const dispatchDialog = reactive<any>({ visible: false, engineerId: null, teamId: null, reason: '' })
const recordDialog = reactive({ visible: false })
const cancelDialog = reactive<any>({ visible: false, reason: '', equipmentTargetStatus: 'RUNNING' })
const record = reactive<any>({ inspectionProcess: '', rootCause: '', repairAction: '', repairResult: '', laborHours: 0, downtimeMinutes: 0, repairable: true })
const order = computed(() => detail.value?.workOrder)
const canProcess = computed(() => auth.hasPermission('workorder:process') && order.value?.assignedEngineerId === auth.currentUser?.userId)
const activeStep = computed(() => workOrderStep(order.value?.status))
const isExceptionState = computed(() => ['SUSPENDED', 'CANCELLED'].includes(order.value?.status))

async function load(showLoading = true) {
  if (showLoading) loading.value = true
  try {
    const fresh = (await api.detail(Number(route.params.id))).data
    if (!showLoading) { detail.value = null; await nextTick() }
    detail.value = fresh
  } catch (e: any) { ElMessage.error(e.response?.data?.message || '工单详情加载失败') }
  finally { if (showLoading) loading.value = false }
}
async function perform(action: () => Promise<any>, message: string) {
  if (actionLoading.value) return false
  actionLoading.value = true
  try { await action(); ElMessage.success(message); await load(false); return true }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '操作失败'); return false }
  finally { actionLoading.value = false }
}
async function openDispatch() {
  actionLoading.value = true
  try { engineers.value = (await api.engineers(order.value.id)).data; Object.assign(dispatchDialog, { visible: true, engineerId: null, teamId: null, reason: '' }) }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '工程师列表加载失败') }
  finally { actionLoading.value = false }
}
function engineerChanged(id: number) { const item = engineers.value.find(i => i.id === id); dispatchDialog.teamId = item?.teamId || null }
async function assign() { if (!dispatchDialog.engineerId) return ElMessage.warning('请选择工程师'); if (await perform(() => api.assign(order.value.id, { engineerId: dispatchDialog.engineerId, teamId: dispatchDialog.teamId, reason: dispatchDialog.reason }), '派单成功')) dispatchDialog.visible = false }
async function reasonAction(kind: 'suspend' | 'resume') {
  try { const { value } = await ElMessageBox.prompt(kind === 'suspend' ? '请输入挂起原因' : '请输入恢复处理说明', kind === 'suspend' ? '挂起工单' : '恢复工单', { inputPattern: /\S+/, inputErrorMessage: '原因不能为空', type: 'warning' }); await perform(() => api[kind](order.value.id, { reason: value }), kind === 'suspend' ? '工单已挂起' : '工单已恢复处理') }
  catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败') }
}
function openRecord() { Object.assign(record, detail.value.repairRecord || { inspectionProcess: '', rootCause: '', repairAction: '', repairResult: '', laborHours: 0, downtimeMinutes: 0, repairable: true }); recordDialog.visible = true }
async function saveRecord() { if (!record.inspectionProcess.trim() || !record.rootCause.trim() || !record.repairAction.trim() || !record.repairResult.trim()) return ElMessage.warning('请完整填写维修过程'); if (await perform(() => api.saveRepairRecord(order.value.id, record), '维修记录已保存')) recordDialog.visible = false }
async function submit() { try { await ElMessageBox.confirm('确认维修记录完整并提交验收？提交后将等待报修人或主管验收。', '提交验收', { type: 'warning' }); await perform(() => api.submitAcceptance(order.value.id), '已提交验收') } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '提交失败') } }
async function pass() { try { const { value } = await ElMessageBox.prompt('可填写验收意见', '验收通过', { inputValue: '试运行正常', type: 'success' }); await perform(() => api.pass(order.value.id, { remark: value }), '验收通过，设备已恢复运行') } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '验收失败') } }
async function reject() { try { const { value } = await ElMessageBox.prompt('请输入验收不通过原因', '退回维修', { inputPattern: /\S+/, inputErrorMessage: '退回原因不能为空', type: 'warning' }); await perform(() => api.reject(order.value.id, { reason: value }), '已退回工程师继续处理') } catch (e: any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '退回失败') } }
async function cancelOrder() { if (!cancelDialog.reason.trim()) return ElMessage.warning('请输入取消原因'); if (await perform(() => api.cancel(order.value.id, { reason: cancelDialog.reason, equipmentTargetStatus: cancelDialog.equipmentTargetStatus }), '工单已取消')) cancelDialog.visible = false }
const actionLabels: any = { CREATE: '创建工单', ASSIGN: '人工派单', ACCEPT: '工程师接单', START: '开始维修', SUSPEND: '挂起', RESUME: '恢复', SUBMIT: '提交验收', ACCEPT_PASS: '验收通过', ACCEPT_RETURN: '验收退回', CANCEL: '取消' }
onMounted(load)
</script>

<template>
  <section class="module-page" v-loading="loading">
    <header class="module-header">
      <div><p class="eyebrow">WORK ORDER DETAIL</p><h1>{{ order?.workOrderNo || '工单详情' }}</h1><p v-if="order">{{ order.equipmentName }} · {{ order.workshopName }}</p></div>
      <div class="header-actions"><el-button @click="router.push('/work-orders')">返回列表</el-button><el-button v-if="order?.status === 'PENDING_ASSIGN'" v-permission="'workorder:assign'" type="primary" :loading="actionLoading" @click="openDispatch">人工派单</el-button><el-button v-if="['PENDING_ASSIGN', 'ASSIGNED'].includes(order?.status)" v-permission="'workorder:cancel'" type="danger" plain :disabled="actionLoading" @click="cancelDialog.visible = true">取消工单</el-button></div>
    </header>

    <template v-if="order">
      <el-card shadow="never" class="summary-card">
        <div class="summary-grid">
          <div class="summary-item"><small>当前状态</small><el-tag :type="workOrderStatusType(order.status)">{{ workOrderStatusLabel(order.status) }}</el-tag></div>
          <div class="summary-item"><small>故障等级</small><el-tag :type="priorityType(order.priority)" effect="plain">{{ priorityLabel(order.priority) }}</el-tag></div>
          <div class="summary-item"><small>设备</small><strong :title="`${order.equipmentNo} · ${order.equipmentName}`">{{ order.equipmentName }}</strong></div>
          <div class="summary-item"><small>当前负责人</small><strong>{{ order.assignedEngineerName || '等待主管派单' }}</strong></div>
          <div class="summary-item"><small>创建时间</small><strong>{{ order.createdAt || detail.repairRequest?.reportedAt || '-' }}</strong></div>
          <div class="summary-item"><small>下一步</small><strong>{{ order.status === 'PENDING_ASSIGN' ? '主管派单' : order.status === 'ASSIGNED' ? '工程师开始维修' : order.status === 'PROCESSING' ? '提交验收' : order.status === 'PENDING_ACCEPT' ? '报修人或主管验收' : order.status === 'SUSPENDED' ? '工程师恢复处理' : '流程已结束' }}</strong></div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header><div class="card-heading"><div class="card-title"><strong>处理进度</strong><small>主流程进度；异常状态单独标识</small></div><el-tag v-if="isExceptionState" :type="workOrderStatusType(order.status)">{{ workOrderStatusLabel(order.status) }}</el-tag></div></template>
        <div class="workflow-steps"><el-steps :active="activeStep" finish-status="success" align-center><el-step title="待派单"/><el-step title="已派单"/><el-step title="处理中"/><el-step title="待验收"/><el-step title="已完成"/></el-steps></div>
      </el-card>

      <el-card v-if="canProcess && ['ASSIGNED', 'PROCESSING', 'SUSPENDED'].includes(order.status)" shadow="never" class="action-card">
        <template #header><div class="card-title"><strong>维修处理</strong><small>仅显示当前状态允许的操作</small></div></template>
        <div class="workflow-actions"><el-button v-if="order.status === 'ASSIGNED' && !order.acceptedAt" :loading="actionLoading" @click="perform(() => api.acceptResponse(order.id), '接单成功')">确认接单</el-button><el-button v-if="order.status === 'ASSIGNED'" type="primary" :loading="actionLoading" @click="perform(() => api.start(order.id), '已开始维修，设备进入维修中')">开始维修</el-button><el-button v-if="order.status === 'PROCESSING'" :disabled="actionLoading" @click="openRecord">填写维修记录</el-button><el-button v-if="order.status === 'PROCESSING'" type="warning" plain :disabled="actionLoading" @click="reasonAction('suspend')">挂起</el-button><el-button v-if="order.status === 'SUSPENDED'" type="primary" :loading="actionLoading" @click="reasonAction('resume')">恢复处理</el-button><el-button v-if="order.status === 'PROCESSING'" type="primary" :loading="actionLoading" @click="submit">提交验收</el-button></div>
      </el-card>

      <el-card v-if="order.status === 'PENDING_ACCEPT' && auth.hasPermission('workorder:accept')" shadow="never" class="action-card">
        <template #header><div class="card-title"><strong>维修验收</strong><small>核对维修结果与试运行情况</small></div></template>
        <p>验收通过后工单完成，设备恢复运行；退回维修必须填写原因，工单将重新进入处理中。</p>
        <div class="workflow-actions"><el-button type="primary" :loading="actionLoading" @click="pass">验收通过</el-button><el-button type="danger" plain :disabled="actionLoading" @click="reject">退回维修</el-button></div>
      </el-card>

      <div class="detail-grid">
        <div class="section-stack">
          <el-card shadow="never"><template #header><div class="card-title"><strong>报修信息</strong><small>故障现象与来源</small></div></template><el-descriptions :column="2" border><el-descriptions-item label="报修人">{{ order.reporterName }}</el-descriptions-item><el-descriptions-item label="报修时间">{{ detail.repairRequest?.reportedAt || '-' }}</el-descriptions-item><el-descriptions-item label="故障描述" :span="2">{{ detail.repairRequest?.faultDescription || '-' }}</el-descriptions-item></el-descriptions></el-card>
          <el-card shadow="never"><template #header><div class="card-title"><strong>设备与责任信息</strong><small>定位设备及当前处理人员</small></div></template><el-descriptions :column="2" border><el-descriptions-item label="设备编号">{{ order.equipmentNo }}</el-descriptions-item><el-descriptions-item label="设备名称">{{ order.equipmentName }}</el-descriptions-item><el-descriptions-item label="所属车间">{{ order.workshopName || '-' }}</el-descriptions-item><el-descriptions-item label="负责班组">{{ order.assignedTeamName || '-' }}</el-descriptions-item><el-descriptions-item label="维修工程师">{{ order.assignedEngineerName || '待派单' }}</el-descriptions-item><el-descriptions-item label="退回次数">{{ order.acceptanceReturnCount }}</el-descriptions-item></el-descriptions></el-card>
          <el-card shadow="never"><template #header><div class="card-title"><strong>维修过程</strong><small>排查、根因、措施与结果</small></div></template><el-descriptions v-if="detail.repairRecord" :column="1" border><el-descriptions-item label="排查过程">{{ detail.repairRecord.inspectionProcess || '-' }}</el-descriptions-item><el-descriptions-item label="根因分析">{{ detail.repairRecord.rootCause || '-' }}</el-descriptions-item><el-descriptions-item label="处理措施">{{ detail.repairRecord.repairAction || '-' }}</el-descriptions-item><el-descriptions-item label="维修结果">{{ detail.repairRecord.repairResult || '-' }}</el-descriptions-item><el-descriptions-item label="维修耗时">{{ detail.repairRecord.laborHours }} 小时 · 停机 {{ detail.repairRecord.downtimeMinutes }} 分钟</el-descriptions-item></el-descriptions><el-empty v-else class="detail-empty" description="暂无维修记录" :image-size="72"/></el-card>
        </div>
        <el-card shadow="never"><template #header><div class="card-title"><strong>流转记录</strong><small>完整记录状态变化与操作人</small></div></template><el-timeline v-if="detail.flows?.length"><el-timeline-item v-for="item in detail.flows" :key="item.id" :timestamp="item.createdAt" placement="top" :type="item.toStatus === 'COMPLETED' ? 'success' : item.toStatus === 'CANCELLED' ? 'info' : 'primary'"><strong>{{ actionLabels[item.action] || item.action }}</strong><p>{{ item.fromStatus ? workOrderStatusLabel(item.fromStatus) : '流程开始' }} → {{ workOrderStatusLabel(item.toStatus) }}</p><span class="timeline-meta">{{ item.operatorName || `用户 ${item.operatorId}` }} · {{ item.remark || '无备注' }}</span></el-timeline-item></el-timeline><el-empty v-else description="暂无流转记录" :image-size="72"/></el-card>
      </div>
    </template>

    <el-dialog v-model="dispatchDialog.visible" title="人工派单" width="520px"><el-alert v-if="order" :title="`${order.workOrderNo} · ${order.equipmentName}`" :description="`${order.workshopName || '位置未填写'} · ${priorityLabel(order.priority)} · ${detail.repairRequest?.faultDescription || '暂无故障描述'}`" type="info" :closable="false"/><el-form label-width="90px" class="dialog-form"><el-form-item label="维修工程师" required><el-select v-model="dispatchDialog.engineerId" style="width:100%" @change="engineerChanged"><el-option v-for="item in engineers" :key="item.id" :label="`${item.realName} · ${item.teamName || '未分组'}`" :value="item.id"/></el-select></el-form-item><el-form-item label="负责班组"><el-input :model-value="engineers.find(i => i.id === dispatchDialog.engineerId)?.teamName || '选择工程师后自动确定'" disabled/></el-form-item><el-form-item label="派单说明"><el-input v-model="dispatchDialog.reason" type="textarea" :rows="3"/></el-form-item></el-form><template #footer><el-button @click="dispatchDialog.visible = false">取消</el-button><el-button type="primary" :loading="actionLoading" @click="assign">确认派单</el-button></template></el-dialog>
    <el-dialog v-model="recordDialog.visible" title="维修过程记录" width="680px"><el-form label-width="90px"><el-form-item label="排查过程" required><el-input v-model="record.inspectionProcess" type="textarea" :rows="2"/></el-form-item><el-form-item label="根因分析" required><el-input v-model="record.rootCause" type="textarea" :rows="2"/></el-form-item><el-form-item label="处理措施" required><el-input v-model="record.repairAction" type="textarea" :rows="2"/></el-form-item><el-form-item label="维修结果" required><el-input v-model="record.repairResult" type="textarea" :rows="2"/></el-form-item><el-form-item label="维修工时"><el-input-number v-model="record.laborHours" :min="0" :precision="2"/></el-form-item><el-form-item label="停机分钟"><el-input-number v-model="record.downtimeMinutes" :min="0"/></el-form-item><el-form-item label="是否修复"><el-switch v-model="record.repairable" active-text="已修复" inactive-text="无法修复"/></el-form-item></el-form><template #footer><el-button @click="recordDialog.visible = false">取消</el-button><el-button type="primary" :loading="actionLoading" @click="saveRecord">保存记录</el-button></template></el-dialog>
    <el-dialog v-model="cancelDialog.visible" title="取消维修工单" width="520px"><el-alert title="取消不会自动恢复设备状态，请明确选择后续状态。" type="warning" :closable="false"/><el-form label-width="100px" class="dialog-form"><el-form-item label="取消原因" required><el-input v-model="cancelDialog.reason" type="textarea" :rows="3"/></el-form-item><el-form-item label="设备状态" required><el-radio-group v-model="cancelDialog.equipmentTargetStatus"><el-radio value="RUNNING">确认正常，恢复运行</el-radio><el-radio value="STOPPED">故障仍在，设备停用</el-radio></el-radio-group></el-form-item></el-form><template #footer><el-button @click="cancelDialog.visible = false">返回</el-button><el-button type="danger" :loading="actionLoading" @click="cancelOrder">确认取消</el-button></template></el-dialog>
  </section>
</template>

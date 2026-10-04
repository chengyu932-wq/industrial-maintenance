<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as equipmentApi from '../../api/equipment'
import * as repairApi from '../../api/repair'
import { priorityLabel, priorityType, repairPriorities, repairRequestStatuses, workOrderStatusLabel, workOrderStatusType } from '../../utils/workOrderStatus'
import { useAuthStore } from '../../stores/auth'
import { statusLabel, statusTagType } from '../../utils/equipmentStatus'

const auth=useAuthStore();const photos=shallowRef<File[]>([]);const attachmentDialog=reactive<any>({visible:false,items:[],requestId:null});
const route = useRoute()
const router = useRouter()
const loading = shallowRef(false)
const submitting = shallowRef(false)
const records = shallowRef<any[]>([])
const equipment = shallowRef<any[]>([])
const scannedEquipment = shallowRef<any>(null)
const total = shallowRef(0)
const query = reactive<any>({ requestNo: '', priority: null, status: null, page: 1, size: 10 })
const form = reactive<any>({ equipmentId: null, priority: 'NORMAL', faultDescription: '', source: 'PC' })
const selectedEquipment = computed(() => scannedEquipment.value?.id === form.equipmentId
  ? scannedEquipment.value
  : equipment.value.find(item => item.id === form.equipmentId))
const canSubmit = computed(() => selectedEquipment.value?.status === 'RUNNING')

async function loadEquipment() {
  const response = await equipmentApi.page({ status: 'RUNNING', page: 1, size: 100 })
  equipment.value = response.data.records
}
async function loadScannedEquipment(id: number) {
  try { scannedEquipment.value = (await equipmentApi.detail(id)).data }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '扫码设备信息加载失败') }
}
async function load() {
  loading.value = true
  try { const response = await repairApi.page(query); records.value = response.data.records; total.value = response.data.total }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '报修记录加载失败') }
  finally { loading.value = false }
}
async function submit() {
  if (!form.equipmentId || !form.faultDescription.trim()) return ElMessage.warning('请选择设备并填写故障描述')
  if (!canSubmit.value) return ElMessage.warning('当前设备状态不允许重复报修，请先查看关联工单')
  submitting.value = true
  try {
    const response = await repairApi.create({ ...form, faultDescription: form.faultDescription.trim() })
    for(const file of photos.value){try{await repairApi.uploadAttachment(response.data.repairRequestId,file)}catch(e:any){ElMessage.warning('报修已保存，部分附件上传失败；可在现场资料中补传。');break}}
    photos.value=[]
    ElMessage.success('报修已提交，等待主管确认后生成维修工单')
    Object.assign(form, { equipmentId: null, priority: 'NORMAL', faultDescription: '', source: 'PC' })
    await Promise.all([loadEquipment(), load()])

  } catch (e: any) { ElMessage.error(e.response?.data?.message || '报修提交失败') }
  finally { submitting.value = false }
}
async function cancel(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因。', '取消报修', { inputPattern: /\S+/, inputErrorMessage: '取消原因不能为空', type: 'warning' })
    const target = row.workOrderId ? await ElMessageBox.confirm('取消后设备是否确认恢复运行？', '设备状态处置', { confirmButtonText: '恢复运行', cancelButtonText: '停用设备', distinguishCancelAndClose: true, type: 'warning' }).then(() => 'RUNNING').catch((action: any) => { if (action === 'cancel') return 'STOPPED'; throw action }) : 'RUNNING'
    await repairApi.cancel(row.id, { reason: value, equipmentTargetStatus: target })
    ElMessage.success('报修已取消')
    await Promise.all([loadEquipment(), load()])
  } catch (e: any) { if (e !== 'close' && e !== 'cancel') ElMessage.error(e.response?.data?.message || '取消失败') }
}
async function confirm(row:any){try{await ElMessageBox.confirm('确认故障后生成维修工单，并将设备标记为故障。','确认报修',{type:'warning',confirmButtonText:'确认',cancelButtonText:'取消'});const result=await repairApi.confirm(row.id);ElMessage.success('已确认并生成维修工单');await load();await router.push(`/work-orders/${result.data.workOrderId}`)}catch(e:any){if(e!=='cancel')ElMessage.error(e.response?.data?.message||'确认失败')}}
async function viewAttachments(row:any){try{attachmentDialog.requestId=row.id;attachmentDialog.canUpload=row.status==='SUBMITTED'&&(row.reporterId===auth.currentUser?.userId||auth.hasRole('ADMIN')||auth.hasRole('MAINTENANCE_SUPERVISOR'));attachmentDialog.items=(await repairApi.attachments(row.id)).data;attachmentDialog.visible=true}catch(e:any){ElMessage.error(e.response?.data?.message||'附件读取失败')}}
async function uploadExisting(event:Event){const input=event.target as HTMLInputElement;const file=input.files?.[0];input.value='';if(!file)return;try{await repairApi.uploadAttachment(attachmentDialog.requestId,file);attachmentDialog.items=(await repairApi.attachments(attachmentDialog.requestId)).data;ElMessage.success('附件已上传')}catch(e:any){ElMessage.error(e.response?.data?.message||'上传失败')}}
async function download(item:any){const blob=await repairApi.downloadAttachment(attachmentDialog.requestId,item.id);const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=item.fileName;link.click();URL.revokeObjectURL(url)}
function search() { query.page = 1; load() }
function reset() { Object.assign(query, { requestNo: '', priority: null, status: null, page: 1, size: 10 }); load() }

onMounted(async () => {
  const equipmentId = Number(route.query.equipmentId)
  if (equipmentId) { form.equipmentId = equipmentId; await loadScannedEquipment(equipmentId) }
  if (route.query.source === 'QR' || equipmentId) form.source = 'QR'
  await Promise.all([loadEquipment(), load()])
})
</script>

<template>
  <section class="module-page">
    <header class="module-header"><div><p class="eyebrow">FAULT REPORTING</p><h1>故障报修</h1><p>快速记录故障，提交报修后由主管确认，确认成功后生成唯一维修工单。</p></div></header>
    <el-card v-permission="'repair:create'" shadow="never" class="repair-create-card">
      <template #header><div class="card-title"><strong>{{ form.source === 'QR' ? '扫码报修' : '创建故障报修' }}</strong><small>设备信息与故障现象</small></div></template>
      <div v-if="selectedEquipment" class="selected-equipment">
        <div class="card-heading"><strong>{{ selectedEquipment.equipmentName }} · {{ selectedEquipment.equipmentNo }}</strong><el-tag :type="statusTagType(selectedEquipment.status)">{{ statusLabel(selectedEquipment.status) }}</el-tag></div>
        <p>{{ selectedEquipment.workshopName }} / {{ selectedEquipment.lineName }} / {{ selectedEquipment.stationName }}</p>
      </div>
      <el-alert v-if="selectedEquipment && !canSubmit" title="当前设备不可报修" :description="selectedEquipment.status === 'SCRAPPED' ? '设备已报废，不能创建维修工单。' : '设备正在维修、已故障或已停用，请先查看现有工单或联系主管处理。'" type="warning" :closable="false" show-icon style="margin-bottom:18px" />
      <el-form label-width="92px">
        <el-form-item label="故障设备" required><el-select v-model="form.equipmentId" filterable placeholder="请选择运行中的设备" style="width:100%"><el-option v-if="scannedEquipment && !equipment.some(i => i.id === scannedEquipment.id)" :key="scannedEquipment.id" :label="`${scannedEquipment.equipmentNo} · ${scannedEquipment.equipmentName}`" :value="scannedEquipment.id" disabled/><el-option v-for="item in equipment" :key="item.id" :label="`${item.equipmentNo} · ${item.equipmentName}`" :value="item.id"/></el-select></el-form-item>
        <el-form-item label="故障等级" required><el-radio-group v-model="form.priority"><el-radio-button v-for="(item, key) in repairPriorities" :key="key" :value="key">{{ item.label }}</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="故障描述" required><el-input v-model="form.faultDescription" type="textarea" :rows="4" maxlength="5000" show-word-limit placeholder="请描述故障现象、发生时间和现场情况"/></el-form-item>
        <el-form-item label="现场资料"><input type="file" multiple accept=".png,.jpg,.jpeg,.pdf,.txt" @change="event=>photos=Array.from((event.target as HTMLInputElement).files||[])"><small>单文件不超过10MB，提交后上传到报修记录。</small></el-form-item>
        <el-form-item><el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">提交报修</el-button><span class="form-tip">报修人由当前登录账号自动确定</span></el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <template #header><div class="card-title"><strong>报修记录</strong><small>跟踪报修与关联工单状态</small></div></template>
      <el-form inline class="compact-filter">
        <el-form-item label="报修编号"><el-input v-model="query.requestNo" clearable @keyup.enter="search"/></el-form-item>
        <el-form-item label="等级"><el-select v-model="query.priority" clearable><el-option v-for="(item, key) in repairPriorities" :key="key" :label="item.label" :value="key"/></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable><el-option v-for="(item, key) in repairRequestStatuses" :key="key" :label="item.label" :value="key"/></el-select></el-form-item>
        <el-form-item class="filter-actions"><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="reset">重置</el-button></el-form-item>
      </el-form>
      <el-table v-loading="loading" :data="records" stripe>
        <template #empty><el-empty description="暂无报修记录" :image-size="72"/></template>
        <el-table-column prop="requestNo" label="报修编号" min-width="190" show-overflow-tooltip/>
        <el-table-column label="设备" min-width="180"><template #default="{ row }"><span class="table-primary">{{ row.equipmentName }}</span><small class="table-subtitle">{{ row.equipmentNo }}</small></template></el-table-column>
        <el-table-column label="等级" width="84"><template #default="{ row }"><el-tag :type="priorityType(row.priority)" effect="light">{{ priorityLabel(row.priority) }}</el-tag></template></el-table-column>
        <el-table-column prop="faultDescription" label="故障描述" min-width="220" show-overflow-tooltip/>
        <el-table-column prop="reporterName" label="报修人" width="100" show-overflow-tooltip/>
        <el-table-column label="工单状态" width="105"><template #default="{ row }"><el-tag v-if="row.workOrderStatus" :type="workOrderStatusType(row.workOrderStatus)">{{ workOrderStatusLabel(row.workOrderStatus) }}</el-tag><span v-else>{{row.status==='SUBMITTED'?'待主管确认':'已取消'}}</span></template></el-table-column>
        <el-table-column prop="reportedAt" label="报修时间" width="168"/>
        <el-table-column label="操作" width="230" fixed="right"><template #default="{ row }"><el-button v-if="row.status==='SUBMITTED'" v-permission="'repair:confirm'" link type="primary" @click="confirm(row)">确认报修</el-button><el-button link @click="viewAttachments(row)">现场资料</el-button><el-button v-if="row.workOrderId" link @click="router.push(`/work-orders/${row.workOrderId}`)">查看</el-button><el-button v-if="row.reporterId===auth.currentUser?.userId && (row.status==='SUBMITTED'||['PENDING_ASSIGN', 'ASSIGNED'].includes(row.workOrderStatus))" v-permission="'repair:cancel'" link type="danger" @click="cancel(row)">取消</el-button></template></el-table-column>
      </el-table>
      <div class="pagination-row"><el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" @change="load"/></div>
    </el-card>
    <el-dialog v-model="attachmentDialog.visible" title="报修现场资料"><div v-if="attachmentDialog.canUpload"><span>补传现场资料：</span><input type="file" accept=".png,.jpg,.jpeg,.pdf,.txt" @change="uploadExisting"></div><el-table :data="attachmentDialog.items"><el-table-column prop="fileName" label="文件名"/><el-table-column label="操作"><template #default="{row}"><el-button link @click="download(row)">下载</el-button></template></el-table-column></el-table></el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '../../api/organization'

type Kind = 'workshop' | 'line' | 'station'
const loading = shallowRef(false)
const workshops = shallowRef<any[]>([])
const lines = shallowRef<any[]>([])
const stations = shallowRef<any[]>([])
const selectedWorkshop = shallowRef<number | null>(null)
const selectedLine = shallowRef<number | null>(null)
const dialog = reactive({ visible: false, kind: 'workshop' as Kind, id: null as number | null, title: '' })
const form = reactive<any>({ code: '', name: '', parentId: null, managerId: null, status: 'ENABLED' })
const currentLines = computed(() => selectedWorkshop.value ? lines.value.filter(i => i.workshopId === selectedWorkshop.value) : lines.value)
const currentStations = computed(() => selectedLine.value ? stations.value.filter(i => i.lineId === selectedLine.value) : stations.value)

async function load() {
  loading.value = true
  try {
    const [w, l, s] = await Promise.all([api.workshops(), api.lines(), api.stations()])
    workshops.value = w.data; lines.value = l.data; stations.value = s.data
    if (selectedWorkshop.value && !workshops.value.some(i => i.id === selectedWorkshop.value)) selectedWorkshop.value = null
    if (selectedLine.value && !lines.value.some(i => i.id === selectedLine.value)) selectedLine.value = null
  } finally { loading.value = false }
}
function selectWorkshop(row:any) { selectedWorkshop.value = row.id; selectedLine.value = null }
function selectLine(row:any) { selectedLine.value = row.id; selectedWorkshop.value = row.workshopId }
function open(kind:Kind, row:any = null) {
  dialog.kind = kind; dialog.id = row?.id || null; dialog.title = `${row ? '修改' : '新增'}${kind === 'workshop' ? '车间' : kind === 'line' ? '产线' : '工位'}`
  form.code = row?.workshopNo || row?.lineNo || row?.stationNo || ''; form.name = row?.workshopName || row?.lineName || row?.stationName || ''
  form.parentId = kind === 'line' ? (row?.workshopId || selectedWorkshop.value) : kind === 'station' ? (row?.lineId || selectedLine.value) : null
  form.managerId = row?.managerId || null; form.status = row?.status || 'ENABLED'; dialog.visible = true
}
async function save() {
  if (!form.code.trim() || !form.name.trim()) return ElMessage.warning('请填写编号和名称')
  const k = dialog.kind; let payload:any
  if (k === 'workshop') payload = { workshopNo: form.code, workshopName: form.name, managerId: form.managerId, status: form.status }
  else if (k === 'line') { if (!form.parentId) return ElMessage.warning('请选择所属车间'); payload = { workshopId: form.parentId, lineNo: form.code, lineName: form.name, status: form.status } }
  else { if (!form.parentId) return ElMessage.warning('请选择所属产线'); payload = { lineId: form.parentId, stationNo: form.code, stationName: form.name, status: form.status } }
  try {
    if (k === 'workshop') await (dialog.id ? api.updateWorkshop(dialog.id, payload) : api.createWorkshop(payload))
    else if (k === 'line') await (dialog.id ? api.updateLine(dialog.id, payload) : api.createLine(payload))
    else await (dialog.id ? api.updateStation(dialog.id, payload) : api.createStation(payload))
    dialog.visible = false; ElMessage.success('保存成功'); await load()
  } catch (e:any) { ElMessage.error(e.response?.data?.message || '保存失败') }
}
async function remove(kind:Kind, row:any) {
  try { await ElMessageBox.confirm(`确定删除“${row.workshopName || row.lineName || row.stationName}”吗？存在下级数据时系统会拒绝删除。`, '删除确认', { type: 'warning' })
    if (kind === 'workshop') await api.deleteWorkshop(row.id); else if (kind === 'line') await api.deleteLine(row.id); else await api.deleteStation(row.id)
    ElMessage.success('删除成功'); await load()
  } catch (e:any) { if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除失败') }
}
onMounted(load)
</script>

<template>
  <section class="module-page" v-loading="loading">
    <header class="module-header"><div><p class="eyebrow">LOCATION MASTER DATA</p><h1>组织结构</h1><p>按车间、产线、工位维护设备的正式位置关系。</p></div><el-button @click="load">刷新</el-button></header>
    <div class="organization-grid">
      <el-card shadow="never"><template #header><div class="card-heading"><b>车间</b><el-button v-permission="'organization:add'" type="primary" size="small" @click="open('workshop')">新增</el-button></div></template>
        <el-table :data="workshops" highlight-current-row @current-change="selectWorkshop"><el-table-column prop="workshopNo" label="编号" width="110"/><el-table-column prop="workshopName" label="名称"/><el-table-column label="状态" width="76"><template #default="{row}"><el-tag :type="row.status==='ENABLED'?'success':'info'">{{ row.status==='ENABLED'?'启用':'停用' }}</el-tag></template></el-table-column><el-table-column label="操作" width="126"><template #default="{row}"><el-button v-permission="'organization:update'" link type="primary" @click.stop="open('workshop',row)">编辑</el-button><el-button v-permission="'organization:delete'" link type="danger" @click.stop="remove('workshop',row)">删除</el-button></template></el-table-column></el-table>
      </el-card>
      <el-card shadow="never"><template #header><div class="card-heading"><b>产线</b><el-button v-permission="'organization:add'" type="primary" size="small" :disabled="!selectedWorkshop" @click="open('line')">新增</el-button></div></template>
        <el-empty v-if="!selectedWorkshop" description="先选择左侧车间"/><el-table v-else :data="currentLines" highlight-current-row @current-change="selectLine"><el-table-column prop="lineNo" label="编号" width="110"/><el-table-column prop="lineName" label="名称"/><el-table-column label="操作" width="126"><template #default="{row}"><el-button v-permission="'organization:update'" link type="primary" @click.stop="open('line',row)">编辑</el-button><el-button v-permission="'organization:delete'" link type="danger" @click.stop="remove('line',row)">删除</el-button></template></el-table-column></el-table>
      </el-card>
      <el-card shadow="never"><template #header><div class="card-heading"><b>工位</b><el-button v-permission="'organization:add'" type="primary" size="small" :disabled="!selectedLine" @click="open('station')">新增</el-button></div></template>
        <el-empty v-if="!selectedLine" description="先选择中间产线"/><el-table v-else :data="currentStations"><el-table-column prop="stationNo" label="编号" width="110"/><el-table-column prop="stationName" label="名称"/><el-table-column label="操作" width="126"><template #default="{row}"><el-button v-permission="'organization:update'" link type="primary" @click="open('station',row)">编辑</el-button><el-button v-permission="'organization:delete'" link type="danger" @click="remove('station',row)">删除</el-button></template></el-table-column></el-table>
      </el-card>
    </div>
    <el-dialog v-model="dialog.visible" :title="dialog.title" width="520px"><el-form label-width="100px"><el-form-item v-if="dialog.kind==='line'" label="所属车间"><el-select v-model="form.parentId" style="width:100%"><el-option v-for="i in workshops" :key="i.id" :label="i.workshopName" :value="i.id"/></el-select></el-form-item><el-form-item v-if="dialog.kind==='station'" label="所属产线"><el-select v-model="form.parentId" style="width:100%"><el-option v-for="i in lines" :key="i.id" :label="`${i.workshopName} / ${i.lineName}`" :value="i.id"/></el-select></el-form-item><el-form-item label="编号"><el-input v-model="form.code" maxlength="50"/></el-form-item><el-form-item label="名称"><el-input v-model="form.name" maxlength="100"/></el-form-item><el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="ENABLED">启用</el-radio><el-radio value="DISABLED">停用</el-radio></el-radio-group></el-form-item></el-form><template #footer><el-button @click="dialog.visible=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template></el-dialog>
  </section>
</template>

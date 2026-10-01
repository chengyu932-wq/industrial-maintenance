<script setup lang="ts">
import * as echarts from 'echarts/core'
import { BarChart, HeatmapChart, LineChart, PieChart } from 'echarts/charts'
import { AriaComponent, CalendarComponent, GraphicComponent, GridComponent, LegendComponent, TooltipComponent, VisualMapComponent } from 'echarts/components'
import { LabelLayout } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsType } from 'echarts/core'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getStatisticsCharts, getStatisticsDrilldowns, getStatisticsKpis, getStatisticsOverview } from '../../api/statistics'

const range = ref('LAST_30_DAYS')
const customDates = ref<string[]>([])
const loading = ref(false)
const overview = ref<any>(null)
const kpis = ref<any[]>([])
const charts = ref<any>(null)
const drilldowns = ref<any>(null)
const drillFilters = reactive<{ workshopId: number | null, equipmentTypeId: number | null, engineerId: number | null }>({ workshopId: null, equipmentTypeId: null, engineerId: null })
const chartNodes = reactive<Record<string, HTMLElement | null>>({})
echarts.use([BarChart, HeatmapChart, LineChart, PieChart, AriaComponent, CalendarComponent, GraphicComponent, GridComponent, LegendComponent, TooltipComponent, VisualMapComponent, LabelLayout, CanvasRenderer])
const instances = new Map<string, EChartsType>()

const rangeOptions = [
  { value: 'LAST_7_DAYS', label: '最近 7 天' },
  { value: 'LAST_30_DAYS', label: '最近 30 天' },
  { value: 'LAST_90_DAYS', label: '最近 90 天' },
  { value: 'CUSTOM', label: '自定义范围' },
]
const periodText = computed(() => overview.value ? `${overview.value.period.startDate} 至 ${overview.value.period.endDate}` : '—')
const statusLabels: Record<string, string> = {
  PENDING: '待启用', RUNNING: '运行中', FAULT: '故障', REPAIRING: '维修中', STOPPED: '停机', SCRAPPED: '已报废',
  PENDING_ASSIGN: '待派单', ASSIGNED: '已派单', PROCESSING: '处理中', SUSPENDED: '已挂起', PENDING_ACCEPT: '待验收', COMPLETED: '已完成', UNREPAIRABLE: '无法修复', CANCELLED: '已取消',
  ON_TIME: '按时完成', OVERDUE: '超时完成',
}
const palette = ['#2878b5', '#58a57b', '#d69a2d', '#c85b4b', '#75849a', '#705aa6', '#3e8c93']

function queryParams() {
  const params: any = { range: range.value }
  if (range.value === 'CUSTOM' && customDates.value?.length === 2) {
    params.startDate = customDates.value[0]
    params.endDate = customDates.value[1]
  }
  for (const key of ['workshopId', 'equipmentTypeId', 'engineerId'] as const) {
    if (drillFilters[key]) params[key] = drillFilters[key]
  }
  return params
}
async function load() {
  if (range.value === 'CUSTOM' && customDates.value?.length !== 2) return ElMessage.warning('请选择完整的自定义日期范围')
  loading.value = true
  try {
    const params = queryParams()
    const [overviewResult, kpiResult, chartResult, drillResult] = await Promise.all([
      getStatisticsOverview(params), getStatisticsKpis(params), getStatisticsCharts(params), getStatisticsDrilldowns(params),
    ])
    overview.value = overviewResult.data
    kpis.value = kpiResult.data.metrics
    charts.value = chartResult.data
    drilldowns.value = drillResult.data
    await nextTick()
    renderCharts()
  } catch (error: any) { ElMessage.error(error.response?.data?.message || '统计数据加载失败') }
  finally { loading.value = false }
}
function setNode(key: string, node: any) { chartNodes[key] = node as HTMLElement | null }
function chart(key: string) {
  const node = chartNodes[key]
  if (!node) return null
  const existing = instances.get(key)
  if (existing) return existing
  const created = echarts.init(node)
  instances.set(key, created)
  return created
}
function namedData(rows: any[] = []) { return rows.map(row => ({ name: statusLabels[row.name] || row.name, value: Number(row.value) })) }
function emptyGraphic(rows: any[]) {
  return rows.length ? [] : [{ type: 'text', left: 'center', top: 'middle', style: { text: '当前范围暂无数据', fill: '#8994a3', fontSize: 13 } }]
}
function donutOption(rows: any[]) {
  const data = namedData(rows)
  return { color: palette, aria: { show: true }, tooltip: { trigger: 'item', valueFormatter: (v: number) => `${v}` },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#667085' } }, graphic: emptyGraphic(data),
    series: [{ type: 'pie', radius: ['48%', '70%'], center: ['50%', '43%'], avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fdfefe', borderWidth: 2 }, label: { color: '#475467', formatter: '{b}\n{c}' }, data }] }
}
function barOption(rows: any[], horizontal = false) {
  const data = namedData(rows)
  const names = data.map(item => item.name)
  const values = data.map(item => item.value)
  const category = { type: 'category', data: names, axisTick: { show: false }, axisLine: { lineStyle: { color: '#dfe5ec' } }, axisLabel: { color: '#667085', interval: 0 } }
  const value = { type: 'value', min: 0, minInterval: 1, axisLine: { show: false }, axisTick: { show: false }, splitLine: { lineStyle: { color: '#edf0f3' } }, axisLabel: { color: '#98a2b3' } }
  return { color: [palette[0]], aria: { show: true }, tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: horizontal ? 92 : 44, right: 20, top: 20, bottom: horizontal ? 28 : 62 }, graphic: emptyGraphic(data),
    xAxis: horizontal ? value : category, yAxis: horizontal ? category : value,
    series: [{ type: 'bar', data: values, barMaxWidth: 28, itemStyle: { borderRadius: horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0] } }] }
}
function lineOption(rows: any[] = []) {
  return { color: [palette[0]], aria: { show: true }, tooltip: { trigger: 'axis' },
    grid: { left: 44, right: 20, top: 24, bottom: 54 }, graphic: emptyGraphic(rows),
    xAxis: { type: 'category', boundaryGap: false, data: rows.map(row => row.date.slice(5)), axisLine: { lineStyle: { color: '#dfe5ec' } }, axisLabel: { color: '#667085', hideOverlap: true } },
    yAxis: { type: 'value', min: 0, minInterval: 1, axisLine: { show: false }, splitLine: { lineStyle: { color: '#edf0f3' } }, axisLabel: { color: '#98a2b3' } },
    series: [{ type: 'line', smooth: false, symbol: 'circle', symbolSize: 5, data: rows.map(row => row.value), lineStyle: { width: 2 }, areaStyle: { color: 'rgba(40,120,181,.10)' } }] }
}
function heatmapOption(rows: any[] = []) {
  const max = Math.max(1, ...rows.map(row => Number(row.value)))
  return {
    tooltip: { formatter: (item: any) => `${item.value[0]}：完成维修 ${item.value[1]} 单` },
    visualMap: { min: 0, max, type: 'continuous', orient: 'horizontal', left: 'center', bottom: 0, calculable: true },
    calendar: { top: 35, left: 55, right: 25, bottom: 75, range: [overview.value.period.startDate, overview.value.period.endDate],
      cellSize: ['auto', 18], dayLabel: { firstDay: 1, nameMap: ['日', '一', '二', '三', '四', '五', '六'] },
      monthLabel: { nameMap: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'] }, yearLabel: { show: false } },
    series: [{ type: 'heatmap', coordinateSystem: 'calendar', data: rows.map(row => [row.date, Number(row.value)]) }],
  }
}
function renderCharts() {
  if (!charts.value) return
  chart('equipment')?.setOption(donutOption(charts.value.equipmentStatus), true)
  chart('workorder')?.setOption(barOption(charts.value.workOrderStatus), true)
  chart('faultTypes')?.setOption(barOption(charts.value.faultEquipmentTypes, true), true)
  chart('trend')?.setOption(lineOption(charts.value.repairTrend), true)
  chart('spares')?.setOption(barOption(charts.value.spareConsumption, true), true)
  chart('sla')?.setOption(donutOption(charts.value.slaCompletion), true)
  chart('heatmap')?.setOption(heatmapOption(charts.value.repairTrend), true)
}
async function selectDrilldown(key: keyof typeof drillFilters, id: number | null) {
  if (id == null) return
  drillFilters[key] = drillFilters[key] === id ? null : id
  await load()
}
async function clearDrilldowns() {
  drillFilters.workshopId = null; drillFilters.equipmentTypeId = null; drillFilters.engineerId = null
  await load()
}
function exportChartData() {
  if (!charts.value || !drilldowns.value) return
  const rows: (string | number)[][] = [['数据集', '维度', '数值', '平均维修小时']]
  for (const [label, values] of [
    ['设备状态', charts.value.equipmentStatus], ['工单状态', charts.value.workOrderStatus],
    ['故障设备类型', charts.value.faultEquipmentTypes], ['维修完成趋势', charts.value.repairTrend.map((row: any) => ({ name: row.date, value: row.value }))],
    ['备件净消耗', charts.value.spareConsumption], ['SLA 完成情况', charts.value.slaCompletion],
  ] as [string, any[]][]) for (const row of values) rows.push([label, row.name, row.value, ''])
  for (const [label, values] of [
    ['车间维修', drilldowns.value.workshops], ['设备类型维修', drilldowns.value.equipmentTypes], ['工程师维修', drilldowns.value.engineers],
  ] as [string, any[]][]) for (const row of values) rows.push([label, row.name, row.completedCount, row.averageRepairHours ?? ''])
  const csv = '\ufeff' + rows.map(row => row.map(value => `"${String(value).replaceAll('"', '""')}"`).join(',')).join('\r\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a'); link.href = url; link.download = `运维统计_${overview.value.period.startDate}_${overview.value.period.endDate}.csv`; link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
function resizeCharts() { instances.forEach(instance => instance.resize()) }
function displayValue(metric: any) { return metric.available ? `${metric.value} ${metric.unit}` : '暂不可计算' }
function metricClass(metric: any) { return metric.available ? 'available' : metric.code === 'FIRST_TIME_FIX_RATE' ? 'gap' : 'insufficient' }
function summary(rows: any[] = []) { return rows.length ? rows.map(row => `${statusLabels[row.name] || row.name} ${row.value}`).join('；') : '当前范围暂无数据' }

watch(range, value => { if (value !== 'CUSTOM') load() })
onMounted(() => { window.addEventListener('resize', resizeCharts); load() })
onBeforeUnmount(() => { window.removeEventListener('resize', resizeCharts); instances.forEach(instance => instance.dispose()) })
</script>

<template>
  <section class="module-page statistics-page" v-loading="loading">
    <header class="module-header statistics-header">
      <div><p class="eyebrow">OPERATIONS ANALYTICS</p><h1>运维统计分析</h1><p>指标直接来自设备履历、维修工单与库存流水。统计周期：{{ periodText }}</p></div>
      <div class="statistics-filter" aria-label="统计时间范围">
        <el-select v-model="range" aria-label="预设时间范围"><el-option v-for="item in rangeOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
        <el-date-picker v-if="range === 'CUSTOM'" v-model="customDates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" />
        <el-button v-if="range === 'CUSTOM'" type="primary" @click="load">查询</el-button>
        <el-button :disabled="!charts" @click="exportChartData">导出图表数据 CSV</el-button>
      </div>
    </header>

    <section v-if="overview" class="operations-strip" aria-label="运行概览">
      <div><small>设备总数</small><strong>{{ overview.equipmentTotal }}</strong></div>
      <div><small>运行设备</small><strong>{{ overview.runningEquipment }}</strong></div>
      <div><small>维修中设备</small><strong>{{ overview.repairingEquipment }}</strong></div>
      <div><small>待处理工单</small><strong>{{ overview.pendingWorkOrders }}</strong></div>
      <div><small>完成维修</small><strong>{{ overview.completedRepairOrders }}</strong></div>
      <div :class="{ warning: overview.openStockWarnings > 0 }"><small>库存预警</small><strong>{{ overview.openStockWarnings }}</strong></div>
    </section>

    <section class="kpi-section" aria-label="核心指标详情">
      <div class="section-heading"><div><p class="eyebrow">KPI DEFINITIONS</p><h2>核心指标</h2></div><p>悬停或聚焦指标可查看公式和数据来源；无可靠数据时不输出估算值。</p></div>
      <div class="kpi-grid">
        <el-tooltip v-for="metric in kpis" :key="metric.code" placement="top" :show-after="200">
          <template #content><div class="metric-tooltip"><b>{{ metric.formula }}</b><span>来源：{{ metric.dataSource }}</span><span>{{ metric.note }}</span></div></template>
          <article class="kpi-item" :class="metricClass(metric)" tabindex="0">
            <div><small>{{ metric.name }}</small><el-tag v-if="!metric.available" size="small" :type="metric.code === 'FIRST_TIME_FIX_RATE' ? 'warning' : 'info'">{{ metric.code === 'FIRST_TIME_FIX_RATE' ? '数据缺口' : '数据不足' }}</el-tag></div>
            <strong>{{ displayValue(metric) }}</strong><p>{{ metric.note }}</p><span>样本 {{ metric.sampleSize }}</span>
          </article>
        </el-tooltip>
      </div>
    </section>

    <section v-if="drilldowns" class="kpi-section" aria-label="维修工单多维度下钻">
      <div class="section-heading"><div><p class="eyebrow">REPAIR DRILLDOWN</p><h2>维修工单多维分析</h2></div><p>点击车间、设备类型或工程师交叉筛选；再次点击取消。上方六项 KPI 保持完整权限范围口径。</p></div>
      <el-button v-if="drillFilters.workshopId || drillFilters.equipmentTypeId || drillFilters.engineerId" @click="clearDrilldowns">清除维度筛选</el-button>
      <div class="chart-grid">
        <el-card v-for="group in [{ key: 'workshopId', title: '按车间', rows: drilldowns.workshops }, { key: 'equipmentTypeId', title: '按设备类型', rows: drilldowns.equipmentTypes }, { key: 'engineerId', title: '按工程师', rows: drilldowns.engineers }]" :key="group.key" shadow="never" class="chart-card">
          <template #header><strong>{{ group.title }}</strong></template>
          <el-table :data="group.rows" height="280" empty-text="当前筛选无已完成维修工单" @row-click="row => selectDrilldown(group.key as keyof typeof drillFilters, row.id)">
            <el-table-column prop="name" label="名称" min-width="110"><template #default="{ row }"><el-button link type="primary" :aria-label="`筛选${group.title}${row.name}`">{{ row.name }}</el-button></template></el-table-column>
            <el-table-column prop="completedCount" label="完成单数" width="90" />
            <el-table-column prop="averageRepairHours" label="平均维修小时" width="120"><template #default="{ row }">{{ row.averageRepairHours ?? '—' }}</template></el-table-column>
          </el-table>
        </el-card>
      </div>
    </section>

    <section v-if="charts" class="chart-grid">
      <el-card shadow="never" class="chart-card" aria-label="设备状态统计图"><template #header><div class="card-title"><strong>设备状态分布</strong><small>当前可见设备 · eqp_equipment.status</small></div></template><div :ref="node => setNode('equipment', node)" class="chart-canvas" role="img" :aria-label="`设备状态分布：${summary(charts.equipmentStatus)}`"></div></el-card>
      <el-card shadow="never" class="chart-card"><template #header><div class="card-title"><strong>工单状态分布</strong><small>按创建时间 · mnt_work_order.status</small></div></template><div :ref="node => setNode('workorder', node)" class="chart-canvas" role="img" :aria-label="`工单状态分布：${summary(charts.workOrderStatus)}`"></div></el-card>
      <el-card shadow="never" class="chart-card"><template #header><div class="card-title"><strong>故障设备类型排行</strong><small>完成维修 Top 5 · eqp_type.type_name</small></div></template><div :ref="node => setNode('faultTypes', node)" class="chart-canvas" role="img" :aria-label="`故障设备类型排行：${summary(charts.faultEquipmentTypes)}`"></div></el-card>
      <el-card shadow="never" class="chart-card chart-wide" aria-label="维修完成趋势统计图"><template #header><div class="card-title"><strong>维修完成趋势</strong><small>按完成日期 · mnt_work_order.completed_at</small></div></template><div :ref="node => setNode('trend', node)" class="chart-canvas" role="img" aria-label="维修完成趋势图"></div></el-card>
      <el-card shadow="never" class="chart-card chart-wide" aria-label="每日维修完成热力图"><template #header><div class="card-title"><strong>维修完成热力图</strong><small>颜色越深，当天完成工单越多</small></div></template><div :ref="node => setNode('heatmap', node)" class="chart-canvas" role="img" aria-label="每日维修完成热力图"></div></el-card>
      <el-card shadow="never" class="chart-card" aria-label="备件消耗统计图"><template #header><div class="card-title"><strong>备件净消耗排行</strong><small>授权仓库 Top 5 · inv_transaction</small></div></template><div :ref="node => setNode('spares', node)" class="chart-canvas" role="img" :aria-label="`备件净消耗排行：${summary(charts.spareConsumption)}`"></div></el-card>
      <el-card shadow="never" class="chart-card"><template #header><div class="card-title"><strong>SLA 完成情况</strong><small>有解决时限的已完成维修工单</small></div></template><div :ref="node => setNode('sla', node)" class="chart-canvas" role="img" :aria-label="`SLA 完成情况：${summary(charts.slaCompletion)}`"></div></el-card>
    </section>
  </section>
</template>

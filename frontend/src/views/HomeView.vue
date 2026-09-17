<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { getStatisticsKpis, getStatisticsOverview } from '../api/statistics'

const auth = useAuthStore()
const overview = ref<any>(null)
const kpis = ref<any[]>([])
const statisticsLoading = ref(false)
const links = computed(() => [
  { permission: 'workorder:list', to: '/work-orders', title: auth.hasPermission('workorder:assign') ? '处理待办工单' : '查看维修工单', description: '查看派单、维修与验收进度' },
  { permission: 'repair:list', to: '/repair-requests', title: '故障报修', description: '提交报修或跟踪我的报修记录' },
  { permission: 'equipment:list', to: '/equipment', title: '设备台账', description: '查看设备位置、责任人与运行状态' },
  { permission: 'organization:list', to: '/organization', title: '组织结构', description: '维护车间、产线与工位关系' },
  { permission: 'system:user:list', to: '/system/users', title: '用户管理', description: '查看系统用户与岗位信息' },
].filter(item => auth.hasPermission(item.permission)))
const keyKpis = computed(() => kpis.value.filter(item => ['MTBF', 'MTTR', 'ON_TIME_CLOSE_RATE', 'EQUIPMENT_AVAILABILITY'].includes(item.code)))
async function loadStatistics() {
  if (!auth.hasPermission('statistics:view')) return
  statisticsLoading.value = true
  try {
    const [overviewResult, kpiResult] = await Promise.all([
      getStatisticsOverview({ range: 'LAST_30_DAYS' }), getStatisticsKpis({ range: 'LAST_30_DAYS' }),
    ])
    overview.value = overviewResult.data
    kpis.value = kpiResult.data.metrics
  } finally { statisticsLoading.value = false }
}
function metricValue(metric: any) { return metric.available ? `${metric.value} ${metric.unit}` : '数据不足' }
onMounted(loadStatistics)
</script>

<template>
  <section class="module-page">
    <header class="module-header">
      <div><p class="eyebrow">WORKSPACE</p><h1>工作台</h1><p>按当前角色快速进入设备运维业务。</p></div>
    </header>
    <section v-if="auth.hasPermission('statistics:view')" class="home-statistics" v-loading="statisticsLoading">
      <div class="home-statistics-heading"><div><p class="eyebrow">LAST 30 DAYS</p><h2>运维运行概览</h2></div><router-link to="/statistics">查看完整统计分析 →</router-link></div>
      <div v-if="overview" class="home-overview-row">
        <div><small>设备总数</small><strong>{{ overview.equipmentTotal }}</strong></div>
        <div><small>运行设备</small><strong>{{ overview.runningEquipment }}</strong></div>
        <div><small>维修中设备</small><strong>{{ overview.repairingEquipment }}</strong></div>
        <div><small>待处理工单</small><strong>{{ overview.pendingWorkOrders }}</strong></div>
        <div :class="{ warning: overview.openStockWarnings > 0 }"><small>库存预警</small><strong>{{ overview.openStockWarnings }}</strong></div>
      </div>
      <div v-if="keyKpis.length" class="home-kpi-row">
        <div v-for="metric in keyKpis" :key="metric.code"><span>{{ metric.name }}</span><strong>{{ metricValue(metric) }}</strong><small>{{ metric.note }}</small></div>
      </div>
    </section>
    <div class="home-grid">
      <section class="welcome-panel">
        <p class="eyebrow">WELCOME BACK</p>
        <h2>你好，{{ auth.currentUser?.realName }}</h2>
        <p>当前账号的角色与操作权限均由系统实时返回。请选择右侧业务入口继续处理，无权限功能不会显示。</p>
        <div class="role-list"><el-tag v-for="role in auth.roles" :key="role" effect="plain">{{ role }}</el-tag></div>
        <el-alert title="工作台指标由统计服务按当前账号数据范围实时计算。" type="info" :closable="false" show-icon />
      </section>
      <el-card shadow="never">
        <template #header><div class="card-title"><strong>快捷入口</strong><small>根据当前账号权限展示</small></div></template>
        <nav class="quick-links" aria-label="业务快捷入口">
          <router-link v-for="item in links" :key="item.to" :to="item.to" class="quick-link">
            <strong>{{ item.title }}</strong><span>{{ item.description }} →</span>
          </router-link>
          <el-empty v-if="!links.length" description="当前角色暂无可用业务入口" :image-size="72" />
        </nav>
      </el-card>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const links = computed(() => [
  { permission: 'workorder:list', to: '/work-orders', title: auth.hasPermission('workorder:assign') ? '处理待办工单' : '查看维修工单', description: '查看派单、维修与验收进度' },
  { permission: 'repair:list', to: '/repair-requests', title: '故障报修', description: '提交报修或跟踪我的报修记录' },
  { permission: 'equipment:list', to: '/equipment', title: '设备台账', description: '查看设备位置、责任人与运行状态' },
  { permission: 'organization:list', to: '/organization', title: '组织结构', description: '维护车间、产线与工位关系' },
  { permission: 'system:user:list', to: '/system/users', title: '用户管理', description: '查看系统用户与岗位信息' },
].filter(item => auth.hasPermission(item.permission)))
</script>

<template>
  <section class="module-page">
    <header class="module-header">
      <div><p class="eyebrow">WORKSPACE</p><h1>工作台</h1><p>按当前角色快速进入设备运维业务。</p></div>
    </header>
    <div class="home-grid">
      <section class="welcome-panel">
        <p class="eyebrow">WELCOME BACK</p>
        <h2>你好，{{ auth.currentUser?.realName }}</h2>
        <p>当前账号的角色与操作权限均由系统实时返回。请选择右侧业务入口继续处理，无权限功能不会显示。</p>
        <div class="role-list"><el-tag v-for="role in auth.roles" :key="role" effect="plain">{{ role }}</el-tag></div>
        <el-alert title="数据以业务接口为准，本页不展示未经统计模块验证的指标。" type="info" :closable="false" show-icon />
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

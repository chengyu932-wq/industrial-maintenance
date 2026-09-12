<script setup lang="ts">
import { onMounted, shallowRef } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../../api/http'

const users = shallowRef<any[]>([])
const loading = shallowRef(true)
async function load() {
  loading.value = true
  try { users.value = (await http.get('/users')).data }
  catch (e: any) { ElMessage.error(e.response?.data?.message || '用户列表加载失败') }
  finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <section class="module-page">
    <header class="module-header"><div><p class="eyebrow">SYSTEM USERS</p><h1>用户管理</h1><p>查看系统账号、人员姓名与岗位状态。</p></div><div class="header-actions"><el-button :loading="loading" @click="load">刷新</el-button></div></header>
    <el-card shadow="never">
      <template #header><div class="card-title"><strong>用户列表</strong><small>用户权限由角色与后端授权共同决定</small></div></template>
      <el-table v-loading="loading" :data="users" stripe>
        <template #empty><el-empty description="暂无用户数据" :image-size="72"/></template>
        <el-table-column prop="username" label="账号" min-width="160" show-overflow-tooltip/>
        <el-table-column prop="realName" label="姓名" min-width="140" show-overflow-tooltip><template #default="{ row }"><span class="table-primary">{{ row.realName }}</span></template></el-table-column>
        <el-table-column prop="jobTitle" label="岗位" min-width="160" show-overflow-tooltip/>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag></template></el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { onMounted, shallowRef } from 'vue'
import http from '../../api/http'
const users = shallowRef<any[]>([])
const loading = shallowRef(true)
onMounted(async () => { try { users.value = (await http.get('/users')).data } finally { loading.value = false } })
</script>
<template>
  <el-card><template #header><strong>基础用户信息</strong></template>
    <el-table v-loading="loading" :data="users">
      <el-table-column prop="username" label="账号" /><el-table-column prop="realName" label="姓名" />
      <el-table-column prop="jobTitle" label="岗位" /><el-table-column prop="status" label="状态" />
    </el-table>
    <el-button v-permission="'system:user:add'" type="primary" class="table-action">新增用户（权限示例）</el-button>
  </el-card>
</template>

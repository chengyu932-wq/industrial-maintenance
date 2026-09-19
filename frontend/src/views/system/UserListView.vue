<script setup lang="ts">
import { onMounted, reactive, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '../../api/users'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()
const users = shallowRef<any[]>([])
const loading = shallowRef(true)
const actionLoading = shallowRef(false)
const importInput = ref<HTMLInputElement>()
const metadata = shallowRef<any>({ roles: [], skills: [], teams: [], workshops: [] })
const dialog = reactive<any>({
  visible: false, id: null, username: '', password: '', realName: '', phone: '', email: '',
  jobTitle: '', teamId: null, workshopId: null, roleIds: [], skillIds: []
})

async function load() {
  loading.value = true
  try {
    const [list, meta] = await Promise.all([api.list(), api.metadata()])
    users.value = list.data
    metadata.value = meta.data
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '用户列表加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(dialog, {
    visible: true, id: null, username: '', password: '', realName: '', phone: '', email: '',
    jobTitle: '', teamId: null, workshopId: null, roleIds: [], skillIds: []
  })
}

async function openEdit(row: any) {
  actionLoading.value = true
  try {
    Object.assign(dialog, (await api.detail(row.id)).data, { visible: true, password: '' })
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '用户详情加载失败')
  } finally {
    actionLoading.value = false
  }
}

async function save() {
  if (!dialog.realName.trim() || !dialog.roleIds.length || (!dialog.id && (!dialog.username.trim() || dialog.password.length < 8))) {
    ElMessage.warning('请填写账号、姓名、至少一个角色和不少于8位的初始密码')
    return
  }
  actionLoading.value = true
  try {
    const base = {
      realName: dialog.realName, phone: dialog.phone || null, email: dialog.email || null,
      jobTitle: dialog.jobTitle || null, teamId: dialog.teamId, workshopId: dialog.workshopId,
      roleIds: dialog.roleIds, skillIds: dialog.skillIds
    }
    if (dialog.id) await api.update(dialog.id, base)
    else await api.create({ ...base, username: dialog.username, password: dialog.password })
    dialog.visible = false
    ElMessage.success(dialog.id ? '用户已更新' : '用户已创建')
    await load()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    actionLoading.value = false
  }
}

async function toggle(row: any) {
  const target = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  try {
    await ElMessageBox.confirm(`确认${target === 'ENABLED' ? '启用' : '禁用'}账号 ${row.username}？`, '账号状态', { type: 'warning' })
    await api.changeStatus(row.id, target)
    ElMessage.success('账号状态已更新')
    await load()
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '状态更新失败')
  }
}

async function reset(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入不少于8位的新密码', '重置密码', {
      inputType: 'password', inputPattern: /^.{8,72}$/, inputErrorMessage: '密码长度必须为8至72位'
    })
    await api.resetPassword(row.id, value)
    ElMessage.success('密码已使用 BCrypt 重新保存')
  } catch (error: any) {
    if (error !== 'cancel') ElMessage.error(error.response?.data?.message || '密码重置失败')
  }
}

async function downloadTemplate() {
  try {
    const response = await api.importTemplate()
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = '用户导入模板.xlsx'
    link.click()
    URL.revokeObjectURL(url)
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '模板下载失败')
  }
}

async function importUsers(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  actionLoading.value = true
  try {
    const result = (await api.importFile(file)).data
    ElMessage.success(`导入完成：成功 ${result.successRows} 条，失败 ${result.failedRows} 条`)
    if (result.failedRows) {
      const details = result.errors.slice(0, 10).map((item: any) => `第${item.row}行（${item.equipmentNo || '未知账号'}）：${item.message}`).join('\n')
      await ElMessageBox.alert(details, '导入失败明细（最多展示10条）', { type: 'warning' })
    }
    await load()
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '用户导入失败')
  } finally {
    actionLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="module-page">
    <header class="module-header">
      <div><p class="eyebrow">SYSTEM USERS</p><h1>用户管理</h1><p>维护账号、岗位、角色、班组与技能标签。</p></div>
      <div class="header-actions">
        <input ref="importInput" type="file" accept=".xlsx" hidden @change="importUsers">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button v-permission="'system:user:import'" @click="downloadTemplate">下载模板</el-button>
        <el-button v-permission="'system:user:import'" :loading="actionLoading" @click="importInput?.click()">Excel 导入</el-button>
        <el-button v-permission="'system:user:add'" type="primary" @click="openCreate">新增用户</el-button>
      </div>
    </header>

    <el-card shadow="never">
      <template #header><div class="card-title"><strong>用户列表</strong><small>账号启停、角色与技能变更均由后端权限控制并写入操作审计</small></div></template>
      <el-table v-loading="loading" :data="users" stripe>
        <template #empty><el-empty description="暂无用户数据" :image-size="72" /></template>
        <el-table-column prop="username" label="账号" min-width="140" />
        <el-table-column prop="realName" label="姓名" min-width="120"><template #default="{ row }"><span class="table-primary">{{ row.realName }}</span></template></el-table-column>
        <el-table-column prop="jobTitle" label="岗位" min-width="140" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column v-if="auth.hasPermission('system:user:update')" label="操作" width="220" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="openEdit(row)">编辑</el-button><el-button link type="primary" @click="reset(row)">重置密码</el-button><el-button link :type="row.status === 'ENABLED' ? 'danger' : 'success'" @click="toggle(row)">{{ row.status === 'ENABLED' ? '禁用' : '启用' }}</el-button></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialog.visible" :title="dialog.id ? '编辑用户' : '新增用户'" width="720px">
      <el-form label-width="90px"><el-row :gutter="16">
        <el-col :span="12"><el-form-item label="登录账号" required><el-input v-model="dialog.username" :disabled="!!dialog.id" maxlength="50" /></el-form-item></el-col>
        <el-col v-if="!dialog.id" :span="12"><el-form-item label="初始密码" required><el-input v-model="dialog.password" type="password" show-password maxlength="72" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="dialog.realName" maxlength="50" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="岗位"><el-input v-model="dialog.jobTitle" maxlength="50" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="手机号"><el-input v-model="dialog.phone" maxlength="20" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="dialog.email" maxlength="100" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="主要车间"><el-select v-model="dialog.workshopId" clearable style="width:100%"><el-option v-for="item in metadata.workshops" :key="item.id" :label="`${item.code} · ${item.name}`" :value="item.id" /></el-select></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="所属班组"><el-select v-model="dialog.teamId" clearable style="width:100%"><el-option v-for="item in metadata.teams" :key="item.id" :label="`${item.code} · ${item.name}`" :value="item.id" /></el-select></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="角色" required><el-select v-model="dialog.roleIds" multiple style="width:100%"><el-option v-for="item in metadata.roles" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="技能标签"><el-select v-model="dialog.skillIds" multiple filterable style="width:100%"><el-option v-for="item in metadata.skills" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item></el-col>
      </el-row></el-form>
      <template #footer><el-button @click="dialog.visible = false">取消</el-button><el-button type="primary" :loading="actionLoading" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

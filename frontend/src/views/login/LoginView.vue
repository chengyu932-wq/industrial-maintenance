<script setup lang="ts">
import { onMounted, reactive, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const loading = shallowRef(false)
const captchaImage = shallowRef('')
const form = reactive({ username: '', password: '', captchaId: '', captchaCode: '' })

async function loadCaptcha() {
  try {
    const response = await getCaptcha()
    form.captchaId = response.data.captchaId
    form.captchaCode = ''
    captchaImage.value = response.data.image
  } catch { ElMessage.error('验证码获取失败，请检查 Redis 和后端服务') }
}

async function submit() {
  if (!form.username || !form.password || !form.captchaCode) return ElMessage.warning('请完整填写登录信息')
  loading.value = true
  try {
    await auth.signIn({ ...form })
    ElMessage.success('登录成功')
    await router.replace(String(route.query.redirect || '/'))
  } catch (error: any) {
    ElMessage.error(error.response?.data?.message || '登录失败')
    await loadCaptcha()
  } finally { loading.value = false }
}

onMounted(loadCaptcha)
</script>

<template>
  <main class="login-page">
    <section class="login-brand">
      <p class="eyebrow">INDUSTRIAL MAINTENANCE</p>
      <h1>工业设备运维管理系统</h1>
      <p>统一管理设备、人员与运维流程</p>
    </section>
    <section class="login-form-wrap"><el-card class="login-card" shadow="never">
      <template #header><strong>账号登录</strong></template>
      <el-form label-position="top" @keyup.enter="submit">
        <el-form-item label="账号"><el-input v-model.trim="form.username" autocomplete="username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password autocomplete="current-password" /></el-form-item>
        <el-form-item label="验证码">
          <div class="captcha-row">
            <el-input v-model.trim="form.captchaCode" maxlength="4" placeholder="输入验证码" />
            <button class="captcha-image" type="button" title="点击刷新验证码" @click="loadCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="图形验证码" />
              <span v-else>加载中</span>
            </button>
          </div>
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" class="login-submit" @click="submit">登录</el-button>
      </el-form>
      <p class="login-help">连续 5 次账号密码错误将锁定 10 分钟；验证码错误不累计次数。</p>
    </el-card></section>
  </main>
</template>

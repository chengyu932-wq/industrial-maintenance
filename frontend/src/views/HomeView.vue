<script setup lang="ts">
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore()
</script>

<template>
  <section class="welcome-panel">
    <p class="eyebrow">AUTHENTICATION · RBAC</p>
    <h1>你好，{{ auth.currentUser?.realName }}</h1>
    <p>当前安全上下文已建立，Access Token、角色与按钮权限均由后端返回。</p>
    <div class="role-list"><el-tag v-for="role in auth.roles" :key="role">{{ role }}</el-tag></div>
    <router-link v-permission="'system:user:list'" to="/system/users"><el-button type="primary">查看基础用户</el-button></router-link>
  </section>
</template>

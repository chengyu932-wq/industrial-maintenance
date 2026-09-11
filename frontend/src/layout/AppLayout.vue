<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const menuItems = computed(() => auth.menus.filter((item: any) => item.menuType === 'MENU' && item.path))

async function signOut() { await auth.signOut(); await router.replace('/login') }
</script>

<template>
  <el-container class="app-shell">
    <el-aside width="238px" class="app-sidebar">
      <div class="app-logo"><span>IM</span><div><strong>工业运维</strong><small>管理系统</small></div></div>
      <el-menu :default-active="route.path" router class="app-menu">
        <el-menu-item index="/"><span>工作台</span></el-menu-item>
        <el-menu-item v-for="item in menuItems" :key="item.id" :index="item.path">
          <span>{{ item.name }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <div><strong>{{ route.meta.title || '工业设备运维管理' }}</strong></div>
        <el-dropdown>
          <span class="user-trigger">{{ auth.currentUser?.realName || auth.currentUser?.username }} ▾</span>
          <template #dropdown><el-dropdown-menu><el-dropdown-item @click="signOut">退出登录</el-dropdown-item></el-dropdown-menu></template>
        </el-dropdown>
      </el-header>
      <el-main class="app-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

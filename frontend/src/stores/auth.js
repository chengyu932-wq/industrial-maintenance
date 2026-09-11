import { computed, shallowRef } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '../api/auth'
import { authSession, clearTokens, setTokens } from '../auth/session'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = shallowRef(null)
  const roles = computed(() => currentUser.value?.roleCodes || [])
  const permissions = computed(() => currentUser.value?.permissions || [])
  const menus = computed(() => currentUser.value?.menus || [])
  const isAuthenticated = computed(() => Boolean(authSession.accessToken))

  async function signIn(payload) {
    const response = await authApi.login(payload)
    setTokens(response.data.accessToken, response.data.refreshToken)
    currentUser.value = response.data.user
  }

  async function fetchCurrentUser() {
    if (!authSession.accessToken) return null
    const response = await authApi.getCurrentUser()
    currentUser.value = response.data
    return response.data
  }

  async function signOut() {
    try { if (authSession.accessToken) await authApi.logout(authSession.refreshToken) }
    finally { currentUser.value = null; clearTokens() }
  }

  function hasPermission(code) { return permissions.value.includes(code) }
  function hasRole(code) { return roles.value.includes(code) }

  return { currentUser, roles, permissions, menus, isAuthenticated, signIn,
    fetchCurrentUser, signOut, hasPermission, hasRole }
})

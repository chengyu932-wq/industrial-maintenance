import { createRouter, createWebHistory } from 'vue-router'
import pinia from '../stores'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/login/LoginView.vue'), meta: { public: true } },
    { path: '/403', name: 'forbidden', component: () => import('../views/error/ForbiddenView.vue') },
    { path: '/', component: () => import('../layout/AppLayout.vue'), children: [
      { path: '', name: 'home', component: () => import('../views/HomeView.vue') },
      { path: 'system/users', name: 'users', component: () => import('../views/system/UserListView.vue'), meta: { permission: 'system:user:list' } },
    ] },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/error/NotFoundView.vue'), meta: { public: true } },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore(pinia)
  if (to.meta.public) return to.name === 'login' && auth.isAuthenticated ? { name: 'home' } : true
  if (!auth.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }
  try { if (!auth.currentUser) await auth.fetchCurrentUser() }
  catch { return { name: 'login', query: { redirect: to.fullPath } } }
  if (to.meta.permission && !auth.hasPermission(to.meta.permission)) return { name: 'forbidden' }
  if (to.meta.role && !auth.hasRole(to.meta.role)) return { name: 'forbidden' }
  return true
})

export default router

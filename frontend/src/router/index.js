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
      { path: 'organization', name: 'organization', component: () => import('../views/organization/OrganizationView.vue'), meta: { title: '组织结构', permission: 'organization:list' } },
      { path: 'equipment', name: 'equipment', component: () => import('../views/equipment/EquipmentListView.vue'), meta: { title: '设备台账', permission: 'equipment:list' } },
      { path: 'equipment/:id', name: 'equipment-detail', component: () => import('../views/equipment/EquipmentDetailView.vue'), meta: { title: '设备详情', permission: 'equipment:view' } },
      { path: 'repair-requests', name: 'repair-requests', component: () => import('../views/repair/RepairRequestView.vue'), meta: { title: '故障报修', permission: 'repair:list' } },
      { path: 'work-orders', name: 'work-orders', component: () => import('../views/workorder/WorkOrderListView.vue'), meta: { title: '工单中心', permission: 'workorder:list' } },
      { path: 'work-orders/:id', name: 'work-order-detail', component: () => import('../views/workorder/WorkOrderDetailView.vue'), meta: { title: '工单详情', permission: 'workorder:view' } },
      { path: 'maintenance-plans', name: 'maintenance-plans', component: () => import('../views/maintenance/MaintenancePlanView.vue'), meta: { title: '预防性维护', permission: 'maintenance:plan:list' } },
      { path: 'warehouses', name: 'warehouses', component: () => import('../views/inventory/WarehouseView.vue'), meta: { title: '仓库管理', permission: 'warehouse:list' } },
      { path: 'spare-parts', name: 'spare-parts', component: () => import('../views/inventory/SparePartView.vue'), meta: { title: '备件管理', permission: 'spare:list' } },
      { path: 'inventory/stocks', name: 'inventory-stocks', component: () => import('../views/inventory/InventoryView.vue'), meta: { title: '库存管理', permission: 'inventory:stock:list' } },
      { path: 'inventory/transactions', name: 'inventory-transactions', component: () => import('../views/inventory/TransactionView.vue'), meta: { title: '库存流水', permission: 'inventory:transaction:list' } },
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

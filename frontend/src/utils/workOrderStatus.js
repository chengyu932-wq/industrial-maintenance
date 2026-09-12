export const workOrderStatuses = {
  PENDING_ASSIGN: { label: '待派单', type: 'warning' }, ASSIGNED: { label: '已派单', type: 'primary' },
  PROCESSING: { label: '处理中', type: 'primary' }, SUSPENDED: { label: '已挂起', type: 'info' },
  PENDING_ACCEPT: { label: '待验收', type: 'warning' }, COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
}
export const repairPriorities = { URGENT: { label: '紧急', type: 'danger' }, IMPORTANT: { label: '重要', type: 'warning' }, NORMAL: { label: '一般', type: 'info' } }
export const workOrderStatusLabel = (code) => workOrderStatuses[code]?.label || code
export const workOrderStatusType = (code) => workOrderStatuses[code]?.type || 'info'
export const priorityLabel = (code) => repairPriorities[code]?.label || code
export const priorityType = (code) => repairPriorities[code]?.type || 'info'

export const workOrderStatuses = Object.freeze({
  PENDING_ASSIGN: { label: '待派单', type: 'warning', order: 1, step: 0 },
  ASSIGNED: { label: '已派单', type: 'primary', order: 2, step: 1 },
  PROCESSING: { label: '处理中', type: 'primary', order: 3, step: 2 },
  SUSPENDED: { label: '已挂起', type: 'info', order: 4, step: 2 },
  PENDING_ACCEPT: { label: '待验收', type: 'warning', order: 5, step: 3 },
  COMPLETED: { label: '已完成', type: 'success', order: 6, step: 5 },
  CANCELLED: { label: '已取消', type: 'info', order: 7, step: 0 },
})
export const repairPriorities = Object.freeze({
  URGENT: { label: '紧急', type: 'danger', order: 1 },
  IMPORTANT: { label: '重要', type: 'warning', order: 2 },
  NORMAL: { label: '一般', type: 'info', order: 3 },
})
export const repairRequestStatuses = Object.freeze({
  CONVERTED: { label: '已转工单', type: 'primary', order: 1 },
  CANCELLED: { label: '已取消', type: 'info', order: 2 },
})
export const workOrderStatusLabel = (code) => workOrderStatuses[code]?.label || code
export const workOrderStatusType = (code) => workOrderStatuses[code]?.type || 'info'
export const workOrderStep = (code) => workOrderStatuses[code]?.step ?? 0
export const priorityLabel = (code) => repairPriorities[code]?.label || code
export const priorityType = (code) => repairPriorities[code]?.type || 'info'
export const repairRequestStatusLabel = (code) => repairRequestStatuses[code]?.label || code
export const repairRequestStatusType = (code) => repairRequestStatuses[code]?.type || 'info'

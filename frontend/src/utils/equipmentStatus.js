export const equipmentStatuses = Object.freeze({
  PENDING: { label: '待启用', type: 'info', order: 1 },
  RUNNING: { label: '运行中', type: 'success', order: 2 },
  FAULT: { label: '故障', type: 'danger', order: 3 },
  REPAIRING: { label: '维修中', type: 'warning', order: 4 },
  STOPPED: { label: '已停用', type: 'info', order: 5 },
  SCRAPPED: { label: '已报废', type: 'danger', order: 6 },
})
export const statusLabel = (code) => equipmentStatuses[code]?.label || code
export const statusTagType = (code) => equipmentStatuses[code]?.type || 'info'
export const manualStatusActions = (code) => ({
  PENDING: [{ code: 'RUNNING', label: '启用' }],
  RUNNING: [{ code: 'STOPPED', label: '停用' }],
  STOPPED: [{ code: 'RUNNING', label: '恢复运行' }],
}[code] || [])

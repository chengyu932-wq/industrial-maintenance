export const equipmentStatuses = Object.freeze({
  PENDING: { label: '待启用', type: 'info' },
  RUNNING: { label: '运行', type: 'success' },
  FAULT: { label: '故障', type: 'danger' },
  REPAIRING: { label: '维修中', type: 'warning' },
  STOPPED: { label: '停用', type: 'info' },
  SCRAPPED: { label: '报废', type: 'danger' },
})
export const statusLabel = (code) => equipmentStatuses[code]?.label || code
export const statusTagType = (code) => equipmentStatuses[code]?.type || 'info'
export const manualStatusActions = (code) => ({
  PENDING: [{ code: 'RUNNING', label: '启用' }],
  RUNNING: [{ code: 'STOPPED', label: '停用' }],
  STOPPED: [{ code: 'RUNNING', label: '恢复运行' }],
}[code] || [])

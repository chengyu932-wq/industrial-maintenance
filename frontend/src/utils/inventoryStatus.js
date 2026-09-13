export const transactionTypes = {
  INBOUND: { label: '入库', type: 'success' }, OUTBOUND: { label: '普通出库', type: 'warning' },
  ISSUE: { label: '工单领用', type: 'danger' }, RETURN: { label: '退库', type: 'success' },
  TRANSFER_IN: { label: '调拨入', type: 'success' }, TRANSFER_OUT: { label: '调拨出', type: 'warning' },
  STOCKTAKE: { label: '盘点调整', type: 'primary' }, SCRAP: { label: '备件报废', type: 'info' },
}
export const transactionLabel = (value) => transactionTypes[value]?.label || value || '-'
export const transactionTagType = (value) => transactionTypes[value]?.type || 'info'
export const stockStatusLabel = (value) => value === 'LOW' ? '低于安全库存' : '库存正常'
export const stockStatusType = (value) => value === 'LOW' ? 'danger' : 'success'
export const issueStatusLabel = (value) => ({ ISSUED: '已领用', PARTIAL_RETURN: '部分退库', RETURNED: '已全部退库' }[value] || value)

import http from './http'
export const page = (params) => http.get('/repair-requests', { params })
export const detail = (id) => http.get(`/repair-requests/${id}`)
export const create = (data) => http.post('/repair-requests', data)
export const cancel = (id, data) => http.post(`/repair-requests/${id}/cancel`, data)

export const confirm = (id) => http.post(`/repair-requests/${id}/confirm`)
export const attachments = (id) => http.get(`/repair-requests/${id}/attachments`)
export const uploadAttachment = (id, file) => { const data = new FormData(); data.append('file', file); return http.post(`/repair-requests/${id}/attachments`, data) }
export const downloadAttachment = (id, attachmentId) => http.get(`/repair-requests/${id}/attachments/${attachmentId}`, { responseType: 'blob' })

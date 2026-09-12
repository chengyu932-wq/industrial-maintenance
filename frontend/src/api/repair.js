import http from './http'
export const page = (params) => http.get('/repair-requests', { params })
export const detail = (id) => http.get(`/repair-requests/${id}`)
export const create = (data) => http.post('/repair-requests', data)
export const cancel = (id, data) => http.post(`/repair-requests/${id}/cancel`, data)

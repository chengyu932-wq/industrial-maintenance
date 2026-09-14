import http from './http'
export const page = (params) => http.get('/knowledge', { params })
export const detail = (id) => http.get(`/knowledge/${id}`)
export const createFromWorkOrder = (workOrderId) => http.post(`/knowledge/from-work-order/${workOrderId}`)
export const update = (id, data) => http.put(`/knowledge/${id}`, data)
export const submit = (id) => http.post(`/knowledge/${id}/submit`)
export const review = (id, data) => http.post(`/knowledge/${id}/review`, data)

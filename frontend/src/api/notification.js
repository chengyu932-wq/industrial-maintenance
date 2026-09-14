import http from './http'
export const page = (params) => http.get('/notifications', { params })
export const unreadCount = () => http.get('/notifications/unread-count')
export const read = (id) => http.post(`/notifications/${id}/read`)
export const readAll = () => http.post('/notifications/read-all')

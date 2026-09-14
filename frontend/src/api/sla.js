import http from './http'
export const rules = () => http.get('/sla/rules')
export const updateRule = (id, data) => http.put(`/sla/rules/${id}`, data)
export const events = (params) => http.get('/sla/events', { params })
export const handleEvent = (id) => http.post(`/sla/events/${id}/handle`)

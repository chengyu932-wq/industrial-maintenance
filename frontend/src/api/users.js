import http from './http'
export const list = () => http.get('/users')
export const detail = (id) => http.get(`/users/${id}`)
export const metadata = () => http.get('/users/metadata')
export const create = (data) => http.post('/users', data)
export const update = (id, data) => http.put(`/users/${id}`, data)
export const changeStatus = (id, status) => http.put(`/users/${id}/status`, { status })
export const resetPassword = (id, password) => http.put(`/users/${id}/password`, { password })
export const importTemplate = () => http.get('/users/import-template', { responseType: 'blob' })
export const importFile = (file) => {
  const data = new FormData()
  data.append('file', file)
  return http.post('/users/import', data, { headers: { 'Content-Type': 'multipart/form-data' } })
}

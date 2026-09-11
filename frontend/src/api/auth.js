import http from './http'

export const getCaptcha = () => http.get('/auth/captcha')
export const login = (payload) => http.post('/auth/login', payload, { skipAuthRefresh: true })
export const refresh = (refreshToken) => http.post('/auth/refresh', { refreshToken }, { skipAuthRefresh: true })
export const logout = (refreshToken) => http.post('/auth/logout', { refreshToken })
export const getCurrentUser = () => http.get('/auth/me')

import axios from 'axios'
import { authSession, clearTokens, setTokens } from '../auth/session'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000,
})

http.interceptors.request.use(
  (config) => {
    if (authSession.accessToken) config.headers.Authorization = `Bearer ${authSession.accessToken}`
    return config
  },
  (error) => Promise.reject(error),
)

let refreshPromise = null

http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    if (status === 401 && !original?.skipAuthRefresh && !original?._retried && authSession.refreshToken) {
      original._retried = true
      if (!refreshPromise) {
        refreshPromise = axios.post(`${http.defaults.baseURL}/auth/refresh`,
          { refreshToken: authSession.refreshToken }, { timeout: http.defaults.timeout })
          .then(({ data }) => {
            setTokens(data.data.accessToken, data.data.refreshToken)
            return data.data.accessToken
          }).finally(() => { refreshPromise = null })
      }
      try {
        const accessToken = await refreshPromise
        original.headers.Authorization = `Bearer ${accessToken}`
        return http(original)
      } catch (refreshError) {
        clearTokens()
        if (window.location.pathname !== '/login') window.location.assign('/login')
        return Promise.reject(refreshError)
      }
    }
    if (status === 403 && window.location.pathname !== '/403') window.location.assign('/403')
    return Promise.reject(error)
  },
)

export default http

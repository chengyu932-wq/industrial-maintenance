import { reactive } from 'vue'

const ACCESS_KEY = 'maintenance.accessToken'
const REFRESH_KEY = 'maintenance.refreshToken'

export const authSession = reactive({
  accessToken: localStorage.getItem(ACCESS_KEY) || '',
  refreshToken: localStorage.getItem(REFRESH_KEY) || '',
})

export function setTokens(accessToken, refreshToken) {
  authSession.accessToken = accessToken
  authSession.refreshToken = refreshToken
  localStorage.setItem(ACCESS_KEY, accessToken)
  localStorage.setItem(REFRESH_KEY, refreshToken)
}

export function clearTokens() {
  authSession.accessToken = ''
  authSession.refreshToken = ''
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

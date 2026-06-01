export function getToken() {
  return uni.getStorageSync('token') || ''
}

export function setToken(token) {
  uni.setStorageSync('token', token)
}

export function clearToken() {
  uni.removeStorageSync('token')
}

export function checkLogin(redirect = true) {
  const token = getToken()
  if (!token) {
    if (redirect) {
      uni.reLaunch({ url: '/pages/login/index' })
    }
    return ''
  }
  return token
}

export function logout() {
  clearToken()
  uni.reLaunch({ url: '/pages/login/index' })
}

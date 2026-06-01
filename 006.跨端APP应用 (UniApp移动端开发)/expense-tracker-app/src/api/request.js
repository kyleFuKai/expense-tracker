import { getToken, clearToken } from '../utils/storage'

const API_BASE = 'http://localhost:8080'

export function request(url, options = {}) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    const headers = {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
    if (token) {
      headers['Authorization'] = 'Bearer ' + token
    }

    uni.request({
      url: API_BASE + '/api' + url,
      method: options.method || 'GET',
      data: options.data || {},
      header: headers,
      success: (res) => {
        if (res.statusCode === 401) {
          clearToken()
          uni.reLaunch({ url: '/pages/login/index' })
          reject(new Error('未登录'))
          return
        }
        resolve(res.data)
      },
      fail: (err) => {
        reject(err)
      }
    })
  })
}

export default request

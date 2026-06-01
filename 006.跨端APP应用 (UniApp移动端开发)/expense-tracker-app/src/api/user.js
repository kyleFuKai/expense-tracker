import { request } from './request'

export function getUserProfile() {
  return request('/user')
}

export function updateUserProfile(data) {
  return request('/user', { method: 'PUT', data })
}

export function changePassword(data) {
  return request('/auth/change-password', { method: 'POST', data })
}

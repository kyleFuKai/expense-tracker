import { request } from './request'

export function getTags() {
  return request('/finance/tags')
}

export function createTag(data) {
  return request('/finance/tags', { method: 'POST', data })
}

export function updateTag(id, data) {
  return request(`/finance/tags/${id}`, { method: 'PUT', data })
}

export function deleteTag(id) {
  return request(`/finance/tags/${id}`, { method: 'DELETE' })
}

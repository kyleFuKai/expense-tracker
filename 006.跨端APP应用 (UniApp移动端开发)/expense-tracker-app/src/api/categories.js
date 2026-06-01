import { request } from './request'

export function getCategories(type) {
  return request(`/categories?type=${type}`)
}

export function createCategory(data) {
  return request('/categories', { method: 'POST', data })
}

export function updateCategory(id, data) {
  return request(`/categories/${id}`, { method: 'PUT', data })
}

export function deleteCategory(id) {
  return request(`/categories/${id}`, { method: 'DELETE' })
}

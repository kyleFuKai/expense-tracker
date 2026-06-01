import { request } from './request'

export function getDashboard(month) {
  return request(`/budgets/dashboard?month=${month}`)
}

export function getBudgets(type) {
  return request(`/budgets?type=${type}`)
}

export function createBudget(data) {
  return request('/budgets', { method: 'POST', data })
}

export function deleteBudget(id) {
  return request(`/budgets/${id}`, { method: 'DELETE' })
}

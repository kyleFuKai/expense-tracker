import { request } from './request'

export function getBills(params) {
  const qs = Object.keys(params)
    .filter(k => params[k] !== null && params[k] !== undefined)
    .map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`)
    .join('&')
  return request('/bills?' + qs)
}

export function getBillById(id) {
  return request(`/bills/${id}`)
}

export function createBill(data) {
  return request('/bills', { method: 'POST', data })
}

export function updateBill(id, data) {
  return request(`/bills/${id}`, { method: 'PUT', data })
}

export function deleteBill(id) {
  return request(`/bills/${id}`, { method: 'DELETE' })
}

export function monthlyStats(month) {
  return request(`/bills/stats/month?month=${month}`)
}

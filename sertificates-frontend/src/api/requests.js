import { api } from 'boot/axios'

export function getRequests() {
  return api.get('/requests')
}

export function getRequestById(id) {
  return api.get(`/requests/${id}`)
}

export function createRequest(payload) {
  return api.post('/requests', payload)
}

export function updateRequest(id, payload) {
  return api.put(`/requests/${id}`, payload)
}

export function deleteRequest(id) {
  return api.delete(`/requests/${id}`)
}

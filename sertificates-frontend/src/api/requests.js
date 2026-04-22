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

export function acceptRequest(id) {
  return api.patch(`/requests/${id}/accept`)
}

export function updateStudentComment(id, comment) {
  return api.patch(`/requests/${id}/student-comment`, { comment })
}

export function updateSecretaryComment(id, comment) {
  return api.patch(`/requests/${id}/secretary-comment`, { comment })
}

export function updateRequestStatus(id, status) {
  return api.patch(`/requests/${id}/status`, { status })
}

export function cancelRequest(id) {
  return api.patch(`/requests/${id}/cancel`)
}

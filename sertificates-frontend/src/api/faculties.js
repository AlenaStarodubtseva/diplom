import { api } from 'boot/axios'

export function getFaculties() {
  return api.get('/faculties')
}

export function getFacultyById(id) {
  return api.get(`/faculties/${id}`)
}

export function createFaculty(payload) {
  return api.post('/faculties', payload)
}

export function updateFaculty(id, payload) {
  return api.put(`/faculties/${id}`, payload)
}

export function toggleFacultyActive(id) {
  return api.patch(`/faculties/${id}/toggle-active`)
}

export function deleteFaculty(id) {
  return api.delete(`/faculties/${id}`)
}

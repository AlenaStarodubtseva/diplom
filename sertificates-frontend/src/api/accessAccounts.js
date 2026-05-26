import axios from 'axios'

const API_URL = 'http://localhost:8081/api/access-accounts'

export function getAccessAccounts() {
  return axios.get(API_URL)
}

export function createAccessAccount(data) {
  return axios.post(API_URL, data)
}

export function updateAccessAccount(id, data) {
  return axios.put(`${API_URL}/${id}`, data)
}

export function toggleAccessAccountActive(id) {
  return axios.patch(`${API_URL}/${id}/toggle-active`)
}

export function deleteAccessAccount(id) {
  return axios.delete(`${API_URL}/${id}`)
}

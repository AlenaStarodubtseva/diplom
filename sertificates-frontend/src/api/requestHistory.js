import { api } from 'boot/axios'

export function getRequestHistory() {
  return api.get('/request-history')
}

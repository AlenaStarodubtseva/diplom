import { api } from 'boot/axios'

export function getFaculties() {
  return api.get('/faculties')
}

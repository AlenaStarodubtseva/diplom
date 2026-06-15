import { api } from 'boot/axios'

export function getRegistrationNumbersByRequestId(requestId) {
  return api.get(`/api/request-registration-numbers/request/${requestId}`)
}

export function getRegistrationNumbersByRequestIds(requestIds) {
  const params = new URLSearchParams()

  requestIds.forEach(id => {
    params.append('requestIds', id)
  })

  return api.get(`/api/request-registration-numbers?${params.toString()}`)
}

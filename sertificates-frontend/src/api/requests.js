import { api } from 'boot/axios'

function getAuthPayload() {
  try {
    const raw = localStorage.getItem('certificates-auth')

    if (!raw) {
      return {
        actorLogin: null,
        actorFullName: null,
        actorRole: null
      }
    }

    const parsed = JSON.parse(raw)

    return {
      actorLogin: parsed.login || null,
      actorFullName: parsed.login || null,
      actorRole: parsed.role || null
    }
  } catch {
    return {
      actorLogin: null,
      actorFullName: null,
      actorRole: null
    }
  }
}

export function getRequests() {
  const auth = getAuthPayload()

  return api.get('/requests', {
    params: {
      actorLogin: auth.actorLogin,
      actorRole: auth.actorRole
    }
  })
}

export function getRequestById(id) {
  const auth = getAuthPayload()

  return api.get(`/requests/${id}`, {
    params: {
      actorLogin: auth.actorLogin,
      actorRole: auth.actorRole
    }
  })
}

export function createRequest(payload) {
  return api.post('/requests', payload)
}

export function updateRequest(id, payload) {
  return api.put(`/requests/${id}`, {
    ...payload,
    ...getAuthPayload()
  })
}

export function deleteRequest(id) {
  return api.delete(`/requests/${id}`)
}

export function acceptRequest(id) {
  return api.patch(`/requests/${id}/accept`, {
    ...getAuthPayload()
  })
}

export function updateStudentComment(id, comment) {
  return api.patch(`/requests/${id}/student-comment`, {
    comment,
    ...getAuthPayload()
  })
}

export function updateSecretaryComment(id, comment) {
  return api.patch(`/requests/${id}/secretary-comment`, {
    comment,
    ...getAuthPayload()
  })
}

export function updateRequestStatus(id, status, comment = null) {
  return api.patch(`/requests/${id}/status`, {
    status,
    comment,
    ...getAuthPayload()
  })
}

export function cancelRequest(id) {
  return api.patch(`/requests/${id}/cancel`, {
    ...getAuthPayload()
  })
}

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

export function uploadRequestScan(id, file) {
  const formData = new FormData()
  const auth = getAuthPayload()

  formData.append('file', file)
  formData.append('actorLogin', auth.actorLogin || '')
  formData.append('actorFullName', auth.actorFullName || '')
  formData.append('actorRole', auth.actorRole || '')

  return api.post(`/requests/${id}/scan`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function downloadRequestScan(id) {
  const auth = getAuthPayload()

  return api.get(`/requests/${id}/scan`, {
    params: {
      actorLogin: auth.actorLogin,
      actorRole: auth.actorRole
    },
    responseType: 'blob'
  })
}

export function deleteRequestScan(id) {
  return api.delete(`/requests/${id}/scan`, {
    data: {
      ...getAuthPayload()
    }
  })
}

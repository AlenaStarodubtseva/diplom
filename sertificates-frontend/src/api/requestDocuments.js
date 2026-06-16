import axios from 'axios'

const API_URL = 'http://localhost:8081/api/request-documents'

function getAuthPayload() {
  try {
    const raw = localStorage.getItem('certificates-auth')

    if (!raw) {
      return {
        actorLogin: null,
        actorRole: null
      }
    }

    const parsed = JSON.parse(raw)

    return {
      actorLogin: parsed.login || null,
      actorRole: parsed.role || null
    }
  } catch {
    return {
      actorLogin: null,
      actorRole: null
    }
  }
}

export function generateCommonRequestDocument(requestIds) {
  return axios.post(
    `${API_URL}/common`,
    {
      requestIds,
      ...getAuthPayload()
    },
    {
      responseType: 'blob'
    }
  )
}

export function generatePrintCertificates(requestIds) {
  return axios.post(
    `${API_URL}/print-certificates`,
    {
      requestIds,
      ...getAuthPayload()
    },
    {
      responseType: 'blob'
    }
  )
}

export function previewPrintCertificates(requestIds) {
  return axios.post(
    `${API_URL}/print-certificates-preview`,
    {
      requestIds,
      ...getAuthPayload()
    }
  )
}

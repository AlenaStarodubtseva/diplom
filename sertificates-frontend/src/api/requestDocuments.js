import axios from 'axios'

const API_URL = 'http://localhost:8081/api/request-documents'

export function generateCommonRequestDocument(requestIds) {
  return axios.post(
    `${API_URL}/common`,
    {
      requestIds
    },
    {
      responseType: 'blob'
    }
  )
}

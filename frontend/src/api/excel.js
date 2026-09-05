import request from '../utils/request'

export const getExcelContext = () => request.get('/excel/context')
export const initializeBatch = () => request.post('/excel/batches/initialize')
export const importExcel = (type, batchId, file) => {
  const form = new FormData()
  form.append('file', file)
  return request.post(`/excel/imports/${type}`, form, { params: { batchId }, timeout: 300000 })
}
export const downloadExcel = async (path, name, params) => {
  const blob = await request.get(`/excel/${path}`, { responseType: 'blob', params })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = name
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

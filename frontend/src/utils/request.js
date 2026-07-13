import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

let isRedirecting = false

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        localStorage.removeItem('username')
        localStorage.removeItem('studentId')
        if (!isRedirecting) {
          isRedirecting = true
          router.push('/login').finally(() => { isRedirecting = false })
        }
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    if (!error.response) {
      ElMessage.error('网络连接异常，请检查后端是否启动')
      return Promise.reject(error)
    }
    const status = error.response.status
    if (status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('username')
      localStorage.removeItem('studentId')
      if (!isRedirecting) {
        isRedirecting = true
        ElMessage.warning('登录已过期，请重新登录')
        router.push('/login').finally(() => { isRedirecting = false })
      }
    } else if (status === 500) {
      ElMessage.error('服务器内部错误')
    } else if (status === 404) {
      ElMessage.error('请求的资源不存在')
    } else {
      ElMessage.error(error.response.data?.message || `请求失败(${status})`)
    }
    return Promise.reject(error)
  }
)

export default request

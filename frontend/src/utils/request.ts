import axios from 'axios'
import { ElMessage } from 'element-plus'

// 生成请求ID（用于链路追踪）
function generateRequestId(): string {
  return 'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/x/g, () =>
    Math.floor(Math.random() * 16).toString(16)
  )
}

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 生成请求ID并添加到请求头
    const requestId = generateRequestId()
    config.headers['X-Request-Id'] = requestId

    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    // 如果是文件下载，直接返回
    if (response.config.responseType === 'blob') {
      return response
    }

    const res = response.data

    // 业务错误处理
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')

      // 未授权，跳转登录
      if (res.code === 401) {
        localStorage.removeItem('token')
        window.location.href = '/login'
      }

      return Promise.reject(new Error(res.message))
    }

    return res
  },
  (error) => {
    console.error('请求错误:', error)
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service

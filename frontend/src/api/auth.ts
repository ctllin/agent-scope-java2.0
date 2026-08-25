import request from '@/utils/request'

// 登录
export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

// 快捷登录
export function quickLogin() {
  return request.post('/auth/quick-login')
}

// 获取当前用户信息
export function getCurrentUser() {
  return request.get('/auth/current')
}

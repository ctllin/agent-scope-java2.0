import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, quickLogin as quickLoginApi, getCurrentUser } from '@/api/auth'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<User | null>(null)

  // 登录
  async function loginAction(params: { username: string; password: string }) {
    try {
      const res: any = await loginApi(params)
      if (res.code === 200) {
        token.value = res.data.token
        user.value = res.data.user
        localStorage.setItem('token', res.data.token)
        return true
      }
      return false
    } catch (error) {
      console.error('登录失败:', error)
      return false
    }
  }

  // 快捷登录
  async function quickLoginAction() {
    try {
      const res: any = await quickLoginApi()
      if (res.code === 200) {
        token.value = res.data.token
        user.value = res.data.user
        localStorage.setItem('token', res.data.token)
        return true
      }
      return false
    } catch (error) {
      console.error('快捷登录失败:', error)
      return false
    }
  }

  // 获取当前用户信息
  async function fetchUserInfo() {
    try {
      const res: any = await getCurrentUser()
      if (res.code === 200) {
        user.value = res.data.user
        return true
      }
      return false
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return false
    }
  }

  // 退出登录
  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    user,
    loginAction,
    quickLoginAction,
    fetchUserInfo,
    logout
  }
})

<template>
  <div class="login-container" @click="handleClick">
    <div class="login-card" @click.stop>
      <h2 class="login-title">Agent-Scope AI Platform</h2>
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tip">
        <el-text type="info" size="small">双击空白区域可快捷登录（仅开发环境）</el-text>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { useUserStore } from '@/store/user'
import { quickLogin } from '@/api'

const router = useRouter()
const userStore = useUserStore()

// 登录表单引用
const loginFormRef = ref<FormInstance>()

// 加载状态
const loading = ref(false)

// 双击次数计数
let clickCount = 0
let clickTimer: ReturnType<typeof setTimeout> | null = null

// 登录表单
const loginForm = reactive({
  username: '',
  password: ''
})

// 表单验证规则
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const success = await userStore.loginAction(loginForm)
      if (success) {
        ElMessage.success('登录成功')
        router.push('/')
      } else {
        ElMessage.error('登录失败')
      }
    } catch (error) {
      ElMessage.error('登录失败')
    } finally {
      loading.value = false
    }
  })
}

// 处理单击（用于检测双击）
const handleClick = async () => {
  clickCount++
  
  if (clickCount === 1) {
    // 第一次点击，启动定时器（1秒内完成双击）
    clickTimer = setTimeout(() => {
      clickCount = 0
    }, 1000)
  } else if (clickCount === 2) {
    // 第二次点击，执行快捷登录
    if (clickTimer) {
      clearTimeout(clickTimer)
    }
    clickCount = 0
    
    loading.value = true
    try {
      const res = await quickLogin()
      if (res.code === 200) {
        userStore.token = res.data.token
        userStore.user = res.data.user
        localStorage.setItem('token', res.data.token)
        ElMessage.success('快捷登录成功')
        router.push('/')
      } else {
        ElMessage.error(res.message || '快捷登录失败')
      }
    } catch (error) {
      ElMessage.error('快捷登录失败')
    } finally {
      loading.value = false
    }
  }
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  font-size: 24px;
}

.login-form {
  .el-input {
    height: 45px;
  }
  
  .login-button {
    width: 100%;
    height: 45px;
  }
}

.login-tip {
  text-align: center;
  margin-top: 20px;
}
</style>

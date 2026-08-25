<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="aside">
      <div class="logo">
        <h3>Agent-Scope</h3>
      </div>
      <el-menu
        :default-active="route.path"
        router
        class="aside-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI对话</span>
        </el-menu-item>
        <el-menu-item index="/knowledge-base">
          <el-icon><Collection /></el-icon>
          <span>知识库</span>
        </el-menu-item>
        <el-menu-item index="/asr">
          <el-icon><Microphone /></el-icon>
          <span>语音识别</span>
        </el-menu-item>
        <el-menu-item index="/tts">
          <el-icon><Reading /></el-icon>
          <span>语音合成</span>
        </el-menu-item>
        <el-menu-item v-if="userStore.user?.root" index="/user">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.user?.nickname || userStore.user?.username }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 内容区 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 页面刷新后 Pinia 状态丢失，用 token 恢复用户信息
onMounted(async () => {
  if (userStore.token && !userStore.user) {
    await userStore.fetchUserInfo()
  }
})

// 处理下拉菜单命令
const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #304156;
  color: #fff;
  
  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #263445;
    
    h3 {
      color: #fff;
      margin: 0;
    }
  }
  
  .aside-menu {
    border-right: none;
    background-color: #304156;
    
    .el-menu-item {
      color: #bfcbd9;
      
      &:hover {
        background-color: #263445;
      }
      
      &.is-active {
        background-color: #1890ff;
        color: #fff;
      }
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  padding: 0 20px;
  
  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      cursor: pointer;
      
      .el-icon {
        margin-right: 5px;
      }
    }
  }
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>

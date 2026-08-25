<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>用户总数</span>
              <el-icon><User /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ stats.userCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>知识库数量</span>
              <el-icon><Collection /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ stats.knowledgeBaseCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>对话会话数</span>
              <el-icon><ChatDotRound /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ stats.chatSessionCount }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>文档总数</span>
              <el-icon><Document /></el-icon>
            </div>
          </template>
          <div class="card-value">{{ stats.documentCount }}</div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>欢迎使用 Agent-Scope AI Platform</span>
            </div>
          </template>
          <div class="welcome-content">
            <p>这是一个基于 Spring Boot + Vue3 构建的企业级 AI 对话与管理平台。</p>
            <p>主要功能：</p>
            <ul>
              <li>用户与权限管理（RBAC）</li>
              <li>AI对话与多模型路由</li>
              <li>知识库管理与RAG对话</li>
              <li>本地Embedding向量化</li>
            </ul>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

// 统计数据
const stats = ref({
  userCount: 0,
  knowledgeBaseCount: 0,
  chatSessionCount: 0,
  documentCount: 0
})

onMounted(() => {
  // TODO: 从API获取统计数据
  stats.value = {
    userCount: 10,
    knowledgeBaseCount: 3,
    chatSessionCount: 25,
    documentCount: 150
  }
})
</script>

<style scoped lang="scss">
.dashboard {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .card-value {
    font-size: 32px;
    font-weight: bold;
    color: #409eff;
    text-align: center;
  }
  
  .welcome-content {
    p {
      margin-bottom: 10px;
    }
    
    ul {
      padding-left: 20px;
      
      li {
        margin-bottom: 5px;
      }
    }
  }
}
</style>

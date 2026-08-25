<template>
  <div class="chat-container">
    <!-- 左侧会话列表 -->
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <el-button type="primary" @click="createSession">
          <el-icon><Plus /></el-icon>
          新建对话
        </el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSession?.id === session.id }"
          @click="selectSession(session)"
        >
          <div class="session-title">{{ session.title }}</div>
          <div class="session-time">{{ formatDate(session.createdAt) }}</div>
        </div>
      </div>
    </div>
    
    <!-- 右侧对话区 -->
    <div class="chat-main">
      <!-- 对话头部 -->
      <div class="chat-header">
        <div class="header-left">
          <el-select v-model="currentModel" placeholder="选择模型" style="width: 150px;">
            <el-option
              v-for="model in models"
              :key="model.value"
              :label="model.label"
              :value="model.value"
            />
          </el-select>
          <el-select v-model="currentMode" placeholder="对话模式" style="width: 150px; margin-left: 10px;">
            <el-option label="普通对话" value="normal" />
            <el-option label="知识库对话" value="knowledge_base" />
          </el-select>
          <el-select
            v-if="currentMode === 'knowledge_base'"
            v-model="currentKnowledgeBase"
            placeholder="选择知识库"
            style="width: 150px; margin-left: 10px;"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
        </div>
        <div class="header-right">
          <el-button @click="deleteSession">
            <el-icon><Delete /></el-icon>
            删除对话
          </el-button>
        </div>
      </div>
      
      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef">
        <div
          v-for="(message, index) in messages"
          :key="message.id || index"
          class="message-item"
          :class="message.role"
        >
          <div class="message-avatar">
            <el-icon v-if="message.role === 'user'" :size="24"><User /></el-icon>
            <el-icon v-else :size="24"><Monitor /></el-icon>
          </div>
          <div class="message-wrapper">
            <div class="message-content">{{ message.content }}</div>
            <div class="message-meta">
              <span class="message-time">{{ formatTime(message.createdAt) }}</span>
              <span v-if="message.role === 'assistant' && message.duration" class="message-duration">
                {{ formatDuration(message.duration) }}
              </span>
            </div>
          </div>
        </div>
        <!-- AI思考中 -->
        <div v-if="sending" class="message-item assistant">
          <div class="message-avatar">
            <el-icon :size="24"><Monitor /></el-icon>
          </div>
          <div class="message-wrapper">
            <div class="message-content thinking">
              <span class="dot"></span>
              <span class="dot"></span>
              <span class="dot"></span>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 输入框 -->
      <div class="chat-input">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="输入消息... (Ctrl+Enter 发送)"
          @keyup.enter.ctrl="sendMessage"
          :disabled="sending"
        />
        <el-button
          type="primary"
          :loading="sending"
          @click="sendMessage"
          :disabled="!inputMessage.trim()"
          style="margin-top: 10px;"
        >
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getChatSessionList,
  createChatSession,
  deleteChatSession,
  sendMessage as sendApi,
  getSessionMessages,
  getKnowledgeBaseList
} from '@/api'
import type { ChatSession, ChatMessage, KnowledgeBase } from '@/types'

const models = ref([
  { label: 'GLM-4-Flash', value: 'glm-4-flash' },
  { label: 'GLM-5', value: 'glm-5' }
])

const sessions = ref<ChatSession[]>([])
const currentSession = ref<ChatSession | null>(null)
const messages = ref<ChatMessage[]>([])
const inputMessage = ref('')
const sending = ref(false)
const currentModel = ref('glm-4-flash')
const currentMode = ref('normal')
const currentKnowledgeBase = ref('')
const knowledgeBases = ref<KnowledgeBase[]>([])
const messageListRef = ref<HTMLElement>()

const loadSessions = async () => {
  try {
    const res = await getChatSessionList()
    if (res.code === 200) {
      sessions.value = res.data
    }
  } catch (error) {
    console.error('加载会话列表失败:', error)
  }
}

const loadKnowledgeBases = async () => {
  try {
    const res = await getKnowledgeBaseList()
    if (res.code === 200) {
      knowledgeBases.value = res.data
    }
  } catch (error) {
    console.error('加载知识库列表失败:', error)
  }
}

const createSession = async () => {
  try {
    const res = await createChatSession({
      title: '新对话',
      mode: currentMode.value,
      model: currentModel.value,
      knowledgeBaseId: currentKnowledgeBase.value
    })
    if (res.code === 200) {
      sessions.value.unshift(res.data)
      selectSession(res.data)
      ElMessage.success('创建成功')
    }
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

const selectSession = async (session: ChatSession) => {
  currentSession.value = session
  currentModel.value = session.model
  currentMode.value = session.mode
  currentKnowledgeBase.value = session.knowledgeBaseId || ''
  
  try {
    const res = await getSessionMessages(session.id)
    if (res.code === 200) {
      messages.value = res.data
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载消息列表失败:', error)
  }
}

const deleteSession = async () => {
  if (!currentSession.value) {
    ElMessage.warning('请先选择会话')
    return
  }
  
  try {
    await ElMessageBox.confirm('确定要删除该会话吗？', '提示', { type: 'warning' })
    const res = await deleteChatSession(currentSession.value.id)
    if (res.code === 200) {
      sessions.value = sessions.value.filter(s => s.id !== currentSession.value?.id)
      currentSession.value = null
      messages.value = []
      ElMessage.success('删除成功')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentSession.value) return
  
  const userContent = inputMessage.value.trim()
  sending.value = true
  
  // 乐观插入用户消息
  const tempUserMsg: ChatMessage = {
    id: 'temp-' + Date.now(),
    role: 'user',
    content: userContent,
    tokens: 0,
    createdAt: new Date().toISOString()
  }
  messages.value.push(tempUserMsg)
  inputMessage.value = ''
  scrollToBottom()
  
  try {
    const res = await sendApi({
      sessionId: currentSession.value.id,
      content: userContent
    })
    if (res.code === 200) {
      // 移除临时用户消息，用后端返回的替换
      const idx = messages.value.findIndex(m => m.id === tempUserMsg.id)
      if (idx !== -1) {
        messages.value.splice(idx, 1, res.data.userMessage)
      }
      // 添加AI消息
      messages.value.push(res.data.aiMessage)
      scrollToBottom()
    }
  } catch (error) {
    ElMessage.error('发送失败')
  } finally {
    sending.value = false
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const formatDate = (date: string) => {
  const d = new Date(date)
  const month = d.getMonth() + 1
  const day = d.getDate()
  return `${month}/${day}`
}

const formatTime = (date: string) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const formatDuration = (ms: number) => {
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

onMounted(() => {
  loadSessions()
  loadKnowledgeBases()
})
</script>

<style scoped lang="scss">
.chat-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.chat-sidebar {
  width: 260px;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  
  .sidebar-header {
    padding: 15px;
    border-bottom: 1px solid #e8e8e8;
    
    .el-button {
      width: 100%;
    }
  }
  
  .session-list {
    flex: 1;
    overflow-y: auto;
    
    .session-item {
      padding: 15px;
      border-bottom: 1px solid #f0f0f0;
      cursor: pointer;
      
      &:hover {
        background: #f5f5f5;
      }
      
      &.active {
        background: #e6f7ff;
        border-left: 3px solid #1890ff;
      }
      
      .session-title {
        font-size: 14px;
        color: #333;
        margin-bottom: 5px;
      }
      
      .session-time {
        font-size: 12px;
        color: #999;
      }
    }
  }
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  
  .chat-header {
    padding: 15px;
    border-bottom: 1px solid #e8e8e8;
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .header-left {
      display: flex;
      align-items: center;
    }
  }
  
  .message-list {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    
    .message-item {
      display: flex;
      margin-bottom: 20px;
      
      &.user {
        flex-direction: row-reverse;
        
        .message-wrapper {
          align-items: flex-end;
        }
        
        .message-content {
          background: #1890ff;
          color: #fff;
          border-radius: 8px 0 8px 8px;
        }
        
        .message-meta {
          flex-direction: row-reverse;
        }
      }
      
      &.assistant {
        .message-content {
          background: #f5f5f5;
          color: #333;
          border-radius: 0 8px 8px 8px;
        }
      }
      
      .message-avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: #e8e8e8;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }
      
      .message-wrapper {
        display: flex;
        flex-direction: column;
        max-width: 70%;
        margin: 0 12px;
      }
      
      .message-content {
        padding: 12px 16px;
        line-height: 1.6;
        word-break: break-word;
        white-space: pre-wrap;
        
        &.thinking {
          display: flex;
          align-items: center;
          gap: 4px;
          padding: 12px 20px;
          
          .dot {
            width: 8px;
            height: 8px;
            background: #999;
            border-radius: 50%;
            animation: bounce 1.4s infinite ease-in-out both;
            
            &:nth-child(1) { animation-delay: -0.32s; }
            &:nth-child(2) { animation-delay: -0.16s; }
          }
        }
      }
      
      .message-meta {
        display: flex;
        gap: 12px;
        margin-top: 4px;
        font-size: 12px;
        color: #999;
      }
    }
  }
  
  .chat-input {
    padding: 15px;
    border-top: 1px solid #e8e8e8;
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>

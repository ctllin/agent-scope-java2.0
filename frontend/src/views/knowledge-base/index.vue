<template>
  <div class="knowledge-base">
    <div class="header">
      <h2>知识库管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        创建知识库
      </el-button>
    </div>
    
    <!-- 知识库列表 -->
    <el-row :gutter="20">
      <el-col :span="8" v-for="kb in knowledgeBases" :key="kb.id">
        <el-card shadow="hover" class="kb-card" @dblclick="router.push(`/knowledge-base/${kb.id}`)">
          <template #header>
            <div class="card-header">
              <span>{{ kb.name }}</span>
              <el-dropdown @command="handleCommand($event, kb)">
                <el-icon class="more-icon"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="upload">上传文档</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <div class="card-content">
            <p>{{ kb.description || '暂无描述' }}</p>
            <div class="stats">
              <span>文档数: {{ kb.documentCount }}</span>
              <span>向量数: {{ kb.vectorCount }}</span>
            </div>
            <div class="card-actions">
              <el-button type="primary" size="small" @click="router.push(`/knowledge-base/${kb.id}`)">
                进入
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 创建知识库对话框 -->
    <el-dialog v-model="createDialogVisible" title="创建知识库" width="500px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="createForm.name" placeholder="请输入知识库名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 上传文档对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文档" width="500px">
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".pdf,.docx,.txt"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 PDF、DOCX、TXT 格式文件
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
import type { UploadFile } from 'element-plus'
import {
  getKnowledgeBaseList,
  createKnowledgeBase,
  deleteKnowledgeBase,
  uploadDocument
} from '@/api'
import type { KnowledgeBase } from '@/types'

// 知识库列表
const knowledgeBases = ref<KnowledgeBase[]>([])

// 创建对话框
const createDialogVisible = ref(false)
const createForm = ref({
  name: '',
  description: ''
})

// 上传对话框
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const currentKbId = ref('')
const uploadFile = ref<File | null>(null)

// 加载知识库列表
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

// 显示创建对话框
const showCreateDialog = () => {
  createForm.value = { name: '', description: '' }
  createDialogVisible.value = true
}

// 处理创建
const handleCreate = async () => {
  if (!createForm.value.name) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  
  try {
    const res = await createKnowledgeBase(createForm.value)
    if (res.code === 200) {
      knowledgeBases.value.push(res.data)
      createDialogVisible.value = false
      ElMessage.success('创建成功')
    }
  } catch (error) {
    ElMessage.error('创建失败')
  }
}

// 处理命令
const handleCommand = async (command: string, kb: KnowledgeBase) => {
  if (command === 'upload') {
    currentKbId.value = kb.id
    uploadFile.value = null
    uploadDialogVisible.value = true
  } else if (command === 'delete') {
    try {
      await ElMessageBox.confirm('确定要删除该知识库吗？', '提示', {
        type: 'warning'
      })
      
      const res = await deleteKnowledgeBase(kb.id)
      if (res.code === 200) {
        knowledgeBases.value = knowledgeBases.value.filter(item => item.id !== kb.id)
        ElMessage.success('删除成功')
      }
    } catch (error) {
      if (error !== 'cancel') {
        ElMessage.error('删除失败')
      }
    }
  }
}

// 处理文件选择
const handleFileChange = (file: UploadFile) => {
  if (file.raw) {
    uploadFile.value = file.raw
  }
}

// 处理上传
const handleUpload = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  
  uploading.value = true
  try {
    const res = await uploadDocument(currentKbId.value, uploadFile.value)
    if (res.code === 200) {
      uploadDialogVisible.value = false
      ElMessage.success('上传成功')
      loadKnowledgeBases()
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped lang="scss">
.knowledge-base {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
    }
  }
  
  .kb-card {
    margin-bottom: 20px;
    
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      .more-icon {
        cursor: pointer;
      }
    }
    
    .card-content {
      p {
        color: #666;
        margin-bottom: 15px;
      }
      
      .stats {
        display: flex;
        justify-content: space-between;
        color: #999;
        font-size: 14px;
        margin-bottom: 12px;
      }
      .card-actions {
        display: flex;
        justify-content: flex-end;
      }
    }
  }
  
  .upload-area {
    width: 100%;
  }
}
</style>

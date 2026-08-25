<template>
  <div class="user-management">
    <div class="header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        创建用户
      </el-button>
    </div>
    
    <!-- 搜索栏 -->
    <el-input
      v-model="searchKeyword"
      placeholder="搜索用户名/昵称"
      style="width: 300px; margin-bottom: 20px;"
      @input="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>
    
    <!-- 用户表格 -->
    <el-table :data="users" border stripe>
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色">
        <template #default="{ row }">
          <el-tag v-if="row.root" type="warning">Root</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button v-if="!row.root" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页 -->
    <el-pagination
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50]"
      :total="total"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 20px; justify-content: flex-end;"
      @size-change="loadUsers"
      @current-change="loadUsers"
    />
    
    <!-- 创建/编辑用户对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '创建用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserList, createUser, updateUser, deleteUser } from '@/api'
import type { User } from '@/types'

// 用户列表
const users = ref<User[]>([])
// 总数
const total = ref(0)
// 当前页
const currentPage = ref(1)
// 每页大小
const pageSize = ref(10)
// 搜索关键词
const searchKeyword = ref('')

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: '',
  username: '',
  password: '',
  nickname: '',
  email: '',
  phone: '',
  status: 1
})

// 加载用户列表
const loadUsers = async () => {
  try {
    const res = await getUserList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value
    })
    if (res.code === 200) {
      users.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadUsers()
}

// 显示创建对话框
const showCreateDialog = () => {
  isEdit.value = false
  form.value = {
    id: '',
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    status: 1
  }
  dialogVisible.value = true
}

// 显示编辑对话框
const showEditDialog = (user: User) => {
  isEdit.value = true
  // 兼容后端返回数字(1/0)或字符串("ENABLED"/"DISABLED")
  const statusNum = typeof user.status === 'string'
    ? (user.status === 'ENABLED' ? 1 : 0)
    : Number(user.status) || 0
  form.value = {
    id: user.id,
    username: user.username,
    password: '',
    nickname: user.nickname,
    email: user.email,
    phone: user.phone,
    status: statusNum
  }
  dialogVisible.value = true
}

// 处理提交
const handleSubmit = async () => {
  if (!form.value.username) {
    ElMessage.warning('请输入用户名')
    return
  }
  
  try {
    if (isEdit.value) {
      const res = await updateUser(form.value.id, {
        nickname: form.value.nickname,
        email: form.value.email,
        phone: form.value.phone,
        status: form.value.status
      })
      if (res.code === 200) {
        ElMessage.success('更新成功')
        loadUsers()
      }
    } else {
      if (!form.value.password) {
        ElMessage.warning('请输入密码')
        return
      }
      const res = await createUser({
        username: form.value.username,
        password: form.value.password,
        nickname: form.value.nickname,
        email: form.value.email,
        phone: form.value.phone
      })
      if (res.code === 200) {
        ElMessage.success('创建成功')
        loadUsers()
      }
    }
    dialogVisible.value = false
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  }
}

// 处理删除
const handleDelete = async (user: User) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      type: 'warning'
    })
    
    const res = await deleteUser(user.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadUsers()
    }
  } catch (error: any) {
    // 业务错误已由拦截器提示具体原因，这里只处理取消和未知错误
    if (error !== 'cancel' && !error?.message) {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped lang="scss">
.user-management {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    h2 {
      margin: 0;
    }
  }
}
.action-btns {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  white-space: nowrap;
}
</style>

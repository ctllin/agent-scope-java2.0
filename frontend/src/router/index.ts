import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', requiresAuth: true }
      },
      {
        path: 'chat',
        name: 'Chat',
        component: () => import('@/views/chat/index.vue'),
        meta: { title: 'AI对话', requiresAuth: true }
      },
      {
        path: 'knowledge-base',
        name: 'KnowledgeBase',
        component: () => import('@/views/knowledge-base/index.vue'),
        meta: { title: '知识库', requiresAuth: true }
      },
      {
        path: 'knowledge-base/:id',
        name: 'KnowledgeBaseDetail',
        component: () => import('@/views/knowledge-base/detail.vue'),
        meta: { title: '知识库详情', requiresAuth: true }
      },
      {
        path: 'asr',
        name: 'Asr',
        component: () => import('@/views/asr/index.vue'),
        meta: { title: '语音识别', requiresAuth: true }
      },
      {
        path: 'tts',
        name: 'Tts',
        component: () => import('@/views/tts/index.vue'),
        meta: { title: '语音合成', requiresAuth: true }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresRoot: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  // 检查是否需要登录认证
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router

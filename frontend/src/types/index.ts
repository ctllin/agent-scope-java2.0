// 枚举
export enum DocumentStatus {
  UPLOADED = 'UPLOADED',
  SPLIT = 'SPLIT'
}

export enum OcrStatus {
  NONE = 'NONE',
  PART = 'PART',
  DONE = 'DONE',
  PROCESSING = 'PROCESSING',
  FAILED = 'FAILED'
}

// 通用响应类型
export interface ApiResponse<T = any> {
  requestId: string
  timestamp: number
  code: number
  message: string
  data: T
}

// 用户信息
export interface User {
  id: string
  username: string
  nickname: string
  email: string
  phone: string
  status: number
  root: boolean
  createdAt: string
}

// 登录参数
export interface LoginParams {
  username: string
  password: string
}

// 登录响应
export interface LoginResult {
  token: string
  user: User
}

// 分页结果
export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

// 角色信息
export interface Role {
  id: string
  name: string
  description: string
  code: string
  menuIds: string[]
  buttonIds: string[]
  createdAt: string
}

// 菜单信息
export interface Menu {
  id: string
  parentId: string
  name: string
  path: string
  icon: string
  type: number
  sort: number
  visible: boolean
  permission: string
  children: Menu[]
  createdAt: string
}

// 知识库信息
export interface KnowledgeBase {
  id: string
  name: string
  description: string
  icon: string
  documentCount: number
  vectorCount: number
  creatorId: string
  createdAt: string
}

// 文档信息
export interface Document {
  id: string
  name: string
  type: string
  fileSize: number
  knowledgeBaseId: string
  chunkCount: number
  embeddedCount: number
  status: DocumentStatus
  ocrStatus: OcrStatus
  createdAt: string
  pageContents?: PageContent[]
}

// 页面内容（OCR结果）
export interface PageContent {
  page: number
  text: string
  imageUrl?: string
}

// 对话会话
export interface ChatSession {
  id: string
  title: string
  mode: string
  model: string
  knowledgeBaseId: string
  createdAt: string
}

// 对话消息
export interface ChatMessage {
  id: string
  role: string
  content: string
  tokens: number
  duration?: number
  createdAt: string
}

// 发送消息响应（返回用户消息+AI回复）
export interface SendMessageResponse {
  userMessage: ChatMessage
  aiMessage: ChatMessage
}

// 文档分块
export interface DocumentChunk {
  id: string
  documentId: string
  knowledgeBaseId: string
  content: string
  sequence: number
  splitStrategy: string
  embedded: boolean
  vectorId?: string
  delFlag: string
  createdAt: string
}

// 语音识别记录
export interface AsrRecord {
  id: string
  name: string
  filePath?: string
  wavPath?: string
  fileSize?: number
  text: string
  status: 'UPLOADED' | 'RECOGNIZING' | 'DONE' | 'FAILED'
  errorMessage?: string
  source: 'FILE' | 'REALTIME'
  duration?: number
  createdAt: string
}

// 语音合成记录分段
export interface TtsSegment {
  text: string
  charStart: number
  charEnd: number
  duration?: number
}

// 语音合成记录
export interface TtsRecord {
  id: string
  title: string
  text: string
  mode: 'LINE' | 'PARAGRAPH' | 'ALL'
  status: 'SYNTHESIZING' | 'DONE' | 'FAILED'
  errorMessage?: string
  segments?: TtsSegment[]
  duration?: number
  fileSize?: number
  charCount?: number
  createdAt: string
}

import request from '@/utils/request'
import type { ApiResponse, LoginParams, LoginResult, User, PageResult, KnowledgeBase, Document, ChatSession, ChatMessage, SendMessageResponse, DocumentChunk, PageContent, AsrRecord, TtsRecord } from '@/types'

// 登录
export function login(data: LoginParams): Promise<ApiResponse<LoginResult>> {
  return request.post('/auth/login', data) as any
}

// 快捷登录
export function quickLogin(): Promise<ApiResponse<LoginResult>> {
  return request.post('/auth/quick-login') as any
}

// 获取当前用户信息
export function getCurrentUser(): Promise<ApiResponse<LoginResult>> {
  return request.get('/auth/current') as any
}

// 用户管理
export function getUserList(params: { page: number; size: number; keyword?: string }): Promise<ApiResponse<PageResult<User>>> {
  return request.get('/users', { params }) as any
}

export function getUserById(id: string): Promise<ApiResponse<User>> {
  return request.get(`/users/${id}`) as any
}

export function createUser(data: any): Promise<ApiResponse<User>> {
  return request.post('/users', data) as any
}

export function updateUser(id: string, data: any): Promise<ApiResponse<User>> {
  return request.put(`/users/${id}`, data) as any
}

export function deleteUser(id: string): Promise<ApiResponse<void>> {
  return request.delete(`/users/${id}`) as any
}

export function changePassword(id: string, data: { oldPassword: string; newPassword: string }): Promise<ApiResponse<void>> {
  return request.put(`/users/${id}/password`, data) as any
}

// 知识库管理
export function getKnowledgeBaseList(): Promise<ApiResponse<KnowledgeBase[]>> {
  return request.get('/knowledge-bases') as any
}

export function getKnowledgeBaseById(id: string): Promise<ApiResponse<KnowledgeBase>> {
  return request.get(`/knowledge-bases/${id}`) as any
}

export function createKnowledgeBase(data: any): Promise<ApiResponse<KnowledgeBase>> {
  return request.post('/knowledge-bases', data) as any
}

export function deleteKnowledgeBase(id: string): Promise<ApiResponse<void>> {
  return request.delete(`/knowledge-bases/${id}`) as any
}

export function uploadDocument(knowledgeBaseId: string, file: File): Promise<ApiResponse<string>> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/knowledge-bases/${knowledgeBaseId}/documents`, formData) as any
}

export function getDocumentList(knowledgeBaseId: string): Promise<ApiResponse<Document[]>> {
  return request.get(`/knowledge-bases/${knowledgeBaseId}/documents`) as any
}

export function getDocumentListPage(knowledgeBaseId: string, params: { page?: number; size?: number; name?: string; status?: string }): Promise<ApiResponse<{ records: Document[]; total: number; page: number; size: number }>> {
  return request.get(`/knowledge-bases/${knowledgeBaseId}/documents`, { params }) as any
}

export function deleteDocument(documentId: string): Promise<ApiResponse<void>> {
  return request.delete(`/knowledge-bases/documents/${documentId}`) as any
}

export function deleteDocuments(documentIds: string[]): Promise<ApiResponse<void>> {
  return request.delete('/knowledge-bases/documents/batch', { data: documentIds }) as any
}

export function batchSplitDocuments(documentIds: string[]): Promise<ApiResponse<void>> {
  return request.post('/knowledge-bases/documents/batch-split', documentIds) as any
}

export function batchEmbedDocuments(documentIds: string[]): Promise<ApiResponse<void>> {
  return request.post('/knowledge-bases/documents/batch-embed', documentIds) as any
}

export function downloadDocument(documentId: string): Promise<Blob> {
  return request.get(`/knowledge-bases/documents/${documentId}/download`, {
    responseType: 'blob'
  }) as any
}

export function searchKnowledgeBase(knowledgeBaseId: string, data: { query: string; topK?: number }): Promise<ApiResponse<any[]>> {
  return request.post(`/knowledge-bases/${knowledgeBaseId}/search`, data) as any
}

// 对话管理
export function createChatSession(data: any): Promise<ApiResponse<ChatSession>> {
  return request.post('/chat/sessions', data) as any
}

export function getChatSessionList(): Promise<ApiResponse<ChatSession[]>> {
  return request.get('/chat/sessions') as any
}

export function getChatSession(id: string): Promise<ApiResponse<ChatSession>> {
  return request.get(`/chat/sessions/${id}`) as any
}

export function deleteChatSession(id: string): Promise<ApiResponse<void>> {
  return request.delete(`/chat/sessions/${id}`) as any
}

export function sendMessage(data: { sessionId: string; content: string }): Promise<ApiResponse<SendMessageResponse>> {
  return request.post('/chat/messages', data) as any
}

export function getSessionMessages(sessionId: string): Promise<ApiResponse<ChatMessage[]>> {
  return request.get(`/chat/sessions/${sessionId}/messages`) as any
}

// 分块管理
export function splitDocument(documentId: string, data?: { strategy?: string; chunkSize?: number; overlapRatio?: number }): Promise<ApiResponse<DocumentChunk[]>> {
  return request.post(`/knowledge-bases/documents/${documentId}/split`, data || {}) as any
}

export function getDocumentChunks(documentId: string): Promise<ApiResponse<DocumentChunk[]>> {
  return request.get(`/knowledge-bases/documents/${documentId}/chunks`) as any
}

export function getDocumentContent(documentId: string): Promise<ApiResponse<string>> {
  return request.get(`/knowledge-bases/documents/${documentId}/content`) as any
}

export function embedDocument(documentId: string): Promise<ApiResponse<void>> {
  return request.post(`/knowledge-bases/documents/${documentId}/embed`) as any
}

export function batchEmbedChunks(chunkIds: string[]): Promise<ApiResponse<void>> {
  return request.post('/knowledge-bases/chunks/batch-embed', { chunkIds }) as any
}

export function deleteChunkVectors(chunkIds: string[]): Promise<ApiResponse<void>> {
  return request.post('/knowledge-bases/chunks/delete-vectors', { chunkIds }) as any
}

export function batchDeleteChunks(chunkIds: string[]): Promise<ApiResponse<void>> {
  return request.post('/knowledge-bases/chunks/batch-delete', { chunkIds }) as any
}

export function mergeChunks(chunkIds: string[], documentId: string, knowledgeBaseId: string): Promise<ApiResponse<DocumentChunk>> {
  return request.post('/knowledge-bases/chunks/merge', { chunkIds, documentId, knowledgeBaseId }) as any
}

// 手动分块
export function createManualChunk(data: { documentId: string; content: string; sequence: number }): Promise<ApiResponse<DocumentChunk>> {
  return request.post('/knowledge-bases/chunks/manual', data) as any
}

export function updateChunkContent(chunkId: string, content: string): Promise<ApiResponse<DocumentChunk>> {
  return request.put(`/knowledge-bases/chunks/${chunkId}`, { content }) as any
}

export function reorderChunks(orderList: { id: string; sequence: number }[]): Promise<ApiResponse<void>> {
  return request.put('/knowledge-bases/chunks/reorder', orderList) as any
}

export function saveManualChunks(data: { documentId: string; knowledgeBaseId: string; chunks: { content: string }[] }): Promise<ApiResponse<DocumentChunk[]>> {
  return request.post('/knowledge-bases/chunks/manual-batch', data) as any
}

// OCR
export function ocrDocument(documentId: string): Promise<ApiResponse<void>> {
  return request.post(`/knowledge-bases/documents/${documentId}/ocr`) as any
}

/** 轮询文档OCR状态：NONE/PART/DONE/PROCESSING/FAILED */
export function getOcrStatus(documentId: string): Promise<ApiResponse<string>> {
  return request.get(`/knowledge-bases/documents/${documentId}/ocr-status`) as any
}

export function ocrPage(documentId: string, pageIndex: number): Promise<ApiResponse<PageContent>> {
  return request.post(`/knowledge-bases/documents/${documentId}/ocr/${pageIndex}`) as any
}

export function getDocumentPages(documentId: string): Promise<ApiResponse<PageContent[]>> {
  return request.get(`/knowledge-bases/documents/${documentId}/pages`) as any
}

export function getDocumentPageCount(documentId: string): Promise<ApiResponse<number>> {
  return request.get(`/knowledge-bases/documents/${documentId}/page-count`) as any
}

// ==================== 语音识别 ====================

export function uploadAudios(files: File[]): Promise<ApiResponse<AsrRecord[]>> {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  return request.post('/asr/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }) as any
}

export function getAsrRecords(
  page = 1,
  size = 20,
  source?: string,
  status?: string,
  keyword?: string
): Promise<ApiResponse<{ records: AsrRecord[]; total: number }>> {
  return request.get('/asr/records', { params: { page, size, source, status, keyword } }) as any
}

export function recognizeAsrRecord(id: string, lang = 'cn'): Promise<ApiResponse<AsrRecord>> {
  return request.post(`/asr/records/${id}/recognize`, null, { params: { lang } }) as any
}

export function recognizeAsrBatch(ids: string[], lang = 'cn'): Promise<ApiResponse<void>> {
  return request.post('/asr/records/batch-recognize', ids, { params: { lang } }) as any
}

export function deleteAsrRecord(id: string): Promise<ApiResponse<void>> {
  return request.delete(`/asr/records/${id}`) as any
}

export function deleteAsrRecords(ids: string[]): Promise<ApiResponse<void>> {
  return request.post('/asr/records/batch-delete', ids) as any
}

// ==================== 语音合成记录 ====================

export function createTtsRecord(data: { title?: string; text: string; mode: 'LINE' | 'PARAGRAPH' | 'ALL' }): Promise<ApiResponse<TtsRecord>> {
  return request.post('/tts-records', data) as any
}

export function getTtsRecords(
  page = 1,
  size = 20,
  status?: string,
  keyword?: string
): Promise<ApiResponse<{ records: TtsRecord[]; total: number }>> {
  return request.get('/tts-records', { params: { page, size, status, keyword } }) as any
}

export function getTtsRecordDetail(id: string): Promise<ApiResponse<TtsRecord>> {
  return request.get(`/tts-records/${id}`) as any
}

export function deleteTtsRecord(id: string): Promise<ApiResponse<void>> {
  return request.delete(`/tts-records/${id}`) as any
}

export function deleteTtsRecords(ids: string[]): Promise<ApiResponse<void>> {
  return request.post('/tts-records/batch-delete', ids) as any
}

/** 合成音频播放地址（完整拼接音频） */
export function ttsAudioUrl(id: string): string {
  return `/api/tts-records/${id}/audio`
}

<template>
  <div class="knowledge-base-detail">
    <div class="detail-header">
      <div class="header-left">
        <el-button text @click="router.push('/knowledge-base')">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <h2>{{ knowledgeBase?.name || '知识库详情' }}</h2>
      </div>
      <div class="header-right">
        <el-tag v-if="knowledgeBase" type="info">
          {{ knowledgeBase.documentCount || 0 }} 个文档 · {{ knowledgeBase.vectorCount || 0 }} 个向量
        </el-tag>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="detail-tabs">
      <!-- 文档管理 -->
      <el-tab-pane label="文档管理" name="documents">
        <div class="tab-toolbar">
          <input
            ref="fileInputRef"
            type="file"
            multiple
            accept=".pdf,.docx,.txt,.md"
            style="display: none"
            @change="handleFilesChange"
          />
          <el-button type="primary" @click="fileInputRef?.click()">
            <el-icon><Upload /></el-icon>
            上传文档
          </el-button>
          <el-input v-model="docSearchName" placeholder="搜索文件名" clearable style="width: 200px;" @keyup.enter="loadDocumentsPage" @clear="loadDocumentsPage" />
          <el-select v-model="docSearchStatus" placeholder="状态筛选" clearable style="width: 130px;" @change="loadDocumentsPage">
            <el-option label="已上传" :value="DocumentStatus.UPLOADED" />
            <el-option label="已分块" :value="DocumentStatus.SPLIT" />
          </el-select>
          <el-select v-model="docSearchOcr" placeholder="OCR筛选" clearable style="width: 130px;" @change="loadDocumentsPage">
            <el-option label="已OCR" :value="OcrStatus.DONE" />
            <el-option label="部分OCR" :value="OcrStatus.PART" />
            <el-option label="未OCR" :value="OcrStatus.NONE" />
          </el-select>
          <el-button type="warning" :disabled="selectedDocIds.length === 0 || ocrBatchLoading" :loading="ocrBatchLoading" @click="handleBatchOcr">
            <el-icon><EditPen /></el-icon>批量OCR
          </el-button>
          <el-button type="primary" :disabled="selectedDocIds.length === 0 || splitBatchLoading" :loading="splitBatchLoading" @click="handleBatchSplit">
            <el-icon><Scissor /></el-icon>批量分块
          </el-button>
          <el-button type="success" :disabled="selectedDocIds.length === 0 || embedBatchLoading" :loading="embedBatchLoading" @click="handleBatchEmbedDocs">
            <el-icon><MagicStick /></el-icon>批量向量化
          </el-button>
          <el-button type="danger" :disabled="selectedDocIds.length === 0 || deleteBatchLoading" :loading="deleteBatchLoading" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>批量删除
          </el-button>
          <span class="tip">支持 PDF、DOCX、TXT、MD 格式，可多选</span>
        </div>

        <el-table :data="documents" style="width: 100%" v-loading="loadingDocuments" @selection-change="handleDocSelectionChange">
          <el-table-column type="selection" width="40" :selectable="(row: Document) => row.type === 'pdf'" />
          <el-table-column label="文件名" min-width="200">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleReadAloud(row)">{{ row.name }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="80">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="大小" width="100">
            <template #default="{ row }">
              {{ formatFileSize(row.fileSize) }}
            </template>
          </el-table-column>
          <el-table-column label="分块" width="80">
            <template #default="{ row }">
              <span>{{ row.chunkCount || 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="已向量化" width="80">
            <template #default="{ row }">
              <span :class="{ 'text-success': row.embeddedCount > 0, 'text-muted': !row.embeddedCount }">
                {{ row.embeddedCount || 0 }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="上传时间" width="150">
            <template #default="{ row }">
              <span class="text-muted">{{ formatCreatedAt(row.createdAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === DocumentStatus.UPLOADED" type="info" size="small">已上传</el-tag>
              <el-tag v-else-if="row.status === DocumentStatus.SPLIT" type="warning" size="small">已分块</el-tag>
              <el-tag v-else type="info" size="small">未知</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="OCR" width="90">
            <template #default="{ row }">
              <el-tag v-if="row.ocrStatus === OcrStatus.DONE" type="success" size="small">已OCR</el-tag>
              <el-tag v-else-if="row.ocrStatus === 'PROCESSING'" type="warning" size="small">识别中</el-tag>
              <el-tag v-else-if="row.ocrStatus === 'FAILED'" type="danger" size="small">失败</el-tag>
              <el-tag v-else-if="row.ocrStatus === OcrStatus.PART" type="warning" size="small">部分OCR</el-tag>
              <el-tag v-else type="info" size="small">未OCR</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="500" fixed="right">
            <template #default="{ row }">
              <div class="action-btns">
                <el-button text type="primary" size="small" @click="handleSplit(row)">
                  <el-icon><Scissor /></el-icon>分块
                </el-button>
                <el-button text type="primary" size="small" @click="handleViewChunks(row)">
                  <el-icon><View /></el-icon>查看分块
                </el-button>
                <el-button text type="primary" size="small" @click="handleRead(row)">
                  <el-icon><Document /></el-icon>阅读
                </el-button>
                <el-button text type="primary" size="small" @click="handleReadAloud(row)">
                  <el-icon><Headset /></el-icon>朗读
                </el-button>
                <el-button text type="primary" size="small" @click="handleDownload(row)">
                  <el-icon><Download /></el-icon>下载
                </el-button>
                <el-button text type="danger" size="small" @click="handleDelete(row)">
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="docPage"
            v-model:page-size="docPageSize"
            :page-sizes="[10, 20, 50, 100, 200]"
            :total="docTotal"
            background
            layout="total, sizes, prev, pager, next"
            @size-change="loadDocumentsPage"
            @current-change="loadDocumentsPage"
          />
        </div>
      </el-tab-pane>

      <!-- 分块管理 -->
      <el-tab-pane label="分块管理" name="chunks" :disabled="!selectedDocument">
        <template #label>
          <span>分块管理{{ selectedDocument ? ` - ${selectedDocument.name}` : '' }}</span>
        </template>
        
        <div v-if="!selectedDocument" class="empty-tip">
          <el-empty description="请在文档管理中点击「查看分块」" />
        </div>

        <template v-else>
          <div class="tab-toolbar">
            <el-button type="primary" size="small" @click="handleBatchEmbed" :disabled="!selectedUnembeddedIds.length">
              <el-icon><MagicStick /></el-icon>
              批量向量化 ({{ selectedUnembeddedIds.length }})
            </el-button>
            <el-button type="warning" size="small" @click="handleDeleteVectors" :disabled="!selectedEmbeddedIds.length">
              <el-icon><Delete /></el-icon>
              删除向量 ({{ selectedEmbeddedIds.length }})
            </el-button>
            <el-button type="danger" size="small" @click="handleBatchDeleteChunks" :disabled="!selectedChunkIds.length">
              <el-icon><Delete /></el-icon>
              删除分块 ({{ selectedChunkIds.length }})
            </el-button>
            <el-button size="small" @click="handleMergeChunks" :disabled="selectedChunkIds.length < 2">
              <el-icon><Sort /></el-icon>
              合并分块 ({{ selectedChunkIds.length }})
            </el-button>
            <el-button text @click="handleBackToDocuments">
              <el-icon><Back /></el-icon>返回文档列表
            </el-button>
          </div>

          <el-table
            ref="chunkTableRef"
            :data="chunks"
            row-key="id"
            style="width: 100%"
            v-loading="loadingChunks"
            @selection-change="handleChunkSelectionChange"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column prop="sequence" label="序号" width="60" />
            <el-table-column label="内容" min-width="350">
              <template #default="{ row }">
                <div class="chunk-content">{{ row.content }}</div>
              </template>
            </el-table-column>
            <el-table-column label="分块策略" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.splitStrategy === 'merged' ? 'warning' : 'info'">{{ row.splitStrategy }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="向量化" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.embedded" type="success" size="small">已向量化</el-tag>
                <el-tag v-else type="info" size="small">未向量化</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button v-if="!row.embedded" text type="primary" size="small" @click="handleEmbedChunk(row)">
                    <el-icon><MagicStick /></el-icon>向量化
                  </el-button>
                  <el-button v-if="row.embedded" text type="warning" size="small" @click="handleDeleteChunkVector(row)">
                    <el-icon><Delete /></el-icon>删向量
                  </el-button>
                  <el-button text type="danger" size="small" @click="handleDeleteSingleChunk(row)">
                    <el-icon><Delete /></el-icon>删除
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </template>
      </el-tab-pane>

      <!-- 朗读 -->
      <el-tab-pane label="朗读" name="tts" :disabled="!ttsReady">
        <template #label>
          <span>朗读{{ ttsDocumentName ? ` - ${ttsDocumentName}` : '' }}</span>
        </template>

        <div v-if="!ttsReady" class="empty-tip">
          <el-empty description="请在文档管理中点击「朗读」" />
        </div>

        <template v-else>
          <TtsToolbar
            :status="tts.status.value"
            :is-playing="tts.isPlaying.value"
            :current-line-index="tts.currentLineIndex.value"
            :total-lines="tts.lines.value.length"
            :playback-rate="tts.playbackRate.value"
            @play="tts.play()"
            @pause="tts.pause()"
            @stop="tts.stop()"
            @prev-line="tts.prevLine()"
            @next-line="tts.nextLine()"
            @set-rate="tts.setRate"
            @clear-cache="tts.clearCache()"
          />

          <div class="tts-toolbar-secondary">
            <el-button size="small" @click="showPdfViewer = !showPdfViewer">
              <el-icon><Monitor /></el-icon>{{ showPdfViewer ? '隐藏PDF' : '显示PDF' }}
            </el-button>
            <el-button v-if="ttsDocumentType === 'pdf'" size="small" type="success" @click="handleOcrAll" :loading="ocrLoading">
              <el-icon><EditPen /></el-icon>OCR全部页面
            </el-button>
            <el-button v-if="ttsDocumentType === 'pdf'" size="small" type="warning" @click="handleOcrCurrentPage" :loading="ocrLoading">
              <el-icon><Edit /></el-icon>OCR当前页
            </el-button>
            <span v-if="ttsPageContents.length > 0" class="page-info">
              第 {{ ttsCurrentPage }} / {{ ttsTotalPages }} 页
            </span>
          </div>

          <div class="tts-body" :class="{ 'with-pdf': showPdfViewer }">
            <div v-if="showPdfViewer && ttsDocUrl" class="tts-pdf-panel">
              <PdfViewer :url="ttsDocUrl" :page-number="ttsCurrentPage" />
            </div>
            <div class="tts-text-panel">
              <TextViewer
                ref="textViewerRef"
                :lines="tts.lines.value"
                :current-line-index="tts.currentLineIndex.value"
                :current-char-index="tts.currentCharIndex.value"
                :page-mode="ttsDocumentType === 'pdf' || ttsDocumentType === 'docx'"
                :page-contents="ttsPageContents"
                @line-click="tts.jumpToLine"
                @edit-line="handleTtsEditLine"
                @page-change="handleTtsPageChange"
                @move-lines="handleTtsMoveLines"
                @delete-lines="handleTtsDeleteLines"
                @merge-lines="handleTtsMergeLines"
                @merge-lines-no-space="handleTtsMergeLinesNoSpace"
                @page-edit-save="handleTtsPageEditSave"
              />
            </div>
          </div>
        </template>
      </el-tab-pane>

      <!-- 知识库问答 -->
      <el-tab-pane label="知识库问答" name="qa">
        <div class="qa-container">
          <div class="qa-messages" ref="messagesRef">
            <div v-if="qaMessages.length === 0" class="qa-empty">
              <el-icon :size="48"><ChatDotRound /></el-icon>
              <p>基于「{{ knowledgeBase?.name }}」知识库进行问答</p>
              <p class="sub">输入问题，AI将从知识库中检索相关信息并回答</p>
            </div>
            <div v-for="(msg, idx) in qaMessages" :key="idx" :class="['qa-message', msg.role]">
              <div class="message-avatar">
                <el-avatar v-if="msg.role === 'user'" :size="32">我</el-avatar>
                <el-avatar v-else :size="32" style="background: #1890ff">AI</el-avatar>
              </div>
              <div class="message-content">
                <div class="message-text">{{ msg.content }}</div>
                <div v-if="msg.duration" class="message-time">耗时: {{ formatDuration(msg.duration) }}</div>
                <div v-if="msg.sources && msg.sources.length > 0" class="message-sources">
                  <div class="sources-title">{{ qaMode === 'pure' ? '匹配结果：' : '参考来源：' }}</div>
                  <div v-for="(src, i) in msg.sources" :key="i" class="source-item">
                    <div class="source-header">
                      <span class="source-name">{{ src.documentName }}</span>
                      <span class="source-score">{{ (src.score * 100).toFixed(1) }}%</span>
                    </div>
                    <div v-if="qaMode === 'pure'" class="source-content">{{ src.content }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="qaSending" class="qa-message assistant">
              <div class="message-avatar">
                <el-avatar :size="32" style="background: #1890ff">AI</el-avatar>
              </div>
              <div class="message-content">
                <div class="typing-indicator"><span></span><span></span><span></span></div>
              </div>
            </div>
          </div>
          <div class="qa-input">
            <div class="qa-mode-bar">
              <el-radio-group v-model="qaMode" size="small">
                <el-radio-button value="ai">AI汇总</el-radio-button>
                <el-radio-button value="pure">纯向量</el-radio-button>
              </el-radio-group>
              <span v-if="qaMode === 'pure'" class="qa-mode-tip">返回前3条匹配结果</span>
            </div>
            <el-input
              v-model="qaInput"
              type="textarea"
              :rows="2"
              :placeholder="qaMode === 'ai' ? '输入问题，AI汇总知识库答案...（Ctrl+Enter 发送）' : '输入问题，查看向量匹配结果...（Ctrl+Enter 发送）'"
              :disabled="qaSending"
              @keydown.enter.ctrl="handleSendQuestion"
            />
            <el-button type="primary" :loading="qaSending" :disabled="!qaInput.trim()" @click="handleSendQuestion">
              发送
            </el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 分块对话框 -->
    <el-dialog v-model="splitDialogVisible" title="文档分块" width="450px">
      <el-form label-width="100px">
        <el-form-item label="分块策略">
          <el-select v-model="splitForm.strategy" style="width: 100%;">
            <el-option label="自动（推荐）" value="AUTO" />
            <el-option label="按段落分割" value="PARAGRAPH" />
            <el-option label="按字符数分割" value="CHARACTERS" />
            <el-option label="手动分块（自定义分隔符）" value="MANUAL" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="splitForm.strategy === 'MANUAL'" label="分隔符">
          <el-input v-model="splitForm.delimiter" placeholder="如：\\n\\n 或 === 或 ---" />
          <div class="form-tip">支持正则表达式，常用：\n（换行）、\n\n（段落）、===（自定义标记）</div>
        </el-form-item>
        <el-form-item v-if="splitForm.strategy !== 'MANUAL'" label="分块大小">
          <el-input-number v-model="splitForm.chunkSize" :min="100" :max="5000" :step="100" style="width: 100%;" />
          <div class="form-tip">每个分块的最大字符数，默认 500</div>
        </el-form-item>
        <el-form-item v-if="splitForm.strategy !== 'MANUAL'" label="重叠比例">
          <el-slider v-model="splitForm.overlapRatio" :min="0" :max="0.5" :step="0.05" show-input />
          <div class="form-tip">相邻分块的重叠字符比例，默认 0.15</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="splitDialogVisible = false">取消</el-button>
        <el-button @click="startManualSplit">手动分块</el-button>
        <el-button type="primary" :loading="splitting" @click="confirmSplit">确定分块</el-button>
      </template>
    </el-dialog>

    <!-- 手动分块对话框 -->
    <ManualSplitDialog
      v-model="showManualSplit"
      :document-id="manualSplitDocId"
      :knowledge-base-id="manualSplitKbId"
      @saved="onManualSplitSaved"
    />

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getKnowledgeBaseById,
  getDocumentListPage,
  getDocumentContent,
  getDocumentPages,
  getOcrStatus,
  ocrDocument,
  ocrPage as ocrPageApi,
  getDocumentPageCount,
  uploadDocument,
  deleteDocument,
  deleteDocuments,
  batchSplitDocuments,
  batchEmbedDocuments,
  searchKnowledgeBase,
  createChatSession,
  sendMessage,
  deleteChatSession,
  splitDocument,
  getDocumentChunks,
  batchEmbedChunks,
  deleteChunkVectors,
  batchDeleteChunks,
  mergeChunks
} from '@/api'
import type { KnowledgeBase, Document, DocumentChunk, PageContent } from '@/types'
import { DocumentStatus, OcrStatus } from '@/types'
import ManualSplitDialog from '@/components/ManualSplitDialog.vue'
import TtsToolbar from '@/components/TtsToolbar.vue'
import TextViewer from '@/components/TextViewer.vue'
import PdfViewer from '@/components/PdfViewer.vue'
import { useTTS } from '@/composables/useTTS'

const route = useRoute()
const router = useRouter()
const kbId = route.params.id as string

const knowledgeBase = ref<KnowledgeBase | null>(null)
const documents = ref<Document[]>([])
const loadingDocuments = ref(false)
const activeTab = ref('documents')

// 分块管理
const selectedDocument = ref<Document | null>(null)
const chunks = ref<DocumentChunk[]>([])
const loadingChunks = ref(false)
const selectedChunkIds = ref<string[]>([])
const selectedEmbeddedIds = ref<string[]>([])
const selectedUnembeddedIds = ref<string[]>([])
const chunkTableRef = ref()
// 分块对话框
const splitDialogVisible = ref(false)
const splitting = ref(false)
const splitForm = ref({ strategy: 'AUTO', chunkSize: 500, overlapRatio: 0.15, delimiter: '\\n\\n' })
let splitTargetDoc: Document | null = null

// QA
const qaMessages = ref<Array<{ role: string; content: string; sources?: any[]; duration?: number }>>([])
const qaInput = ref('')
const qaSending = ref(false)
const qaMode = ref<'ai' | 'pure'>('ai')
const messagesRef = ref<HTMLElement>()
const fileInputRef = ref<HTMLInputElement>()

// 手动分块
const showManualSplit = ref(false)
const manualSplitDocId = ref('')
const manualSplitKbId = ref('')

// 朗读
const tts = useTTS()
const ttsReady = ref(false)
const ttsDocumentName = ref('')
const ttsDocumentId = ref('')
const ttsDocumentType = ref('')
const ttsDocUrl = ref('')
const ttsPageContents = ref<PageContent[]>([])
const ttsTotalPages = ref(1)
const showPdfViewer = ref(true)
const ocrLoading = ref(false)
const textViewerRef = ref()

const ttsCurrentPage = computed(() => {
  if (ttsPageContents.value.length === 0) return 1
  const lineIdx = tts.currentLineIndex.value
  let offset = 0
  for (const pc of ttsPageContents.value) {
    const pageCount = Math.max((pc.text || '').split('\n').filter((l: string) => l.trim()).length, 1)
    if (lineIdx < offset + pageCount) return pc.page
    offset += pageCount
  }
  return ttsPageContents.value[ttsPageContents.value.length - 1]?.page || 1
})

// 文档预览
function handleRead(doc: Document) {
  window.open(`/api/knowledge-bases/documents/${doc.id}/view`, '_blank')
}

async function handleReadAloud(doc: Document) {
  try {
    ElMessage.info('正在加载文档内容...')
    ttsDocumentName.value = doc.name
    ttsDocumentId.value = doc.id
    ttsDocumentType.value = doc.type
    ttsDocUrl.value = `/api/knowledge-bases/documents/${doc.id}/view`

    // 获取 pageContents
    let pages: PageContent[] = []
    try {
      const pagesRes = await getDocumentPages(doc.id)
      pages = pagesRes.data || []
    } catch {
      pages = []
    }

    // 获取PDF总页数
    if (doc.type === 'pdf') {
      try {
        const countRes = await getDocumentPageCount(doc.id)
        ttsTotalPages.value = countRes.data || 1
      } catch {
        ttsTotalPages.value = 1
      }
    } else {
      ttsTotalPages.value = 1
    }

    // 如果没有 pageContents，尝试从 content 获取（兼容旧数据）
    if (pages.length === 0) {
      try {
        const contentRes = await getDocumentContent(doc.id)
        const content = contentRes.data || ''
        if (content.trim()) {
          const lines = content.split('\n').filter((l: string) => l.trim())
          if (lines.length > 0) {
            pages = [{ page: 1, text: content }]
          }
        }
      } catch {
        // ignore
      }
    }

    if (pages.length === 0) {
      ElMessage.warning('文档内容为空，无法朗读')
      return
    }

    // 按页构建 lines，每页至少一个空行
    const lines: string[] = []
    for (const pc of pages) {
      if (pc.text && pc.text.trim()) {
        const pageLines = pc.text.split('\n').filter((l: string) => l.trim())
        lines.push(...(pageLines.length > 0 ? pageLines : ['']))
      } else {
        lines.push('')
      }
    }

    ttsPageContents.value = pages
    tts.setLines(lines)
    syncPageLineCounts()

    ttsReady.value = true
    activeTab.value = 'tts'
  } catch (e) {
    ElMessage.error('加载文档内容失败')
  }
}

async function handleOcrAll() {
  if (!ttsDocumentId.value) return
  ocrLoading.value = true
  try {
    // 提交OCR事件（异步），轮询状态直到完成
    await ocrDocument(ttsDocumentId.value)
    const done = await pollOcrStatus(ttsDocumentId.value)
    if (!done) return

    const res = await getDocumentPages(ttsDocumentId.value)
    const ocrResults: PageContent[] = res.data || []

    // 补齐空页，确保 pageContents 与 PDF 总页数一致
    const allPages: PageContent[] = []
    for (let i = 1; i <= ttsTotalPages.value; i++) {
      const found = ocrResults.find(p => p.page === i)
      allPages.push(found || { page: i, text: '' })
    }
    ttsPageContents.value = allPages

    // 按页序构建 lines，每页至少占一行
    const lines: string[] = []
    for (const pc of allPages) {
      if (pc.text && !pc.text.startsWith('[OCR')) {
        const pageLines = pc.text.split('\n').filter((l: string) => l.trim())
        lines.push(...(pageLines.length > 0 ? pageLines : ['']))
      } else {
        lines.push('')
      }
    }
    if (lines.length > 0) {
      tts.stop()
      tts.setLines(lines)
      syncPageLineCounts()
      nextTick(() => textViewerRef.value?.goPage(1))
      ElMessage.success(`OCR完成，共 ${ttsTotalPages.value} 页`)
    } else {
      ElMessage.warning('OCR未识别到文字，请检查 rapidocr-onnxruntime 依赖是否已添加')
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || 'OCR失败'
    ElMessage.error(msg)
  } finally {
    ocrLoading.value = false
  }
}

/** 轮询OCR状态：DONE返回true；FAILED/超时提示并返回false */
async function pollOcrStatus(documentId: string, maxSeconds = 300): Promise<boolean> {
  for (let i = 0; i < maxSeconds / 2; i++) {
    await new Promise(r => setTimeout(r, 2000))
    try {
      const res = await getOcrStatus(documentId)
      const status = res.data
      if (status === 'DONE') return true
      if (status === 'FAILED') {
        ElMessage.error('OCR失败，请重试')
        return false
      }
    } catch { /* 忽略单次轮询错误 */ }
  }
  ElMessage.warning('OCR超时，请稍后刷新查看')
  return false
}

async function handleOcrCurrentPage() {
  if (!ttsDocumentId.value) return
  ocrLoading.value = true
  try {
    const res = await ocrPageApi(ttsDocumentId.value, ttsCurrentPage.value)
    const pageContent = res.data
    if (pageContent) {
      // 确保 pageContents 有所有页的条目
      if (ttsPageContents.value.length === 0) {
        for (let i = 1; i <= ttsTotalPages.value; i++) {
          ttsPageContents.value.push({ page: i, text: '' })
        }
      }
      const idx = ttsPageContents.value.findIndex(p => p.page === pageContent.page)
      if (idx >= 0) ttsPageContents.value[idx] = pageContent
      else ttsPageContents.value.push(pageContent)
      ttsPageContents.value.sort((a, b) => a.page - b.page)

      // 重建 lines
      const lines: string[] = []
      for (const pc of ttsPageContents.value) {
        if (pc.text && !pc.text.startsWith('[OCR')) {
          const pageLines = pc.text.split('\n').filter((l: string) => l.trim())
          lines.push(...(pageLines.length > 0 ? pageLines : ['']))
        } else {
          lines.push('')
        }
      }
      if (lines.length > 0) {
        tts.stop()
        tts.setLines(lines)
        syncPageLineCounts()
        nextTick(() => textViewerRef.value?.goPage(ttsCurrentPage.value))
      }

      if (pageContent.text && !pageContent.text.startsWith('[OCR')) {
        ElMessage.success(`第 ${ttsCurrentPage.value} 页 OCR 完成`)
      } else {
        ElMessage.warning(`第 ${ttsCurrentPage.value} 页 OCR 未识别到文字`)
      }
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || 'OCR失败'
    ElMessage.error(msg)
  } finally {
    ocrLoading.value = false
  }
}

// 记录每页原始行数
const ttsPageLineCounts = ref<number[]>([])

function syncPageLineCounts() {
  ttsPageLineCounts.value = ttsPageContents.value.map(pc =>
    Math.max((pc.text || '').split('\n').filter((l: string) => l.trim()).length, 1)
  )
}

// 根据多个行索引找到涉及的页并更新
function updatePagesForLines(indices: number[]) {
  const counts = ttsPageLineCounts.value
  if (counts.length === 0) return
  const affectedPages = new Set<number>()
  let offset = 0
  for (let i = 0; i < counts.length; i++) {
    const pageEnd = offset + counts[i]
    for (const idx of indices) {
      if (idx >= offset && idx < pageEnd) affectedPages.add(i)
    }
    offset = pageEnd
  }
  // 从 lines 重建涉及的页
  offset = 0
  for (let i = 0; i < counts.length; i++) {
    if (affectedPages.has(i)) {
      const pageLines = tts.lines.value.slice(offset, offset + counts[i])
      ttsPageContents.value[i] = { ...ttsPageContents.value[i], text: pageLines.join('\n') }
    }
    offset += counts[i]
  }
}

// 用当前 counts 从 lines 重建所有页文本（页边界不变）
function refreshPageContents() {
  const counts = ttsPageLineCounts.value
  const allLines = tts.lines.value
  if (counts.length === 0) return
  let offset = 0
  for (let i = 0; i < ttsPageContents.value.length; i++) {
    const n = counts[i] ?? 1
    const pageLines = allLines.slice(offset, offset + n)
    ttsPageContents.value[i] = { ...ttsPageContents.value[i], text: pageLines.join('\n') }
    offset += n
  }
}

// 计算每个索引落在哪一页，返回 {页下标 -> 命中数量}
function countIndicesPerPage(indices: number[]): Map<number, number> {
  const counts = ttsPageLineCounts.value
  const result = new Map<number, number>()
  let offset = 0
  for (let p = 0; p < counts.length; p++) {
    let hit = 0
    for (const idx of indices) {
      if (idx >= offset && idx < offset + counts[p]) hit++
    }
    if (hit > 0) result.set(p, hit)
    offset += counts[p]
  }
  return result
}

function handleTtsEditLine(index: number, text: string) {
  const lines = [...tts.lines.value]
  lines[index] = text
  tts.updateLines(lines, true)
  refreshPageContents()
  nextTick(() => tts.jumpToLine(index))
}

function handleTtsPageChange(page: number) {
  if (ttsPageContents.value.length === 0) return
  let lineOffset = 0
  for (const pc of ttsPageContents.value) {
    if (pc.page === page) break
    lineOffset += Math.max((pc.text || '').split('\n').filter((l: string) => l.trim()).length, 1)
  }
  tts.jumpToLine(lineOffset)
}

function handleTtsMoveLines(fromIdx: number, count: number, toIdx: number) {
  const lines = [...tts.lines.value]
  const moving = lines.splice(fromIdx, count)
  const insertAt = toIdx > fromIdx ? toIdx - count + 1 : toIdx
  lines.splice(insertAt, 0, ...moving)
  tts.updateLines(lines, true)
  // 行数不变，直接更新涉及的页
  const affected = []
  for (let i = 0; i < count; i++) affected.push(Math.min(fromIdx + i, lines.length - 1))
  for (let i = 0; i < count; i++) affected.push(Math.min(insertAt + i, lines.length - 1))
  updatePagesForLines([...new Set(affected)])
  nextTick(() => tts.jumpToLine(insertAt))
}

function handleTtsDeleteLines(indices: number[]) {
  const lines = [...tts.lines.value]
  const toDelete = new Set(indices)
  const newLines = lines.filter((_, i) => !toDelete.has(i))
  if (newLines.length === 0) {
    ElMessage.warning('不能删除所有行')
    return
  }
  // 只扣减被删行所在页的计数，其他页边界不动
  const counts = [...ttsPageLineCounts.value]
  for (const [p, hit] of countIndicesPerPage(indices)) {
    counts[p] = Math.max(counts[p] - hit, 1)
  }
  ttsPageLineCounts.value = counts
  tts.updateLines(newLines, true)
  refreshPageContents()
}

function mergeLinesInternal(indices: number[], sep: string) {
  const sorted = [...indices].sort((a, b) => a - b)
  const lines = [...tts.lines.value]
  const parts = sorted.map(i => lines[i]).filter(Boolean)
  if (parts.length === 0) return
  const merged = parts.join(sep)
  // 记录合并前行所在的页
  const perPageHits = countIndicesPerPage(sorted)
  for (let i = sorted.length - 1; i >= 0; i--) {
    lines.splice(sorted[i], 1)
  }
  lines.splice(sorted[0], 0, merged)
  // 扣减各页被合并的行数，再把合并后的新行加回首行所在页
  const counts = [...ttsPageLineCounts.value]
  for (const [p, hit] of perPageHits) {
    counts[p] -= hit
  }
  let offset = 0
  for (let p = 0; p < counts.length; p++) {
    if (sorted[0] < offset + counts[p] + (perPageHits.get(p) ?? 0)) {
      counts[p] += 1
      break
    }
    offset += counts[p]
  }
  ttsPageLineCounts.value = counts
  tts.updateLines(lines, true)
  refreshPageContents()
  nextTick(() => tts.jumpToLine(sorted[0]))
}

function handleTtsMergeLines(indices: number[]) {
  mergeLinesInternal(indices, ' ')
}

function handleTtsMergeLinesNoSpace(indices: number[]) {
  mergeLinesInternal(indices, '')
}

function handleTtsPageEditSave(pageText: string) {
  const newPageLines = pageText.split('\n').filter((l: string) => l.trim())
  if (newPageLines.length === 0) newPageLines.push('')
  const allLines = [...tts.lines.value]
  const counts = [...ttsPageLineCounts.value]
  if (counts.length > 0) {
    let start = 0
    const pageIdx = ttsPageContents.value.findIndex(p => p.page === ttsCurrentPage.value)
    for (let i = 0; i < pageIdx; i++) {
      start += counts[i] || 1
    }
    const oldPageCount = counts[pageIdx] || 1
    allLines.splice(start, oldPageCount, ...newPageLines)
    // 只更新该页行数，其他页边界不动
    counts[pageIdx] = newPageLines.length
    ttsPageLineCounts.value = counts
  }
  tts.updateLines(allLines, true)
  refreshPageContents()
}

// 文档分页
const docSearchName = ref('')
const docSearchStatus = ref('')
const docSearchOcr = ref('')
const docPage = ref(1)
const docPageSize = ref(20)
const docTotal = ref(0)
const selectedDocIds = ref<string[]>([])
const ocrBatchLoading = ref(false)
const splitBatchLoading = ref(false)
const embedBatchLoading = ref(false)
const deleteBatchLoading = ref(false)

onMounted(async () => {
  await loadKnowledgeBase()
  await loadDocumentsPage()
})

watch(activeTab, (tab) => {
  if (tab === 'documents') {
    loadDocumentsPage()
  }
})

async function loadKnowledgeBase() {
  try {
    const res = await getKnowledgeBaseById(kbId)
    knowledgeBase.value = res.data
  } catch (e) {
    ElMessage.error('加载知识库信息失败')
  }
}

async function loadDocumentsPage() {
  loadingDocuments.value = true
  try {
    const params: any = { page: docPage.value - 1, size: docPageSize.value }
    if (docSearchName.value) params.name = docSearchName.value
    if (docSearchStatus.value) params.status = docSearchStatus.value
    if (docSearchOcr.value) params.ocrStatus = docSearchOcr.value
    const res = await getDocumentListPage(kbId, params)
    documents.value = res.data?.records || []
    docTotal.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载文档列表失败')
  } finally {
    loadingDocuments.value = false
  }
}

function handleDocSelectionChange(rows: Document[]) {
  selectedDocIds.value = rows.map(r => r.id)
}

async function handleBatchOcr() {
  if (selectedDocIds.value.length === 0) return
  ocrBatchLoading.value = true
  try {
    // 全部提交为异步OCR事件
    for (const docId of selectedDocIds.value) {
      try {
        await ocrDocument(docId)
      } catch { /* 单个失败不中断 */ }
    }
    ElMessage.success(`已提交 ${selectedDocIds.value.length} 个OCR任务，处理中...`)
    // 轮询列表直到无PROCESSING
    for (let i = 0; i < 150; i++) {
      await new Promise(r => setTimeout(r, 2000))
      await loadDocumentsPage()
      const hasProcessing = documents.value.some(d => d.ocrStatus === OcrStatus.PROCESSING)
      if (!hasProcessing) break
    }
    await loadDocumentsPage()
  } finally {
    ocrBatchLoading.value = false
  }
}

async function handleBatchDelete() {
  if (selectedDocIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedDocIds.value.length} 个文档吗？此操作将同时删除文件、分块和向量数据。`, '批量删除确认', { type: 'warning' })
  } catch { return }
  deleteBatchLoading.value = true
  try {
    await deleteDocuments(selectedDocIds.value)
    ElMessage.success(`已删除 ${selectedDocIds.value.length} 个文档`)
    selectedDocIds.value = []
    await loadDocumentsPage()
  } catch {
    ElMessage.error('批量删除失败')
  } finally {
    deleteBatchLoading.value = false
  }
}

async function handleBatchSplit() {
  if (selectedDocIds.value.length === 0) return
  splitBatchLoading.value = true
  let successCount = 0
  let failCount = 0
  try {
    for (const docId of selectedDocIds.value) {
      try {
        await batchSplitDocuments([docId])
        successCount++
      } catch {
        failCount++
      }
    }
    ElMessage.success(`批量分块完成：成功 ${successCount}，失败 ${failCount}`)
    await loadDocumentsPage()
  } finally {
    splitBatchLoading.value = false
  }
}

async function handleBatchEmbedDocs() {
  if (selectedDocIds.value.length === 0) return
  embedBatchLoading.value = true
  let successCount = 0
  let failCount = 0
  try {
    for (const docId of selectedDocIds.value) {
      try {
        await batchEmbedDocuments([docId])
        successCount++
      } catch {
        failCount++
      }
    }
    ElMessage.success(`批量向量化完成：成功 ${successCount}，失败 ${failCount}`)
    await loadDocumentsPage()
  } finally {
    embedBatchLoading.value = false
  }
}

async function handleFilesChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  let successCount = 0
  let failCount = 0

  for (let i = 0; i < files.length; i++) {
    const rawFile = files[i]
    const ext = rawFile.name.substring(rawFile.name.lastIndexOf('.')).toLowerCase()
    if (!['.pdf', '.docx', '.txt', '.md'].includes(ext)) {
      ElMessage.warning(`跳过不支持的文件: ${rawFile.name}`)
      failCount++
      continue
    }
    try {
      await uploadDocument(kbId, rawFile)
      successCount++
    } catch (err) {
      failCount++
    }
  }

  // 重置input以允许重复选择相同文件
  input.value = ''

  if (successCount > 0) {
    ElMessage.success(`成功上传 ${successCount} 个文档`)
    await loadDocumentsPage()
    await loadKnowledgeBase()
  }
  if (failCount > 0 && successCount > 0) {
    ElMessage.warning(`${failCount} 个文件上传失败`)
  }
}

function handleDownload(doc: Document) {
  const link = document.createElement('a')
  link.href = `/api/knowledge-bases/documents/${doc.id}/download`
  link.download = doc.name
  link.click()
}

async function handleDelete(doc: Document) {
  try {
    await ElMessageBox.confirm(`确定删除文档「${doc.name}」吗？`, '确认删除', { type: 'warning' })
    await deleteDocument(doc.id)
    ElMessage.success('删除成功')
    if (selectedDocument.value?.id === doc.id) {
      selectedDocument.value = null
      chunks.value = []
    }
    await loadDocumentsPage()
    await loadKnowledgeBase()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

// 分块
function handleSplit(doc: Document) {
  splitTargetDoc = doc
  splitForm.value = { strategy: 'AUTO', chunkSize: 500, overlapRatio: 0.15, delimiter: '\\n\\n' }
  splitDialogVisible.value = true
}

async function confirmSplit() {
  if (!splitTargetDoc) return
  splitting.value = true
  try {
    const params: any = {
      strategy: splitForm.value.strategy,
      chunkSize: splitForm.value.chunkSize,
      overlapRatio: splitForm.value.overlapRatio
    }
    if (splitForm.value.strategy === 'MANUAL') {
      params.delimiter = splitForm.value.delimiter
    }
    await splitDocument(splitTargetDoc.id, params)
    ElMessage.success('分块完成')
    splitDialogVisible.value = false
    await loadDocumentsPage()
    if (selectedDocument.value?.id === splitTargetDoc.id) {
      await loadChunks(splitTargetDoc)
    }
  } catch (e) {
    ElMessage.error('分块失败')
  } finally {
    splitting.value = false
  }
}

// 查看分块
async function handleViewChunks(doc: Document) {
  selectedDocument.value = doc
  activeTab.value = 'chunks'
  await loadChunks(doc)
}

async function loadChunks(doc: Document) {
  loadingChunks.value = true
  try {
    const res = await getDocumentChunks(doc.id)
    chunks.value = res.data || []
  } catch (e) {
    ElMessage.error('加载分块失败')
  } finally {
    loadingChunks.value = false
  }
}

function handleChunkSelectionChange(selection: DocumentChunk[]) {
  selectedChunkIds.value = selection.map(c => c.id)
  selectedEmbeddedIds.value = selection.filter(c => c.embedded).map(c => c.id)
  selectedUnembeddedIds.value = selection.filter(c => !c.embedded).map(c => c.id)
}

async function handleBatchEmbed() {
  if (!selectedUnembeddedIds.value.length) return
  try {
    await batchEmbedChunks(selectedUnembeddedIds.value)
    ElMessage.success('向量化完成')
    await loadChunks(selectedDocument.value!)
    await loadDocumentsPage()
  } catch (e) {
    ElMessage.error('向量化失败')
  }
}

async function handleDeleteVectors() {
  if (!selectedEmbeddedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除 ${selectedEmbeddedIds.value.length} 个分块的向量数据吗？`, '确认删除', { type: 'warning' })
    await deleteChunkVectors(selectedEmbeddedIds.value)
    ElMessage.success('向量删除成功')
    await loadChunks(selectedDocument.value!)
    await loadDocumentsPage()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除向量失败')
  }
}

async function handleBatchDeleteChunks() {
  if (!selectedChunkIds.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除 ${selectedChunkIds.value.length} 个分块吗？`, '确认删除', { type: 'warning' })
    await batchDeleteChunks(selectedChunkIds.value)
    ElMessage.success('分块删除成功')
    selectedChunkIds.value = []
    selectedEmbeddedIds.value = []
    selectedUnembeddedIds.value = []
    await loadChunks(selectedDocument.value!)
    await loadDocumentsPage()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除分块失败')
  }
}

// 单条分块向量化
async function handleEmbedChunk(chunk: DocumentChunk) {
  try {
    await batchEmbedChunks([chunk.id])
    ElMessage.success('向量化完成')
    await loadChunks(selectedDocument.value!)
    await loadDocumentsPage()
  } catch (e) {
    ElMessage.error('向量化失败')
  }
}

// 单条分块删除向量
async function handleDeleteChunkVector(chunk: DocumentChunk) {
  try {
    await ElMessageBox.confirm('确定删除该分块的向量数据吗？', '确认删除', { type: 'warning' })
    await deleteChunkVectors([chunk.id])
    ElMessage.success('向量删除成功')
    await loadChunks(selectedDocument.value!)
    await loadDocumentsPage()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除向量失败')
  }
}

// 单条分块删除
async function handleDeleteSingleChunk(chunk: DocumentChunk) {
  try {
    await ElMessageBox.confirm('确定删除该分块吗？', '确认删除', { type: 'warning' })
    await batchDeleteChunks([chunk.id])
    ElMessage.success('分块删除成功')
    await loadChunks(selectedDocument.value!)
    await loadDocumentsPage()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除分块失败')
  }
}

async function handleMergeChunks() {
  if (selectedChunkIds.value.length < 2 || !selectedDocument.value) return
  try {
    await ElMessageBox.confirm(`确定合并 ${selectedChunkIds.value.length} 个分块吗？`, '确认合并', { type: 'warning' })
    await mergeChunks(selectedChunkIds.value, selectedDocument.value.id, kbId)
    ElMessage.success('分块合并完成')
    await loadChunks(selectedDocument.value!)
    await loadDocumentsPage()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('合并分块失败')
  }
}

function handleBackToDocuments() {
  selectedDocument.value = null
  chunks.value = []
  activeTab.value = 'documents'
}

// 手动分块
function startManualSplit() {
  if (!splitTargetDoc) return
  splitDialogVisible.value = false
  manualSplitDocId.value = splitTargetDoc.id
  manualSplitKbId.value = kbId
  showManualSplit.value = true
}

async function onManualSplitSaved() {
  await loadDocumentsPage()
  if (selectedDocument.value) {
    await loadChunks(selectedDocument.value)
  }
}

// QA
async function handleSendQuestion() {
  const question = qaInput.value.trim()
  if (!question || qaSending.value) return
  qaSending.value = true
  qaInput.value = ''
  qaMessages.value.push({ role: 'user', content: question })

  try {
    const topK = qaMode.value === 'pure' ? 3 : 5
    const searchRes = await searchKnowledgeBase(kbId, { query: question, topK })
    const sources = searchRes.data || []

    if (qaMode.value === 'pure') {
      // 纯向量模式：直接展示匹配结果
      if (sources.length === 0) {
        qaMessages.value.push({ role: 'assistant', content: '未找到相关结果。' })
      } else {
        qaMessages.value.push({
          role: 'assistant',
          content: `共找到 ${sources.length} 条匹配结果：`,
          sources: sources
        })
      }
    } else {
      // AI汇总模式
      let enrichedQuestion = question
      if (sources.length > 0) {
        let context = '以下是从知识库中检索到的相关信息：\n'
        sources.forEach((s: any, i: number) => {
          context += `${i + 1}. ${s.content}（来源：${s.documentName}）\n`
        })
        context += `\n请基于以上信息回答：${question}`
        enrichedQuestion = context
      }

      const sessionRes = await createChatSession({
        title: '知识库问答',
        mode: 'knowledge_base',
        knowledgeBaseId: kbId
      })
      const sessionId = sessionRes.data?.id

      if (sessionId) {
        const msgRes = await sendMessage({ sessionId, content: enrichedQuestion })
        const aiMsg = msgRes.data?.aiMessage
        qaMessages.value.push({
          role: 'assistant',
          content: aiMsg?.content || '抱歉，无法生成回复。',
          sources: sources,
          duration: aiMsg?.duration
        })
        await deleteChatSession(sessionId)
      }
    }
  } catch (e) {
    qaMessages.value.push({
      role: 'assistant',
      content: '处理请求时出现错误，请稍后重试。'
    })
  } finally {
    qaSending.value = false
  }
}

function formatFileSize(bytes?: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function formatDuration(ms: number) {
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(1)}s`
}

function formatCreatedAt(value?: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit'
  })
}
</script>

<style scoped>
.knowledge-base-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-left h2 {
  margin: 0;
  font-size: 20px;
}
.detail-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.detail-tabs :deep(.el-tabs__content) {
  flex: 1;
}
.detail-tabs :deep(.el-tab-pane) {
  height: 100%;
}
.tab-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.tab-toolbar .tip {
  color: #909399;
  font-size: 13px;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.text-success { color: #67c23a; }
.text-muted { color: #c0c4cc; }
.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
}
.tts-content {
  height: calc(100% - 60px);
}
.tts-toolbar-secondary {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.page-info {
  font-size: 13px;
  color: #606266;
  margin-left: 8px;
}
.tts-body {
  display: flex;
  gap: 12px;
  height: calc(100% - 120px);
}
.tts-body.with-pdf .tts-text-panel {
  flex: 1;
}
.tts-body:not(.with-pdf) .tts-text-panel {
  width: 100%;
}
.tts-pdf-panel {
  width: 50%;
  flex-shrink: 0;
}
.tts-text-panel {
  flex: 1;
  min-width: 0;
}
.chunk-content {
  max-height: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}
:deep(.el-table .cell) {
  white-space: nowrap;
}
:deep(.el-table__body-wrapper) {
  overflow-x: auto;
}
.action-btns {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  white-space: nowrap;
}

/* QA样式 */
.qa-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}
.qa-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #fafafa;
}
.qa-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}
.qa-empty p { margin: 8px 0; }
.qa-empty .sub { font-size: 13px; }
.qa-message {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.qa-message.user {
  flex-direction: row-reverse;
}
.qa-message.user .message-content {
  background: #1890ff;
  color: white;
  border-radius: 12px 0 12px 12px;
}
.qa-message.assistant .message-content {
  background: white;
  border-radius: 0 12px 12px 12px;
  border: 1px solid #e4e7ed;
}
.message-content {
  max-width: 70%;
  padding: 12px 16px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.message-time {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
.message-sources {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0,0,0,0.1);
}
.sources-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}
.source-item {
  font-size: 12px;
  padding: 6px 8px;
  margin: 4px 0;
  background: #f5f7fa;
  border-radius: 4px;
}
.source-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 2px;
}
.source-name { color: #606266; font-weight: 500; }
.source-score { color: #1890ff; font-weight: 500; }
.source-content { color: #909399; font-size: 12px; line-height: 1.5; white-space: pre-wrap; }
.qa-input {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  background: white;
  border-top: 1px solid #e4e7ed;
}
.qa-mode-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}
.qa-mode-tip {
  font-size: 12px;
  color: #909399;
}
.qa-input .el-input { flex: 1; }
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}
.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: bounce 1.4s infinite ease-in-out;
}
.typing-indicator span:nth-child(1) { animation-delay: 0s; }
.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0); }
  40% { transform: scale(1); }
}
</style>

<style>
.ctx-menu {
  position: fixed;
  z-index: 9999;
  min-width: 140px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
}
.ctx-menu-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  transition: background 0.15s;
}
.ctx-menu-item:hover {
  background: #ecf5ff;
  color: #409eff;
}
.ctx-danger { color: #f56c6c; }
.ctx-danger:hover { background: #fef0f0; color: #f56c6c; }
.ctx-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 4px 0;
}
</style>

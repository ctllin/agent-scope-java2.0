<template>
  <el-dialog
    v-model="visible"
    title="手动分块"
    width="90%"
    top="3vh"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="manual-split-container" @click="hideContextMenu">
      <div class="split-left">
        <div class="panel-header">
          <span>文档内容</span>
          <span class="tip">选中文字后右键加入分块</span>
        </div>
        <div
          class="document-content"
          ref="contentRef"
          @mouseup="handleSelection"
          @contextmenu.prevent="handleContextMenu"
        >
          <template v-for="part in contentParts">
            <span :class="{ 'used-text': part.used }">{{ part.text }}</span>
          </template>
        </div>
        <div class="panel-footer">
          <el-button size="small" @click="createBlankChunk">
            <el-icon><Plus /></el-icon>
            新建空白分块
          </el-button>
        </div>
      </div>

      <!-- 右键菜单 -->
      <Teleport to="body">
        <div
          v-if="contextMenu.visible"
          class="context-menu"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
          @click.stop
        >
          <div class="context-menu-item" @click="createChunkFromContextMenu">
            <el-icon><Plus /></el-icon>
            加入分块（{{ contextMenu.text.length }}字）
          </div>
        </div>
      </Teleport>

      <div class="split-right">
        <div class="panel-header">
          <span>分块列表（{{ chunks.length }}）</span>
        </div>
        <div class="chunk-list" v-loading="loading">
          <div v-if="chunks.length === 0" class="empty-tip">
            <el-empty description="暂无分块，请在左侧选中文字创建" :image-size="60" />
          </div>
          <div
            v-for="(chunk, index) in chunks"
            :key="index"
            class="chunk-item"
            :class="{ editing: editingIndex === index, 'drag-over': dragOverIndex === index }"
            draggable="true"
            @dragstart="onDragStart(index, $event)"
            @dragover.prevent="onDragOver(index)"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop(index)"
            @dragend="onDragEnd"
          >
            <div class="chunk-header">
              <span class="drag-handle">⠿</span>
              <span class="chunk-seq">{{ index + 1 }}</span>
              <span class="chunk-length">{{ chunk.content.length }}字</span>
              <div class="chunk-actions">
                <el-button text size="small" @click="editChunk(index)">
                  <el-icon><Edit /></el-icon>
                </el-button>
                <el-button text size="small" type="danger" @click="removeChunk(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>
            <div v-if="editingIndex === index" class="chunk-edit">
              <el-input
                v-model="editingContent"
                type="textarea"
                :autosize="{ minRows: 5, maxRows: 20 }"
                placeholder="编辑分块内容"
              />
              <div class="edit-actions">
                <el-button size="small" type="primary" @click="saveEdit(index)">保存</el-button>
                <el-button size="small" @click="editingIndex = -1">取消</el-button>
              </div>
            </div>
            <div v-else class="chunk-content" @dblclick="editChunk(index)">
              {{ chunk.content }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" :disabled="chunks.length === 0" @click="handleSave">
        保存分块（{{ chunks.length }}）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getDocumentContent, getDocumentChunks, saveManualChunks } from '@/api'

interface ChunkData {
  content: string
  sourceStart?: number
  sourceEnd?: number
}

const props = defineProps<{
  modelValue: boolean
  documentId: string
  knowledgeBaseId: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'saved'): void
}>()

const visible = ref(false)
const loading = ref(false)
const saving = ref(false)
const documentContent = ref('')
const chunks = ref<ChunkData[]>([])
const selectedText = ref('')
const editingIndex = ref(-1)
const editingContent = ref('')
const contentRef = ref<HTMLElement>()
const usedRanges = ref<Array<{ start: number; end: number }>>([])
const contextMenu = ref({ visible: false, x: 0, y: 0, text: '', startOffset: 0, endOffset: 0 })
const dragIndex = ref(-1)
const dragOverIndex = ref(-1)

const contentParts = computed(() => {
  if (!documentContent.value) return []
  const text = documentContent.value
  if (usedRanges.value.length === 0) {
    return [{ text, used: false }]
  }
  const sorted = [...usedRanges.value].sort((a, b) => a.start - b.start)
  const parts: Array<{ text: string; used: boolean }> = []
  let lastEnd = 0
  for (const range of sorted) {
    if (range.start > lastEnd) {
      parts.push({ text: text.slice(lastEnd, range.start), used: false })
    }
    parts.push({ text: text.slice(range.start, range.end), used: true })
    lastEnd = range.end
  }
  if (lastEnd < text.length) {
    parts.push({ text: text.slice(lastEnd), used: false })
  }
  return parts
})

watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val && props.documentId) {
    await loadDocumentContent()
    editingIndex.value = -1
    selectedText.value = ''
  }
})

async function loadDocumentContent() {
  loading.value = true
  try {
    const [contentRes, chunksRes] = await Promise.all([
      getDocumentContent(props.documentId),
      getDocumentChunks(props.documentId)
    ])
    documentContent.value = contentRes.data || ''
    
    // Load existing chunks and find their positions in the document
    const existingChunks = chunksRes.data || []
    if (existingChunks.length > 0 && documentContent.value) {
      const newChunks: ChunkData[] = []
      const newRanges: Array<{ start: number; end: number }> = []
      
      for (const chunk of existingChunks) {
        const content = chunk.content || ''
        // Find the position of this chunk in the document
        const startPos = documentContent.value.indexOf(content)
        if (startPos >= 0) {
          newChunks.push({
            content,
            sourceStart: startPos,
            sourceEnd: startPos + content.length
          })
          newRanges.push({
            start: startPos,
            end: startPos + content.length
          })
        } else {
          // Chunk content not found in document (may have been edited)
          newChunks.push({ content })
        }
      }
      
      chunks.value = newChunks
      usedRanges.value = newRanges
    }
  } catch (e) {
    ElMessage.error('加载文档内容失败')
  } finally {
    loading.value = false
  }
}

function getSelectionOffset() {
  const selection = window.getSelection()
  if (!selection || selection.isCollapsed || !contentRef.value) return null
  const range = selection.getRangeAt(0)
  const preRange = document.createRange()
  preRange.selectNodeContents(contentRef.value)
  preRange.setEnd(range.startContainer, range.startOffset)
  const startOffset = preRange.toString().length
  const endOffset = startOffset + selection.toString().length
  return { startOffset, endOffset }
}

function isOverlapped(start: number, end: number) {
  return usedRanges.value.some(r => start < r.end && end > r.start)
}

function handleSelection() {
  const selection = window.getSelection()
  if (!selection) {
    selectedText.value = ''
    return
  }
  const text = selection.toString().trim()
  if (!text || selection.isCollapsed) {
    selectedText.value = ''
    return
  }
  selectedText.value = text
}

function handleContextMenu(e: MouseEvent) {
  const selection = window.getSelection()
  const text = selection?.toString().trim() || ''
  if (!text || !selection || selection.isCollapsed) {
    contextMenu.value.visible = false
    return
  }

  const offsets = getSelectionOffset()
  if (!offsets) {
    contextMenu.value.visible = false
    return
  }

  if (isOverlapped(offsets.startOffset, offsets.endOffset)) {
    contextMenu.value.visible = false
    selection.removeAllRanges()
    return
  }

  contextMenu.value = {
    visible: true,
    x: e.clientX,
    y: e.clientY,
    text,
    startOffset: offsets.startOffset,
    endOffset: offsets.endOffset
  }
}

function hideContextMenu() {
  contextMenu.value.visible = false
}

function createChunkFromContextMenu() {
  const { text, startOffset, endOffset } = contextMenu.value
  if (!text) return

  usedRanges.value.push({ start: startOffset, end: endOffset })
  chunks.value.push({ content: text, sourceStart: startOffset, sourceEnd: endOffset })

  contextMenu.value.visible = false
  window.getSelection()?.removeAllRanges()
  selectedText.value = ''
}

function createBlankChunk() {
  chunks.value.push({ content: '' })
  editingIndex.value = chunks.value.length - 1
  editingContent.value = ''
}

function onDragStart(index: number, e: DragEvent) {
  dragIndex.value = index
  e.dataTransfer!.effectAllowed = 'move'
  e.dataTransfer!.setData('text/plain', String(index))
}

function onDragOver(index: number) {
  if (dragIndex.value === -1 || dragIndex.value === index) return
  dragOverIndex.value = index
}

function onDragLeave() {
  dragOverIndex.value = -1
}

function onDrop(toIndex: number) {
  const fromIndex = dragIndex.value
  if (fromIndex === -1 || fromIndex === toIndex) return

  const item = chunks.value.splice(fromIndex, 1)[0]
  chunks.value.splice(toIndex, 0, item)

  // 同步更新 editingIndex
  if (editingIndex.value === fromIndex) {
    editingIndex.value = toIndex
  } else if (fromIndex < editingIndex.value && toIndex >= editingIndex.value) {
    editingIndex.value--
  } else if (fromIndex > editingIndex.value && toIndex <= editingIndex.value) {
    editingIndex.value++
  }

  dragIndex.value = -1
  dragOverIndex.value = -1
}

function onDragEnd() {
  dragIndex.value = -1
  dragOverIndex.value = -1
}

function editChunk(index: number) {
  editingIndex.value = index
  editingContent.value = chunks.value[index].content
}

function saveEdit(index: number) {
  if (!editingContent.value.trim()) {
    ElMessage.warning('分块内容不能为空')
    return
  }
  chunks.value[index].content = editingContent.value.trim()
  editingIndex.value = -1
}

function removeChunk(index: number) {
  const chunk = chunks.value[index]
  if (chunk.sourceStart !== undefined && chunk.sourceEnd !== undefined) {
    usedRanges.value = usedRanges.value.filter(
      r => !(r.start === chunk.sourceStart && r.end === chunk.sourceEnd)
    )
  }
  chunks.value.splice(index, 1)
  if (editingIndex.value === index) {
    editingIndex.value = -1
  } else if (editingIndex.value > index) {
    editingIndex.value--
  }
}

async function handleSave() {
  if (chunks.value.length === 0) return
  saving.value = true
  try {
    await saveManualChunks({
      documentId: props.documentId,
      knowledgeBaseId: props.knowledgeBaseId,
      chunks: chunks.value
    })
    ElMessage.success(`保存成功，共 ${chunks.value.length} 个分块`)
    emit('update:modelValue', false)
    emit('saved')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

function handleClose() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.manual-split-container {
  display: flex;
  height: 70vh;
  gap: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}
.split-left, .split-right {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-weight: 500;
  font-size: 14px;
}
.panel-header .tip {
  font-weight: 400;
  font-size: 12px;
  color: #909399;
}
.document-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  user-select: text;
  cursor: text;
}
.used-text {
  background-color: #f0f0f0;
  color: #c0c4cc;
  text-decoration: line-through;
}
.panel-footer {
  padding: 10px 16px;
  border-top: 1px solid #e4e7ed;
  background: #f5f7fa;
  display: flex;
  gap: 8px;
  align-items: center;
}
.chunk-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.empty-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}
.chunk-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  transition: border-color 0.2s, background-color 0.2s;
}
.chunk-item:hover {
  border-color: #c0c4cc;
}
.chunk-item.editing {
  border-color: #409eff;
  overflow: visible;
}
.chunk-item.drag-over {
  border-color: #409eff;
  background-color: #ecf5ff;
}
.drag-handle {
  cursor: grab;
  color: #909399;
  font-size: 14px;
  user-select: none;
}
.drag-handle:active {
  cursor: grabbing;
}
.chunk-header {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  gap: 8px;
}
.chunk-seq {
  font-weight: 600;
  font-size: 12px;
  color: #409eff;
  min-width: 20px;
}
.chunk-length {
  font-size: 12px;
  color: #909399;
  flex: 1;
}
.chunk-actions {
  display: flex;
  gap: 2px;
}
.chunk-content {
  padding: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: #303133;
  cursor: pointer;
  white-space: pre-wrap;
  word-break: break-word;
}
.chunk-content:hover {
  background: #f5f7fa;
}
.chunk-edit {
  padding: 10px;
}
.edit-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.context-menu {
  position: fixed;
  z-index: 9999;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  padding: 4px 0;
  min-width: 160px;
}
.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 14px;
  cursor: pointer;
  color: #303133;
}
.context-menu-item:hover {
  background: #f5f7fa;
  color: #409eff;
}
</style>

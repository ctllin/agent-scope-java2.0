<template>
  <div class="text-viewer" @click="clearSelection" @contextmenu.prevent>
    <!-- 分页模式（PDF/Word） -->
    <template v-if="pageMode">
      <div class="page-nav" v-if="totalPages > 1">
        <el-button size="small" :disabled="currentPage <= 1" @click="goPage(currentPage - 1)">
          <el-icon><ArrowLeft /></el-icon>上一页
        </el-button>
        <span class="page-info">{{ currentPage }} / {{ totalPages }}</span>
        <el-button size="small" :disabled="currentPage >= totalPages" @click="goPage(currentPage + 1)">
          下一页<el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>
      <!-- 页编辑模式 -->
      <div v-if="pageEditing" class="page-edit-area">
        <el-input
          v-model="pageEditText"
          type="textarea"
          :autosize="{ minRows: 10, maxRows: 30 }"
          placeholder="编辑当前页内容..."
        />
      </div>
      <!-- 行显示模式 -->
      <div v-else class="page-content" ref="viewerRef">
        <div
          v-for="(line, idx) in currentPageLines"
          :key="`${currentPage}-${idx}`"
          :class="['text-line', {
            'is-current': currentPage === activePage && idx === activeLineInPage,
            'is-read': currentPage < activePage || (currentPage === activePage && idx < activeLineInPage),
            'is-selected': editing && isSelected(calcGlobalIdx(idx)),
            'is-drag-over': editing && dragOverIdx === idx,
          }]"
          :draggable="editing"
          @click.stop="handleLineClick(idx, $event)"
          @contextmenu.stop.prevent="openMenu($event, calcGlobalIdx(idx))"
          @dragstart="onDragStart($event, idx)"
          @dragover.prevent="onDragOver($event, idx)"
          @dragleave="onDragLeave"
          @drop="onDrop($event, idx)"
          @dragend="onDragEnd"
        >
          <span class="line-num">{{ calcGlobalIdx(idx) + 1 }}</span>
          <el-input
            v-if="editing"
            :model-value="editDraft[calcGlobalIdx(idx)] ?? line"
            type="text"
            class="line-editor"
            @update:model-value="(val: string) => { editDraft[calcGlobalIdx(idx)] = val }"
            @change="commitEdit(idx)"
          />
          <span v-else class="line-text">
            <template v-if="currentPage === activePage && idx === activeLineInPage && currentCharIndex >= 0">
              <span class="text-read">{{ line.substring(0, currentCharIndex + 1) }}</span>
              <span class="text-current">{{ line.charAt(currentCharIndex + 1) || '' }}</span>
              <span class="text-unread">{{ line.substring(currentCharIndex + 2) }}</span>
            </template>
            <template v-else>{{ line }}</template>
          </span>
        </div>
        <div v-if="currentPageLines.length === 0" class="empty-text">该页无内容</div>
      </div>
    </template>

    <!-- 平铺模式（TXT） -->
    <template v-else>
      <div class="page-content" ref="viewerRef">
        <div
          v-for="(line, idx) in lines"
          :key="idx"
          :class="['text-line', {
            'is-current': idx === currentLineIndex,
            'is-read': idx < currentLineIndex,
            'is-selected': editing && isSelected(idx),
            'is-drag-over': editing && dragOverIdx === idx,
          }]"
          :draggable="editing"
          @click.stop="handleLineClick(idx, $event)"
          @contextmenu.stop.prevent="openMenu($event, idx)"
          @dragstart="onDragStart($event, idx)"
          @dragover.prevent="onDragOver($event, idx)"
          @dragleave="onDragLeave"
          @drop="onDrop($event, idx)"
          @dragend="onDragEnd"
          :ref="(el) => { if (idx === currentLineIndex) currentLineRef = el as HTMLElement }"
        >
          <span class="line-num">{{ idx + 1 }}</span>
          <el-input
            v-if="editing"
            :model-value="editDraft[idx] ?? line"
            type="text"
            class="line-editor"
            @update:model-value="(val: string) => { editDraft[idx] = val }"
            @change="commitEdit(idx)"
          />
          <span v-else class="line-text">
            <template v-if="idx === currentLineIndex && currentCharIndex >= 0">
              <span class="text-read">{{ line.substring(0, currentCharIndex + 1) }}</span>
              <span class="text-current">{{ line.charAt(currentCharIndex + 1) || '' }}</span>
              <span class="text-unread">{{ line.substring(currentCharIndex + 2) }}</span>
            </template>
            <template v-else>{{ line }}</template>
          </span>
        </div>
        <div v-if="lines.length === 0" class="empty-text">暂无文本内容</div>
      </div>
    </template>

    <!-- 右键菜单 -->
    <Teleport to="body">
      <div
        v-if="menuVisible"
        class="ctx-menu"
        :style="{ left: menuX + 'px', top: menuY + 'px' }"
        @click.stop
      >
        <div class="ctx-menu-item" @click="handleMerge">合并为一行</div>
        <div class="ctx-menu-item" @click="handleMergeNoSpace">合并为一行(无空格)</div>
        <div class="ctx-divider" />
        <div class="ctx-menu-item ctx-danger" @click="handleDelete">删除选中行</div>
      </div>
    </Teleport>

    <div class="viewer-footer">
      <span v-if="editing && selectedSet.size > 0" class="sel-info">已选 {{ selectedSet.size }} 行</span>
      <el-button v-if="!editing && !pageEditing" text size="small" @click="editing = true">
        <el-icon><Edit /></el-icon>行编辑
      </el-button>
      <el-button v-if="!editing && !pageEditing && pageMode" text size="small" @click="enterPageEdit">
        <el-icon><Edit /></el-icon>页编辑
      </el-button>
      <template v-if="editing">
        <el-button text size="small" @click="editing = false; clearSelection()">
          完成
        </el-button>
      </template>
      <template v-if="pageEditing">
        <el-button text type="success" size="small" @click="savePageEdit">
          保存
        </el-button>
        <el-button text type="info" size="small" @click="cancelPageEdit">
          取消
        </el-button>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onBeforeUnmount } from 'vue'

const props = defineProps<{
  lines: string[]
  currentLineIndex: number
  currentCharIndex: number
  pageMode: boolean
  pageContents?: { page: number; text: string }[]
}>()

const emit = defineEmits<{
  lineClick: [index: number]
  editLine: [index: number, text: string]
  pageChange: [page: number]
  moveLines: [fromIdx: number, count: number, toIdx: number]
  deleteLines: [indices: number[]]
  mergeLines: [indices: number[]]
  mergeLinesNoSpace: [indices: number[]]
  pageEditSave: [pageText: string]
}>()

const viewerRef = ref<HTMLElement>()
const currentLineRef = ref<HTMLElement>()
const editing = ref(false)
const pageEditing = ref(false)
const pageEditText = ref('')
const currentPage = ref(1)
const linesPerPage = 20

// --- 多选 ---
const selectedSet = ref(new Set<number>())
let anchorIdx = -1

function isSelected(idx: number) {
  return selectedSet.value.has(idx)
}

function clearSelection() {
  selectedSet.value.clear()
  hideMenu()
}

function calcGlobalIdx(idx: number): number {
  if (props.pageMode && pageStartOffsets.value) {
    const pageIdx = currentPage.value - 1
    return (pageStartOffsets.value[pageIdx] ?? 0) + idx
  }
  return (currentPage.value - 1) * linesPerPage + idx
}

function handleLineClick(idx: number, e: MouseEvent) {
  let globalIdx: number
  if (props.pageMode && pageStartOffsets.value) {
    const pageIdx = currentPage.value - 1
    globalIdx = (pageStartOffsets.value[pageIdx] ?? 0) + idx
  } else if (props.pageMode) {
    globalIdx = (currentPage.value - 1) * linesPerPage + idx
  } else {
    globalIdx = idx
  }
  if (props.pageMode) {
    emit('lineClick', globalIdx)
  } else {
    emit('lineClick', idx)
  }

  if (!editing.value) return

  if (e.shiftKey && anchorIdx >= 0) {
    const start = Math.min(anchorIdx, globalIdx)
    const end = Math.max(anchorIdx, globalIdx)
    for (let i = start; i <= end; i++) selectedSet.value.add(i)
  } else if (e.ctrlKey || e.metaKey) {
    if (selectedSet.value.has(globalIdx)) selectedSet.value.delete(globalIdx)
    else selectedSet.value.add(globalIdx)
    anchorIdx = globalIdx
  } else {
    selectedSet.value.clear()
    selectedSet.value.add(globalIdx)
    anchorIdx = globalIdx
  }
}

// --- 拖拽 ---
const dragFromIdx = ref(-1)
const dragOverIdx = ref(-1)

function onDragStart(e: DragEvent, idx: number) {
  const globalIdx = calcGlobalIdx(idx)
  dragFromIdx.value = globalIdx
  if (!selectedSet.value.has(globalIdx)) {
    selectedSet.value.clear()
    selectedSet.value.add(globalIdx)
  }
  e.dataTransfer!.effectAllowed = 'move'
}

function onDragOver(_e: DragEvent, idx: number) {
  dragOverIdx.value = idx
}

function onDragLeave() {
  dragOverIdx.value = -1
}

function onDrop(_e: DragEvent, toIdx: number) {
  const globalTo = calcGlobalIdx(toIdx)
  const from = dragFromIdx.value
  if (from < 0 || from === globalTo) return

  const count = selectedSet.value.size || 1
  emit('moveLines', from, count, globalTo)
  dragFromIdx.value = -1
  dragOverIdx.value = -1
}

function onDragEnd() {
  dragFromIdx.value = -1
  dragOverIdx.value = -1
}

// --- 右键菜单 ---
const menuVisible = ref(false)
const menuX = ref(0)
const menuY = ref(0)
const menuTargetIdx = ref(-1)

function openMenu(e: MouseEvent, globalIdx: number) {
  if (!selectedSet.value.has(globalIdx)) {
    selectedSet.value.clear()
    selectedSet.value.add(globalIdx)
  }
  menuTargetIdx.value = globalIdx
  menuX.value = e.clientX
  menuY.value = e.clientY
  menuVisible.value = true
}

function hideMenu() {
  menuVisible.value = false
}

function handleMerge() {
  const sorted = [...selectedSet.value].sort((a, b) => a - b)
  if (sorted.length === 0) return
  emit('mergeLines', sorted)
  clearSelection()
}

function handleMergeNoSpace() {
  const sorted = [...selectedSet.value].sort((a, b) => a - b)
  if (sorted.length === 0) return
  emit('mergeLinesNoSpace', sorted)
  clearSelection()
}

function handleDelete() {
  const sorted = [...selectedSet.value].sort((a, b) => a - b)
  if (sorted.length === 0) return
  emit('deleteLines', sorted)
  clearSelection()
}

function onDocClick() {
  hideMenu()
}

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})

watch(menuVisible, (v) => {
  if (v) document.addEventListener('click', onDocClick, { once: true })
})

// --- 分页 ---

// 从 pageContents 计算每页在 lines 数组中的起始偏移
const pageStartOffsets = computed(() => {
  if (!props.pageMode || !props.pageContents || props.pageContents.length === 0) return null
  const offsets: number[] = []
  let offset = 0
  for (const pc of props.pageContents) {
    offsets.push(offset)
    const pageCount = Math.max((pc.text || '').split('\n').filter((l: string) => l.trim()).length, 1)
    offset += pageCount
  }
  return offsets
})

const totalPages = computed(() => {
  if (!props.pageMode) return 1
  if (pageStartOffsets.value) return pageStartOffsets.value.length
  return Math.ceil(props.lines.length / linesPerPage) || 1
})

const currentPageLines = computed(() => {
  if (!props.pageMode) return props.lines
  if (pageStartOffsets.value) {
    const idx = currentPage.value - 1
    const start = pageStartOffsets.value[idx] ?? 0
    const nextStart = idx + 1 < pageStartOffsets.value.length
      ? pageStartOffsets.value[idx + 1]
      : props.lines.length
    return props.lines.slice(start, nextStart)
  }
  const start = (currentPage.value - 1) * linesPerPage
  return props.lines.slice(start, start + linesPerPage)
})

const activePage = computed(() => {
  if (!props.pageMode) return 1
  if (pageStartOffsets.value) {
    const offsets = pageStartOffsets.value
    for (let i = offsets.length - 1; i >= 0; i--) {
      if (props.currentLineIndex >= offsets[i]) return i + 1
    }
    return 1
  }
  return Math.floor(props.currentLineIndex / linesPerPage) + 1
})

const activeLineInPage = computed(() => {
  if (!props.pageMode) return props.currentLineIndex
  if (pageStartOffsets.value) {
    const idx = activePage.value - 1
    const start = pageStartOffsets.value[idx] ?? 0
    return props.currentLineIndex - start
  }
  return props.currentLineIndex % linesPerPage
})

function goPage(page: number) {
  currentPage.value = page
  emit('pageChange', page)
}

function enterPageEdit() {
  pageEditText.value = currentPageLines.value.join('\n')
  pageEditing.value = true
}

function savePageEdit() {
  emit('pageEditSave', pageEditText.value)
  pageEditing.value = false
}

function cancelPageEdit() {
  pageEditing.value = false
}

// --- 行编辑草稿 ---
// el-input 单向 :model-value 时输入会被重渲染覆盖，必须用本地草稿承接 update 事件
const editDraft = ref<Record<number, string>>({})

function commitEdit(idx: number) {
  const gIdx = calcGlobalIdx(idx)
  const val = editDraft.value[gIdx]
  if (val === undefined) return
  delete editDraft.value[gIdx]
  const original = props.pageMode
    ? (currentPageLines.value[idx] ?? '')
    : props.lines[gIdx]
  if (val !== original) {
    emit('editLine', gIdx, val)
  }
}

watch(editing, (on) => {
  if (!on) editDraft.value = {}
})

watch(() => currentPage.value, () => {
  editDraft.value = {}
})

watch(activePage, (p) => {
  if (props.pageMode && p !== currentPage.value) {
    currentPage.value = p
  }
})

function scrollLineIntoView() {
  nextTick(() => {
    if (currentLineRef.value && viewerRef.value) {
      const container = viewerRef.value
      const el = currentLineRef.value
      const containerRect = container.getBoundingClientRect()
      const elRect = el.getBoundingClientRect()
      if (elRect.top < containerRect.top || elRect.bottom > containerRect.bottom) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }
  })
}

watch(() => props.currentLineIndex, () => scrollLineIntoView())

defineExpose({ scrollLineIntoView, goPage })
</script>

<style scoped>
.text-viewer {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fafafa;
}
.page-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 16px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}
.page-info {
  font-size: 13px;
  color: #606266;
  min-width: 60px;
  text-align: center;
}
.page-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  font-size: 15px;
  line-height: 1.8;
}
.text-line {
  display: flex;
  gap: 12px;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
  user-select: none;
}
.text-line:hover {
  background: #ecf5ff;
}
.text-line.is-current {
  background: #ecf5ff;
  border-left: 3px solid #409eff;
  padding-left: 5px;
}
.text-line.is-read {
  color: #c0c4cc;
}
.text-line.is-selected {
  background: #d9ecff !important;
  border-left: 3px solid #67c23a;
  padding-left: 5px;
}
.text-line.is-drag-over {
  border-top: 2px solid #409eff;
}
.line-num {
  color: #c0c4cc;
  min-width: 28px;
  text-align: right;
  font-size: 13px;
  user-select: none;
  flex-shrink: 0;
}
.line-text {
  flex: 1;
  white-space: pre-wrap;
  word-break: break-all;
}
.line-editor {
  flex: 1;
}
.text-read {
  color: #c0c4cc;
}
.text-current {
  color: #f56c6c;
  font-weight: bold;
  border-bottom: 2px solid #f56c6c;
}
.text-unread {
  color: #303133;
}
.empty-text {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
  color: #c0c4cc;
}
.viewer-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 6px 12px;
  border-top: 1px solid #e4e7ed;
  background: #f5f7fa;
  flex-shrink: 0;
}
.page-edit-area {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}
.page-edit-area :deep(.el-textarea__inner) {
  font-size: 15px;
  line-height: 1.8;
  font-family: inherit;
}
.sel-info {
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}
</style>

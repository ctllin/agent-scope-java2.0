<template>
  <div class="tts-page">
    <!-- ============ 合成区 ============ -->
    <el-card shadow="never" class="synth-card">
      <div class="synth-header">
        <span class="synth-title">文本转语音</span>
        <span class="synth-tip">输入文章内容，按行/段落/整篇合成朗读音频，支持播放跟随高亮</span>
      </div>
      <el-input
        v-model="inputTitle"
        placeholder="标题（可选，默认取正文前20字）"
        maxlength="50"
        style="margin-bottom: 10px;"
      />
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="6"
        placeholder="在此粘贴或输入要朗读的文章内容..."
        maxlength="50000"
        show-word-limit
      />
      <div class="synth-actions">
        <span class="mode-label">合成方式</span>
        <el-radio-group v-model="synthMode">
          <el-radio-button value="LINE">按行</el-radio-button>
          <el-radio-button value="PARAGRAPH">按段落</el-radio-button>
          <el-radio-button value="ALL">整篇</el-radio-button>
        </el-radio-group>
        <el-button
          type="primary"
          :loading="synthesizing"
          :disabled="!inputText.trim()"
          @click="handleSynthesize"
        >
          <el-icon v-if="!synthesizing"><VideoPlay /></el-icon>
          开始合成
        </el-button>
        <span class="char-hint">{{ inputText.length }} 字</span>
      </div>
    </el-card>

    <!-- ============ 主体两栏 ============ -->
    <div class="main-area">
      <!-- 左：记录列表 -->
      <el-card shadow="never" class="records-card">
        <template #header>
          <div class="card-header">
            <span>合成记录</span>
            <span v-if="hasSynthesizing" class="synthesizing-tip">
              <el-icon class="is-loading"><Loading /></el-icon>
              合成中...
            </span>
          </div>
        </template>
        <div class="toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="按标题/内容模糊搜索"
            clearable
            style="width: 180px;"
            @keyup.enter="handleFilterChange"
            @clear="handleFilterChange"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="statusFilter"
            placeholder="状态"
            clearable
            style="width: 110px;"
            @change="handleFilterChange"
          >
            <el-option label="合成中" value="SYNTHESIZING" />
            <el-option label="已完成" value="DONE" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
            批量删除 ({{ selectedIds.length }})
          </el-button>
        </div>

        <el-table
          ref="tableRef"
          :data="records"
          size="small"
          row-key="id"
          highlight-current-row
          @current-change="handleCurrentRowChange"
          @selection-change="(rows: TtsRecord[]) => selectedIds = rows.map(r => r.id)"
        >
          <el-table-column type="selection" width="42" reserve-selection />
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column label="方式" width="72" align="center">
            <template #default="{ row }">{{ modeLabel(row.mode) }}</template>
          </el-table-column>
          <el-table-column label="时长" width="70" align="center">
            <template #default="{ row }">{{ row.duration ? formatTime(row.duration) : '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="82" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="118" align="center" fixed="right">
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                :disabled="row.status !== 'DONE'"
                @click="handlePlayRecord(row)"
              >
                播放
              </el-button>
              <el-popconfirm title="确定删除该记录吗？" @confirm="handleDelete(row)">
                <template #reference>
                  <el-button link type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          small
          class="pager"
          @current-change="loadRecords"
          @size-change="handleFilterChange"
        />
      </el-card>

      <!-- 右：阅读高亮区 -->
      <el-card shadow="never" class="reader-card">
        <template #header>
          <div class="card-header">
            <span class="reader-title">{{ activeRecord?.title || '阅读区' }}</span>
            <span v-if="activeRecord" class="reader-meta">
              {{ modeLabel(activeRecord.mode) }} · {{ activeRecord.charCount || activeRecord.text.length }} 字 ·
              {{ activeRecord.duration ? formatTime(activeRecord.duration) : '' }}
            </span>
          </div>
        </template>

        <div v-if="!activeRecord" class="reader-empty">
          <el-empty description="点击左侧记录行或「播放」按钮，此处展示全文并跟随高亮朗读" />
        </div>

        <div v-else ref="readerRef" class="reader-body">
          <div
            v-for="(line, idx) in displayLines"
            :key="idx"
            class="reader-line"
            :class="{
              'line-active': idx === activeLineIdx,
              'line-past': playing && idx < activeLineIdx
            }"
            @click="seekToLine(idx)"
          >
            <template v-if="idx === activeLineIdx && line.text">
              <span class="char-done">{{ line.text.slice(0, Math.max(activeCharInLine, 0)) }}</span><span class="char-active">{{ line.text.slice(Math.max(activeCharInLine, 0), Math.max(activeCharInLine, 0) + 1) }}</span><span>{{ line.text.slice(Math.max(activeCharInLine, 0) + 1) }}</span>
            </template>
            <template v-else>{{ line.text }}</template>
          </div>
        </div>

        <!-- 播放条 -->
        <div v-if="activeRecord && audioReady" class="tts-player">
          <el-button
            circle
            type="primary"
            :icon="playing ? VideoPause : VideoPlay"
            @click="togglePlay"
          />
          <el-button circle :icon="CircleClose" @click.stop="stopPlayback" />
          <el-slider
            :model-value="progressPercent"
            :min="0"
            :max="100"
            :step="0.1"
            :show-tooltip="false"
            class="player-slider"
            @input="onSeek"
          />
          <span class="player-time">{{ formatTime(currentTime) }} / {{ formatTime(totalDuration) }}</span>
          <el-select v-model="playbackRate" style="width: 86px;" @change="applyRate">
            <el-option v-for="r in rateOptions" :key="r" :label="r + 'x'" :value="r" />
          </el-select>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { VideoPlay, VideoPause, CircleClose } from '@element-plus/icons-vue'
import {
  createTtsRecord,
  getTtsRecords,
  getTtsRecordDetail,
  deleteTtsRecord,
  deleteTtsRecords,
  ttsAudioUrl
} from '@/api'
import type { TtsRecord, TtsSegment } from '@/types'

// ==================== 合成表单 ====================
const inputTitle = ref('')
const inputText = ref('')
const synthMode = ref<'LINE' | 'PARAGRAPH' | 'ALL'>('LINE')
const synthesizing = ref(false)

// ==================== 记录列表 ====================
const records = ref<TtsRecord[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const statusFilter = ref('')
const searchKeyword = ref('')
const selectedIds = ref<string[]>([])
const tableRef = ref()
let pollTimer: number | null = null

const hasSynthesizing = computed(() => records.value.some(r => r.status === 'SYNTHESIZING'))

function modeLabel(mode: string) {
  return mode === 'PARAGRAPH' ? '按段落' : mode === 'ALL' ? '整篇' : '按行'
}
function statusLabel(status: string) {
  return status === 'SYNTHESIZING' ? '合成中' : status === 'FAILED' ? '失败' : '已完成'
}
function statusType(status: string): 'warning' | 'success' | 'danger' | 'info' {
  return status === 'SYNTHESIZING' ? 'warning' : status === 'FAILED' ? 'danger' : 'success'
}

async function loadRecords() {
  try {
    const res = await getTtsRecords(page.value, pageSize.value, statusFilter.value || undefined, searchKeyword.value.trim() || undefined)
    records.value = res.data.records
    total.value = res.data.total
    schedulePoll()
  } catch (e) {
    console.error(e)
  }
}

function handleFilterChange() {
  page.value = 1
  loadRecords()
}

function schedulePoll() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
  if (hasSynthesizing.value) {
    pollTimer = window.setTimeout(async () => {
      const wasSynthesizing = !!activeId && records.value.find(r => r.id === activeId)?.status === 'SYNTHESIZING'
      await loadRecords()
      // 正在查看的记录合成完成后自动刷新详情并开始播放
      if (wasSynthesizing && autoPlayAfterDone && activeId) {
        const cur = records.value.find(r => r.id === activeId)
        if (cur?.status === 'DONE') {
          autoPlayAfterDone = false
          await openReader(cur)
          startPlayback(0)
        } else if (cur?.status === 'FAILED') {
          autoPlayAfterDone = false
          ElMessage.error(`「${cur.title}」合成失败：${cur.errorMessage || '未知原因'}`)
        }
      }
      schedulePoll()
    }, 2000)
  }
}

async function handleSynthesize() {
  const text = inputText.value
  if (!text.trim()) return
  synthesizing.value = true
  try {
    await createTtsRecord({ title: inputTitle.value.trim() || undefined, text, mode: synthMode.value })
    ElMessage.success('已提交合成任务')
    autoPlayAfterDone = true
    inputText.value = ''
    inputTitle.value = ''
    page.value = 1
    await loadRecords()
  } catch (e) {
    console.error(e)
  } finally {
    synthesizing.value = false
  }
}

async function handleDelete(row: TtsRecord) {
  try {
    if (activeId === row.id) stopPlaybackAndClear()
    await deleteTtsRecord(row.id)
    ElMessage.success('删除成功')
    await loadRecords()
  } catch (e) {
    console.error(e)
  }
}

async function handleBatchDelete() {
  try {
    if (activeId && selectedIds.value.includes(activeId)) stopPlaybackAndClear()
    await deleteTtsRecords(selectedIds.value)
    ElMessage.success(`已删除 ${selectedIds.value.length} 条记录`)
    tableRef.value?.clearSelection()
    selectedIds.value = []
    await loadRecords()
  } catch (e) {
    // 用户取消或业务错误由拦截器提示
  }
}

// ==================== 阅读高亮区 ====================
interface DisplayLine { text: string; start: number; end: number }

const activeRecord = ref<TtsRecord | null>(null)
const readerRef = ref<HTMLElement>()
const displayLines = ref<DisplayLine[]>([])
const activeLineIdx = ref(-1)
const activeCharInLine = ref(-1)
let activeId: string | null = null
let autoPlayAfterDone = false

/** 将原文按换行拆为展示行（保留原文偏移） */
function buildDisplayLines(text: string): DisplayLine[] {
  const lines: DisplayLine[] = []
  let start = 0
  while (start <= text.length) {
    let end = text.indexOf('\n', start)
    if (end < 0) end = text.length
    // 去掉行尾\r（兼容windows换行）
    let tEnd = end
    if (tEnd > start && text[tEnd - 1] === '\r') tEnd--
    lines.push({ text: text.slice(start, tEnd), start, end: tEnd })
    if (end >= text.length) break
    start = end + 1
  }
  return lines
}

// ==================== 播放器 ====================
const audioRef = new Audio()
const playing = ref(false)
const audioReady = ref(false)
const currentTime = ref(0)
const totalDuration = ref(0)
const playbackRate = ref(1)
const rateOptions = [0.5, 0.75, 1, 1.25, 1.5, 2]
const progressPercent = computed(() =>
  totalDuration.value > 0 ? (currentTime.value / totalDuration.value) * 100 : 0
)

let rafId: number | null = null

/** 各段有效起始时间与时长（缺失时按时长占比补齐） */
let segStarts: number[] = []
let segDurs: number[] = []

function computeSegTimeline(segs: TtsSegment[], totalDur: number) {
  const n = segs.length
  segDurs = segs.map(s => (s.duration && s.duration > 0 ? s.duration : -1))
  const knownSum = segDurs.filter(d => d > 0).reduce((a, b) => a + b, 0)
  const unknownChars = segs.reduce((acc, s, i) => acc + (segDurs[i] < 0 ? s.charEnd - s.charStart : 0), 0)
  const estPerChar = unknownChars > 0 ? Math.max(totalDur - knownSum, 0) / unknownChars : 0
  for (let i = 0; i < n; i++) {
    if (segDurs[i] < 0) segDurs[i] = Math.max((segs[i].charEnd - segs[i].charStart) * estPerChar, 0.05)
  }
  segStarts = new Array(n)
  let acc = 0
  for (let i = 0; i < n; i++) {
    segStarts[i] = acc
    acc += segDurs[i]
  }
}

/** 二分查找当前时间所在段 */
function findSegIndex(t: number): number {
  let lo = 0
  let hi = segStarts.length - 1
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1
    if (segStarts[mid] <= t) lo = mid
    else hi = mid - 1
  }
  return lo
}

/** 全局字符坐标 → 展示行索引与行内偏移 */
function locateLine(globalChar: number): { lineIdx: number; charInLine: number } {
  const lines = displayLines.value
  for (let i = 0; i < lines.length; i++) {
    if (globalChar >= lines[i].start && globalChar < lines[i].end) {
      return { lineIdx: i, charInLine: globalChar - lines[i].start }
    }
  }
  // 落在空白/换行区域时归到前一行末尾
  for (let i = lines.length - 1; i >= 0; i--) {
    if (lines[i].text.length > 0 && globalChar >= lines[i].start) {
      return { lineIdx: i, charInLine: lines[i].text.length }
    }
  }
  return { lineIdx: -1, charInLine: -1 }
}

function tick() {
  const audio = audioRef
  if (!audio.paused) {
    currentTime.value = audio.currentTime
    const t = audio.currentTime
    if (activeRecord.value?.segments?.length && totalDuration.value > 0) {
      const i = findSegIndex(t)
      const seg = activeRecord.value.segments[i]
      const p = segDurs[i] > 0 ? (t - segStarts[i]) / segDurs[i] : 0
      const g = seg.charStart + Math.round(p * (seg.charEnd - seg.charStart))
      const clamped = Math.min(Math.max(g, seg.charStart), seg.charEnd - 1)
      const { lineIdx, charInLine } = locateLine(clamped)
      if (lineIdx !== activeLineIdx.value) {
        activeLineIdx.value = lineIdx
        scrollToLine(lineIdx)
      }
      activeCharInLine.value = charInLine
    }
  }
  rafId = requestAnimationFrame(tick)
}

function scrollToLine(idx: number) {
  nextTick(() => {
    const el = readerRef.value?.children[idx] as HTMLElement | undefined
    el?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  })
}

/** 点击/选中记录行 → 阅读区直接展示该记录全文（不自动播放） */
async function handleCurrentRowChange(row: TtsRecord | null) {
  if (!row || row.id === activeId) return
  // 切换查看的记录：停止当前播放
  stopPlaybackInternal()
  activeId = row.id
  await openReader(row)
}

async function handlePlayRecord(row: TtsRecord) {
  if (activeId !== row.id) {
    stopPlaybackInternal()
  }
  activeId = row.id
  autoPlayAfterDone = false
  const ok = await openReader(row)
  if (ok) startPlayback(0)
}

/** 加载详情并准备播放资源 */
async function openReader(row: TtsRecord): Promise<boolean> {
  try {
    const res = await getTtsRecordDetail(row.id)
    const detail = res.data
    if (!detail.segments?.length) {
      ElMessage.warning('该记录缺少分段信息')
      return false
    }
    activeRecord.value = detail
    displayLines.value = buildDisplayLines(detail.text)
    totalDuration.value = detail.duration || 0
    computeSegTimeline(detail.segments, totalDuration.value)
    currentTime.value = 0
    activeLineIdx.value = -1
    activeCharInLine.value = -1
    audioReady.value = true
    audioRef.src = ttsAudioUrl(detail.id)
    audioRef.load()
    return true
  } catch (e) {
    console.error(e)
    return false
  }
}

function startPlayback(fromSec: number) {
  audioRef.currentTime = fromSec
  applyRate()
  audioRef.play().catch(() => {})
}

function togglePlay() {
  if (!audioReady.value) return
  if (audioRef.paused) {
    applyRate()
    audioRef.play().catch(() => {})
  } else {
    audioRef.pause()
  }
}

function stopPlayback() {
  stopPlaybackInternal()
}

function stopPlaybackInternal() {
  audioRef.pause()
  audioRef.currentTime = 0
  currentTime.value = 0
  playing.value = false
  activeLineIdx.value = -1
  activeCharInLine.value = -1
}

function stopPlaybackAndClear() {
  stopPlaybackInternal()
  audioRef.removeAttribute('src')
  audioReady.value = false
  activeRecord.value = null
  activeId = null
}

function onSeek(pct: number) {
  if (!audioReady.value || totalDuration.value <= 0) return
  const t = (pct / 100) * totalDuration.value
  audioRef.currentTime = t
  currentTime.value = t
}

/** 点击某行 → 从包含该行起点的分段处开始播 */
function seekToLine(idx: number) {
  const line = displayLines.value[idx]
  if (!line || !activeRecord.value?.segments?.length) return
  const g = line.start
  const segs = activeRecord.value.segments
  let target = segs.findIndex(s => g >= s.charStart && g < s.charEnd)
  if (target < 0) {
    // 行起点落在段间空白：找下一个覆盖点
    target = segs.findIndex(s => s.charStart >= g)
    if (target < 0) target = segs.length - 1
  }
  const seg = segs[target]
  const ratio = seg.charEnd > seg.charStart ? (Math.min(g, seg.charEnd) - seg.charStart) / (seg.charEnd - seg.charStart) : 0
  const time = segStarts[target] + ratio * segDurs[target] + 0.01
  if (audioRef.paused && !audioReady.value) return
  startPlayback(time)
  if (audioRef.paused) togglePlay()
}

function applyRate() {
  audioRef.playbackRate = playbackRate.value
}

function formatTime(sec: number): string {
  if (!sec || sec <= 0) return '00:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

// ==================== 音频事件 ====================
audioRef.addEventListener('play', () => {
  playing.value = true
  if (rafId === null) rafId = requestAnimationFrame(tick)
})
audioRef.addEventListener('pause', () => {
  playing.value = false
})
audioRef.addEventListener('ended', () => {
  playing.value = false
  currentTime.value = 0
  activeLineIdx.value = -1
  activeCharInLine.value = -1
})
audioRef.addEventListener('error', () => {
  if (audioReady.value) ElMessage.error('音频加载失败，请重试')
})

onMounted(() => {
  loadRecords()
})

onBeforeUnmount(() => {
  if (pollTimer) clearTimeout(pollTimer)
  if (rafId !== null) cancelAnimationFrame(rafId)
  audioRef.pause()
  audioRef.removeAttribute('src')
})
</script>

<style scoped lang="scss">
.tts-page {
  padding: 16px;
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-sizing: border-box;
  overflow: hidden;

  .synth-card {
    .synth-header {
      margin-bottom: 12px;

      .synth-title {
        font-weight: 600;
        margin-right: 12px;
      }

      .synth-tip {
        font-size: 12px;
        color: #909399;
      }
    }

    .synth-actions {
      display: flex;
      align-items: center;
      gap: 14px;
      margin-top: 12px;

      .mode-label {
        font-size: 13px;
        color: #606266;
      }

      .char-hint {
        margin-left: auto;
        font-size: 12px;
        color: #909399;
      }
    }
  }

  .main-area {
    flex: 1;
    min-height: 0;
    display: grid;
    grid-template-columns: minmax(480px, 5fr) 4fr;
    gap: 12px;
  }

  .records-card {
    display: flex;
    flex-direction: column;
    overflow: hidden;

    :deep(.el-card__body) {
      flex: 1;
      min-height: 0;
      overflow: auto;
      display: flex;
      flex-direction: column;
    }

    .card-header {
      display: flex;
      align-items: center;
      gap: 10px;

      .synthesizing-tip {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        color: #e6a23c;
        font-size: 13px;
      }
    }

    .toolbar {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 10px;
      flex-wrap: wrap;
    }

    .pager {
      margin-top: 10px;
      justify-content: flex-end;
    }
  }

  .reader-card {
    display: flex;
    flex-direction: column;
    overflow: hidden;

    :deep(.el-card__body) {
      flex: 1;
      min-height: 0;
      display: flex;
      flex-direction: column;
      padding-bottom: 0;
    }

    .card-header {
      display: flex;
      align-items: baseline;
      gap: 10px;
      min-width: 0;

      .reader-title {
        font-weight: 600;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .reader-meta {
        font-size: 12px;
        color: #909399;
        flex-shrink: 0;
      }
    }

    .reader-empty {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .reader-body {
      flex: 1;
      min-height: 0;
      overflow-y: auto;
      padding: 8px 12px;
      line-height: 2;
      font-size: 15px;

      .reader-line {
        cursor: pointer;
        border-radius: 4px;
        transition: background-color 0.15s;
        word-break: break-all;

        &:hover {
          background-color: #f5f7fa;
        }

        &.line-active {
          background-color: #ecf5ff;

          .char-active {
            background: #409eff;
            color: #fff;
            border-radius: 2px;
            padding: 0 1px;
          }

          .char-done {
            color: #409eff;
          }
        }

        &.line-past {
          color: #b0b3ba;
        }
      }
    }

    .tts-player {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 12px;
      border-top: 1px solid #ebeef5;
      background: #fafafa;

      .player-slider {
        flex: 1;
      }

      .player-time {
        font-size: 12px;
        color: #606266;
        font-variant-numeric: tabular-nums;
        white-space: nowrap;
      }
    }
  }
}
</style>

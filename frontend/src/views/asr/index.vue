<template>
  <div class="asr-page">
    <el-tabs v-model="activeTab">
      <!-- ============ 文件识别 ============ -->
      <el-tab-pane label="文件识别" name="file">
        <div class="upload-area">
          <el-upload
            drag
            multiple
            :auto-upload="false"
            :show-file-list="false"
            accept=".wav,.mp3,.m4a,.aac,.ogg,.flac,.wma,.amr,.opus,.webm"
            :on-change="(file: any) => handleFilesSelected([file.raw])"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽音频文件到此处，或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">支持 wav/mp3/m4a/ogg/flac 等，自动转码为16k单声道后识别</div>
            </template>
          </el-upload>
        </div>

        <div class="toolbar">
          <el-input
            v-model="searchKeyword"
            placeholder="按识别内容模糊搜索"
            clearable
            style="width: 220px;"
            @keyup.enter="handleFilterChange"
            @clear="handleFilterChange"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="statusFilter"
            placeholder="识别状态"
            clearable
            style="width: 130px;"
            @change="handleFilterChange"
          >
            <el-option label="待识别" value="UPLOADED" />
            <el-option label="识别中" value="RECOGNIZING" />
            <el-option label="已完成" value="DONE" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          <el-tooltip content="识别语言" placement="top">
            <el-segmented
              v-model="asrLang"
              :options="langOptions"
              size="small"
              class="lang-segmented"
            />
          </el-tooltip>
          <el-button type="primary" :disabled="!selectedFileIds.length" @click="handleBatchRecognize">
            识别选中 ({{ selectedFileIds.length }})
          </el-button>
          <el-button type="danger" :disabled="!selectedFileIds.length" @click="handleBatchDelete">
            批量删除 ({{ selectedFileIds.length }})
          </el-button>
          <span v-if="hasRecognizing" class="recognizing-tip">
            <el-icon class="is-loading"><Loading /></el-icon>
            正在识别...
          </span>
        </div>

        <el-table
          ref="fileTableRef"
          :data="records"
          row-key="id"
          @selection-change="(rows: AsrRecord[]) => selectedFileIds = rows.map(r => r.id)"
        >
          <el-table-column type="selection" width="45" reserve-selection />
          <el-table-column prop="name" label="文件名" min-width="180" show-overflow-tooltip />
          <el-table-column label="大小" width="90">
            <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="识别结果" min-width="300">
            <template #default="{ row }">
              <div v-if="row.text" class="record-text" title="点击查看全文" @click="openTextView(row)">
                {{ row.text }}
              </div>
              <span v-else-if="row.status === 'FAILED'" class="error-text">{{ row.errorMessage || '识别失败' }}</span>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="215" fixed="right">
            <template #default="{ row }">
              <div class="action-btns">
                <el-button link type="primary" :loading="row.status === 'RECOGNIZING'" @click="handleRecognize(row)">
                  {{ row.text ? '重新识别' : '识别' }}
                </el-button>
                <el-button
                  link
                  :type="playingId === row.id ? 'warning' : 'primary'"
                  :disabled="row.status === 'RECOGNIZING'"
                  @click="togglePlay(row)"
                >
                  <el-icon><VideoPlay /></el-icon>{{ playingId === row.id ? '暂停' : '播放' }}
                </el-button>
                <el-popover trigger="click" width="280">
                  <template #reference>
                    <el-button link type="info">{{ getRate(row.id).toFixed(2) }}x</el-button>
                  </template>
                  <div class="speed-control">
                    <div class="speed-label">
                      <span>播放速度（仅本条记录）</span>
                      <span class="speed-value">{{ getRate(row.id).toFixed(2) }}x</span>
                    </div>
                    <el-slider
                      :model-value="getRate(row.id)"
                      :min="0.01"
                      :max="5"
                      :step="0.1"
                      :show-tooltip="false"
                      @input="(val: number) => setRate(row.id, val)"
                    />
                    <div class="speed-quick">
                      <el-button size="small" @click="adjustRate(row.id, -0.1)">-0.1</el-button>
                      <el-button size="small" @click="setRate(row.id, 1)">重置</el-button>
                      <el-button size="small" @click="adjustRate(row.id, 0.1)">+0.1</el-button>
                    </div>
                  </div>
                </el-popover>
                <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100, 200]"
          :total="total"
          background
          layout="total, sizes, prev, pager, next"
          @size-change="loadRecords"
          @current-change="loadRecords"
        />
      </el-tab-pane>

      <!-- ============ 实时识别 ============ -->
      <el-tab-pane label="实时识别" name="realtime">
        <div class="realtime-panel">
          <el-alert
            v-if="micSilent && realtimeRunning"
            type="warning"
            :closable="false"
            show-icon
            title="麦克风没有声音输入，请检查系统输入设备与音量设置"
            style="margin-bottom: 12px;"
          />
          <div class="realtime-controls">
            <el-radio-group v-model="realtimeMode" :disabled="realtimeRunning">
              <el-radio-button value="browser">浏览器麦克风</el-radio-button>
              <el-radio-button value="server">服务器麦克风</el-radio-button>
            </el-radio-group>

            <el-button
              :type="realtimeRunning ? 'danger' : 'success'"
              size="large"
              :loading="connecting"
              @click="toggleRealtime"
            >
              <el-icon><Microphone /></el-icon>
              {{ realtimeRunning ? '停止识别' : '开始识别' }}
            </el-button>

            <span v-if="realtimeDuration > 0" class="duration-tip">
              已进行 {{ formatDuration(realtimeDuration * 1000) }}
            </span>
          </div>

          <div class="transcript-area">
            <div v-if="!realtimeFinal && !realtimePartial && !realtimeRunning" class="empty-hint">
              点击「开始识别」后对着麦克风说话，识别文本将实时显示在这里
            </div>
            <template v-else>
              <span>{{ realtimeFinal }}</span>
              <span class="partial-text">{{ realtimePartial }}</span><span v-if="realtimeRunning" class="cursor-blink">▌</span>
            </template>
          </div>

          <div class="realtime-footer" v-if="realtimeFinal">
            <el-button text type="primary" @click="copyToClipboard(realtimeFinal)"><el-icon><CopyDocument /></el-icon>复制全文</el-button>
          </div>

          <div class="history-title">实时识别历史</div>
          <div class="toolbar">
            <el-button
              type="danger"
              :disabled="!rtSelectedIds.length"
              @click="handleRtBatchDelete"
            >
              批量删除 ({{ rtSelectedIds.length }})
            </el-button>
          </div>
          <el-table
            ref="rtTableRef"
            :data="realtimeRecords"
            size="small"
            row-key="id"
            @selection-change="(rows: AsrRecord[]) => rtSelectedIds = rows.map(r => r.id)"
          >
            <el-table-column type="selection" width="45" reserve-selection />            <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
            <el-table-column label="时长" width="80">
              <template #default="{ row }">{{ row.duration ? row.duration + 's' : '-' }}</template>
            </el-table-column>
            <el-table-column label="内容" min-width="300">
              <template #default="{ row }">
                <div v-if="row.text" class="record-text" title="点击查看全文" @click="openTextView(row)">
                  {{ row.text }}
                </div>
                <span v-else class="muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <div class="action-btns">
                  <el-button
                    link
                    :type="playingId === row.id ? 'warning' : 'primary'"
                    @click="togglePlay(row)"
                  >
                    <el-icon><VideoPlay /></el-icon>{{ playingId === row.id ? '暂停' : '播放' }}
                  </el-button>
                  <el-popover trigger="click" width="280">
                    <template #reference>
                      <el-button link type="info">{{ getRate(row.id).toFixed(2) }}x</el-button>
                    </template>
                    <div class="speed-control">
                      <div class="speed-label">
                        <span>播放速度（仅本条记录）</span>
                        <span class="speed-value">{{ getRate(row.id).toFixed(2) }}x</span>
                      </div>
                      <el-slider
                        :model-value="getRate(row.id)"
                        :min="0.01"
                        :max="5"
                        :step="0.1"
                        :show-tooltip="false"
                        @input="(val: number) => setRate(row.id, val)"
                      />
                      <div class="speed-quick">
                        <el-button size="small" @click="adjustRate(row.id, -0.1)">-0.1</el-button>
                        <el-button size="small" @click="setRate(row.id, 1)">重置</el-button>
                        <el-button size="small" @click="adjustRate(row.id, 0.1)">+0.1</el-button>
                      </div>
                    </div>
                  </el-popover>
                  <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="rtPage"
            v-model:page-size="rtPageSize"
            :page-sizes="[10, 20, 50, 100, 200]"
            :total="rtTotal"
            background
            layout="total, sizes, prev, pager, next"
            @size-change="handleRtPageChange"
            @current-change="handleRtPageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 播放进度条（页面级，文件/实时共用） -->
    <div v-if="playingRow" class="player-bar">
      <el-icon class="player-icon"><VideoPlay /></el-icon>
      <span class="player-name" :title="playingRow.name">{{ playingRow.name }}</span>
      <el-slider
        :model-value="progressPercent"
        :min="0"
        :max="100"
        :step="0.1"
        :show-tooltip="false"
        class="player-slider"
        @input="onSeek"
      />
      <span class="player-time">{{ formatTime(playCurrentTime) }} / {{ formatTime(playTotalDuration) }}</span>
      <el-button link type="info">{{ getRate(playingRow.id).toFixed(2) }}x</el-button>
      <el-button link type="danger" @click="stopPlayback">停止</el-button>
    </div>

    <!-- 识别全文查看 -->
    <el-dialog v-model="viewDialogVisible" :title="viewTitle" width="720px" top="8vh">
      <div class="fulltext-area">{{ viewTextContent }}</div>
      <template #footer>
        <span class="char-count">{{ viewTextContent.length }} 字</span>
        <el-button type="primary" @click="copyToClipboard(viewTextContent)">复制全文</el-button>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  uploadAudios,
  getAsrRecords,
  recognizeAsrRecord,
  recognizeAsrBatch,
  deleteAsrRecord,
  deleteAsrRecords
} from '@/api'
import type { AsrRecord } from '@/types'

const activeTab = ref('file')

// ==================== 文件识别 ====================
const records = ref<AsrRecord[]>([])
const realtimeRecords = ref<AsrRecord[]>([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const statusFilter = ref('')
const searchKeyword = ref('')
const asrLang = ref<'cn' | 'en'>('cn')
const langOptions = [
  { label: '中文', value: 'cn' },
  { label: '英文', value: 'en' }
]
const selectedFileIds = ref<string[]>([])
const fileTableRef = ref()
let pollTimer: number | null = null

// 实时识别分页与批量删除
const rtPage = ref(1)
const rtPageSize = ref(20)
const rtTotal = ref(0)
const rtSelectedIds = ref<string[]>([])
const rtTableRef = ref()

const hasRecognizing = computed(() => records.value.some(r => r.status === 'RECOGNIZING'))

async function loadRecords() {
  try {
    // 仅查询文件来源记录，支持状态筛选与内容模糊搜索
    const res = await getAsrRecords(
      page.value, pageSize.value, 'FILE',
      statusFilter.value || undefined, searchKeyword.value || undefined
    )
    if (res.code === 200) {
      records.value = res.data.records
      total.value = res.data.total
    }
  } catch (e) {
    console.error('加载识别记录失败:', e)
  }
}

async function loadRealtimeRecords() {
  try {
    const res = await getAsrRecords(rtPage.value, rtPageSize.value, 'REALTIME')
    if (res.code === 200) {
      realtimeRecords.value = res.data.records
      rtTotal.value = res.data.total
    }
  } catch (e) {
    console.error('加载实时识别历史失败:', e)
  }
}

function handleFilterChange() {
  page.value = 1
  loadRecords()
}

function handleRtPageChange() {
  loadRealtimeRecords()
}

async function handleRtBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${rtSelectedIds.value.length} 条实时记录吗？`, '提示', { type: 'warning' })
    if (playingId.value && rtSelectedIds.value.includes(playingId.value)) stopPlayback()
    await deleteAsrRecords(rtSelectedIds.value)
    ElMessage.success('批量删除成功')
    rtTableRef.value?.clearSelection()
    rtSelectedIds.value = []
    await loadRealtimeRecords()
  } catch (e) {
    // 用户取消或业务错误由拦截器提示
  }
}

async function handleFilesSelected(files: File[]) {
  if (!files.length) return
  try {
    const res = await uploadAudios(files)
    if (res.code === 200) {
      ElMessage.success(`已上传 ${res.data.length} 个音频文件`)
      page.value = 1
      await loadRecords()
    }
  } catch (e) {
    console.error('上传失败:', e)
  }
}

async function handleRecognize(row: AsrRecord) {
  try {
    await recognizeAsrRecord(row.id, asrLang.value)
    row.status = 'RECOGNIZING'
    startPolling()
  } catch (e) {
    console.error(e)
  }
}

async function handleBatchRecognize() {
  try {
    await recognizeAsrBatch(selectedFileIds.value, asrLang.value)
    ElMessage.success(`已提交 ${selectedFileIds.value.length} 个识别任务`)
    clearFileSelection()
    await loadRecords()
    startPolling()
  } catch (e) {
    console.error(e)
  }
}

function clearFileSelection() {
  fileTableRef.value?.clearSelection()
  selectedFileIds.value = []
}

async function handleDelete(row: AsrRecord) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', { type: 'warning' })
    if (playingId.value === row.id) stopPlayback()
    await deleteAsrRecord(row.id)
    await Promise.all([loadRecords(), loadRealtimeRecords()])
  } catch (e) {
    // 用户取消或业务错误由拦截器提示
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedFileIds.value.length} 条记录吗？`, '提示', { type: 'warning' })
    if (playingId.value && selectedFileIds.value.includes(playingId.value)) stopPlayback()
    await deleteAsrRecords(selectedFileIds.value)
    ElMessage.success('批量删除成功')
    clearFileSelection()
    await loadRecords()
  } catch (e) {
    // 用户取消或业务错误由拦截器提示
  }
}

// 有识别中任务时轮询刷新
function startPolling() {
  stopPolling()
  pollTimer = window.setInterval(async () => {
    await loadRecords()
    if (!hasRecognizing.value) stopPolling()
  }, 2000)
}
function stopPolling() {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function statusTagType(status: string) {
  switch (status) {
    case 'DONE': return 'success'
    case 'RECOGNIZING': return 'warning'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
}
function statusText(row: AsrRecord) {
  switch (row.status) {
    case 'DONE': return '已完成'
    case 'RECOGNIZING': return '识别中'
    case 'FAILED': return '失败'
    default: return '待识别'
  }
}
function formatSize(bytes?: number) {
  if (!bytes) return '-'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

function formatDuration(ms: number) {
  const totalSec = Math.floor(ms / 1000)
  const m = Math.floor(totalSec / 60)
  const s = totalSec % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}

function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text || '').then(
    () => ElMessage.success('已复制'),
    () => ElMessage.error('复制失败')
  )
}

// ==================== 音频播放（语速按记录独立保存） ====================
const playingId = ref<string>('')
const rateMap = ref<Record<string, number>>({})
const playCurrentTime = ref(0)
const playTotalDuration = ref(0)
let audio: HTMLAudioElement | null = null

const playingRow = computed(() =>
  records.value.find(r => r.id === playingId.value)
  || realtimeRecords.value.find(r => r.id === playingId.value)
)
const progressPercent = computed(() => {
  if (!playTotalDuration.value) return 0
  return (playCurrentTime.value / playTotalDuration.value) * 100
})

function getRate(recordId: string): number {
  return rateMap.value[recordId] ?? 1
}

function setRate(recordId: string, rate: number) {
  rateMap.value[recordId] = clampRate(Math.round(rate * 100) / 100)
  if (audio && playingId.value === recordId) {
    audio.playbackRate = rateMap.value[recordId]
  }
}

function adjustRate(recordId: string, delta: number) {
  setRate(recordId, getRate(recordId) + delta)
}

function togglePlay(row: AsrRecord) {
  if (playingId.value === row.id) {
    stopPlayback()
    return
  }
  if (!audio) {
    audio = new Audio()
    audio.ontimeupdate = () => { playCurrentTime.value = audio?.currentTime || 0 }
    audio.onloadedmetadata = () => { playTotalDuration.value = audio?.duration || 0 }
    audio.onended = () => { stopPlayback() }
    audio.onerror = () => {
      stopPlayback()
      ElMessage.error('音频播放失败')
    }
  }
  audio.pause()
  audio.src = `/api/asr/records/${row.id}/audio`
  audio.playbackRate = clampRate(getRate(row.id))
  playCurrentTime.value = 0
  playTotalDuration.value = 0
  audio.play().then(() => {
    playingId.value = row.id
  }).catch(() => {
    ElMessage.error('音频播放失败')
  })
}

function stopPlayback() {
  audio?.pause()
  playingId.value = ''
  playCurrentTime.value = 0
  playTotalDuration.value = 0
}

function onSeek(percent: number) {
  if (audio && playTotalDuration.value > 0) {
    audio.currentTime = (percent / 100) * playTotalDuration.value
  }
}

function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds)) return '00:00'
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function clampRate(rate: number): number {
  // 浏览器最低支持约0.0625，低于此值会被钳制
  return Math.min(5, Math.max(0.01, rate))
}

// ==================== 长文本查看 ====================
const viewDialogVisible = ref(false)
const viewTitle = ref('')
const viewTextContent = ref('')

function openTextView(row: AsrRecord) {
  viewTitle.value = row.name
  viewTextContent.value = row.text || ''
  viewDialogVisible.value = true
}

// ==================== 实时识别 ====================
const realtimeMode = ref<'browser' | 'server'>('browser')
const realtimeRunning = ref(false)
const connecting = ref(false)
const realtimeFinal = ref('')
const realtimePartial = ref('')
const realtimeDuration = ref(0)
const micSilent = ref(false)

let ws: WebSocket | null = null
let audioContext: AudioContext | null = null
let mediaStream: MediaStream | null = null
let processor: ScriptProcessorNode | null = null
let sourceNode: MediaStreamAudioSourceNode | null = null
let durationTimer: number | null = null

function toggleRealtime() {
  realtimeRunning.value ? stopRealtime() : startRealtime()
}

async function startRealtime() {
  connecting.value = true
  realtimeFinal.value = ''
  realtimePartial.value = ''
  realtimeDuration.value = 0
  micSilent.value = false
  streamStartSent = false
  pendingChunks.length = 0

  try {
    if (realtimeMode.value === 'browser') {
      // 安全上下文检查：getUserMedia 仅支持 localhost 或 HTTPS
      if (!navigator.mediaDevices?.getUserMedia) {
        const msg = location.protocol === 'http:' && !['localhost', '127.0.0.1'].includes(location.hostname)
          ? 'HTTP非本机访问无法使用麦克风，请用 localhost 访问或部署HTTPS'
          : '当前环境不支持麦克风采集'
        throw new Error(msg)
      }
      // 先开麦并启动本地采集缓存，连接WS期间的语音不丢失
      mediaStream = await navigator.mediaDevices.getUserMedia({
        audio: { channelCount: 1, echoCancellation: true, noiseSuppression: true }
      })
    }

    ws = createRealtimeSocket()
    await new Promise<void>((resolve, reject) => {
      const sock = ws!
      const timer = setTimeout(() => reject(new Error('WebSocket连接超时')), 8000)
      sock.onopen = () => { clearTimeout(timer); resolve() }
      sock.onerror = () => { clearTimeout(timer); reject(new Error('WebSocket连接失败')) }
    })

    if (mediaStream) setupBrowserCapture()

    ws!.send(JSON.stringify({ action: 'start', mode: realtimeMode.value }))
    // WS按连接内消息序投递：start文本帧先于补发的音频帧到达服务端
    streamStartSent = true

    realtimeRunning.value = true
    durationTimer = window.setInterval(() => { realtimeDuration.value += 1 }, 1000)
  } catch (e: any) {
    let msg = e?.message || '启动实时识别失败'
    if (e?.name === 'NotAllowedError') msg = '麦克风权限被拒绝，请在浏览器地址栏允许麦克风访问'
    else if (e?.name === 'NotFoundError') msg = '未检测到麦克风设备'
    ElMessage.error({ message: msg, duration: 6000 })
    teardownBrowserCapture()
    ws?.close()
    ws = null
  } finally {
    connecting.value = false
  }
}

function stopRealtime() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ action: 'stop' }))
    // 等final消息到达后再关闭（在onmessage final分支处理）
    setTimeout(() => closeWs(), 3000)
  } else {
    closeWs()
  }
  realtimeRunning.value = false
  if (durationTimer !== null) {
    clearInterval(durationTimer)
    durationTimer = null
  }
}

function closeWs() {
  teardownBrowserCapture()
  if (ws) {
    ws.onmessage = null
    try { ws.close() } catch {}
    ws = null
  }
}

function createRealtimeSocket(): WebSocket {
  const proto = location.protocol === 'https:' ? 'wss' : 'ws'
  const sock = new WebSocket(`${proto}://${location.host}/api/asr/ws`)
  sock.binaryType = 'arraybuffer'

  sock.onmessage = (ev) => {
    let msg: any
    try { msg = JSON.parse(ev.data) } catch { return }
    switch (msg.event) {
      case 'started':
        break
      case 'partial':
        realtimePartial.value = msg.text || ''
        break
      case 'result':
        appendFinal(msg.text || '')
        break
      case 'final': {
        // 合并服务端最终结果（包含未推送的尾巴）
        if (msg.text && !realtimeFinal.value.endsWith(msg.text)) {
          appendFinal(msg.text)
        }
        realtimePartial.value = ''
        ElMessage.success('识别完成，已保存到历史记录')
        loadRealtimeRecords()
        break
      }
      case 'error':
        ElMessage.error(msg.message || '识别出错')
        break
    }
  }

  sock.onclose = () => {
    if (realtimeRunning.value) {
      realtimeRunning.value = false
      if (durationTimer !== null) { clearInterval(durationTimer); durationTimer = null }
      teardownBrowserCapture()
    }
  }
  return sock
}

function appendFinal(text: string) {
  realtimeFinal.value = realtimeFinal.value
    ? `${realtimeFinal.value} ${text}`.trim()
    : text
}

/** 浏览器麦克风采集 → 手动下采样16k → Int16 PCM → WS二进制 */
function setupBrowserCapture() {
  // 使用默认采样率（48k/44.1k），避免强制16k导致的兼容性问题，手动下采样
  audioContext = new AudioContext()
  sourceNode = audioContext.createMediaStreamSource(mediaStream!)
  processor = audioContext.createScriptProcessor(4096, 1, 1)
  const targetRate = 16000
  const srcRate = audioContext.sampleRate

  let silentSince = Date.now()
  processor.onaudioprocess = (e) => {
    const input = e.inputBuffer.getChannelData(0)

    // 静音检测：连续5秒峰值接近0提示检查麦克风
    let peak = 0
    for (let i = 0; i < input.length; i++) {
      const v = Math.abs(input[i])
      if (v > peak) peak = v
    }
    if (peak > 0.01) {
      micSilent.value = false
      silentSince = Date.now()
    } else if (Date.now() - silentSince > 5000) {
      micSilent.value = true
    }

    // 线性插值下采样到16k
    const ratio = srcRate / targetRate
    const outLen = Math.floor(input.length / ratio)
    const int16 = new Int16Array(outLen)
    for (let i = 0; i < outLen; i++) {
      const pos = i * ratio
      const idx = Math.floor(pos)
      const frac = pos - idx
      const sample = idx + 1 < input.length
        ? input[idx] * (1 - frac) + input[idx + 1] * frac
        : input[idx]
      const s = Math.max(-1, Math.min(1, sample))
      int16[i] = s < 0 ? s * 0x8000 : s * 0x7fff
    }

    // 连接未就绪时本地缓存，start发出后按序补发，避免开头语音丢失
    if (!ws || ws.readyState !== WebSocket.OPEN || !streamStartSent) {
      if (pendingChunks.length < 300) pendingChunks.push(int16)
      return
    }
    flushPendingChunks()
    ws.send(int16.buffer)
  }

  sourceNode.connect(processor)
  // 经由零增益节点保持处理器运转，不外放声音
  const sink = audioContext.createGain()
  sink.gain.value = 0
  processor.connect(sink)
  sink.connect(audioContext.destination)
}

/** start指令已发出的标记：置位后音频帧才允许上行 */
let streamStartSent = false
/** WS就绪前缓存的音频块（每块4096样本@源率≈85ms，300块上限防内存失控） */
const pendingChunks: Int16Array[] = []

function flushPendingChunks() {
  if (!pendingChunks.length || !ws) return
  for (const chunk of pendingChunks.splice(0)) {
    ws.send(chunk.buffer)
  }
}

function teardownBrowserCapture() {
  try { processor && processor.disconnect() } catch {}
  try { sourceNode && sourceNode.disconnect() } catch {}
  if (audioContext && audioContext.state !== 'closed') {
    audioContext.close().catch(() => {})
  }
  mediaStream?.getTracks().forEach(t => t.stop())
  processor = null
  sourceNode = null
  audioContext = null
  mediaStream = null
  streamStartSent = false
  pendingChunks.length = 0
}

onMounted(() => {
  loadRecords()
  loadRealtimeRecords()
})

onBeforeUnmount(() => {
  stopPolling()
  if (realtimeRunning.value) stopRealtime()
  else closeWs()
  stopPlayback()
  audio = null
})
</script>

<style scoped lang="scss">
.asr-page {
  background: #fff;
  border-radius: 8px;
  padding: 16px 24px;
  height: 100%;
  overflow-y: auto;
}

.upload-area {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;

  .lang-segmented {
    --el-border-radius-base: 14px;
    flex-shrink: 0;
  }

  .recognizing-tip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #e6a23c;
    font-size: 13px;
  }
}

.record-text {
  white-space: pre-wrap;
  word-break: break-all;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;

  &:hover {
    color: #409eff;
  }
}

.fulltext-area {
  max-height: 60vh;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 14px;
  line-height: 1.8;
  padding: 4px 8px;
  background: #fafafa;
  border-radius: 6px;
}

.char-count {
  float: left;
  color: #909399;
  font-size: 13px;
  line-height: 32px;
}

.player-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  margin-bottom: 12px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 8px;

  .player-icon {
    color: #409eff;
    flex-shrink: 0;
  }

  .player-name {
    max-width: 220px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    color: #303133;
    flex-shrink: 0;
  }

  .player-slider {
    flex: 1;
  }

  .player-time {
    font-size: 13px;
    font-family: monospace;
    color: #606266;
    flex-shrink: 0;
  }
}

.muted {
  color: #c0c4cc;
}

.error-text {
  color: #f56c6c;
  font-size: 13px;
}

.realtime-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.realtime-controls {
  display: flex;
  align-items: center;
  gap: 16px;

  .duration-tip {
    color: #909399;
    font-size: 13px;
  }
}

.transcript-area {
  min-height: 160px;
  max-height: 320px;
  overflow-y: auto;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafafa;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;

  .empty-hint {
    color: #c0c4cc;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 128px;
  }

  .partial-text {
    color: #409eff;
  }

  .cursor-blink {
    animation: blink 1s step-start infinite;
    color: #409eff;
  }
}

@keyframes blink {
  50% { opacity: 0; }
}

.realtime-footer {
  display: flex;
  justify-content: flex-end;
}

.history-title {
  font-weight: 600;
  margin-bottom: -8px;
}

.action-btns {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;

  :deep(.el-button.is-link) {
    padding: 0;
    height: auto;
  }

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}

.speed-control {
  .speed-label {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-size: 13px;
    color: #606266;

    .speed-value {
      font-weight: 600;
      color: #409eff;
    }
  }

  .speed-quick {
    display: flex;
    justify-content: center;
    gap: 8px;
    margin-top: 12px;

    .el-button + .el-button {
      margin-left: 0;
    }
  }
}
</style>

<template>
  <div class="tts-toolbar">
    <div class="toolbar-left">
      <el-button-group>
        <el-button v-if="!isPlaying" type="primary" size="small" @click="$emit('play')">
          <el-icon><VideoPlay /></el-icon>播放
        </el-button>
        <el-button v-else type="warning" size="small" @click="$emit('pause')">
          <el-icon><VideoPause /></el-icon>暂停
        </el-button>
        <el-button size="small" @click="$emit('stop')" :disabled="status === 'idle'">
          <el-icon><VideoDelete /></el-icon>停止
        </el-button>
      </el-button-group>
    </div>

    <div class="toolbar-right">
      <span class="rate-label">语速</span>
      <el-slider
        :model-value="playbackRate"
        :min="0.5"
        :max="3.0"
        :step="0.1"
        :show-tooltip="false"
        style="width: 120px;"
        @input="(val: number) => $emit('setRate', val)"
      />
      <span class="rate-value">{{ playbackRate.toFixed(1) }}x</span>

      <el-button text size="small" @click="$emit('clearCache')" style="margin-left: 12px;">
        <el-icon><Delete /></el-icon>清缓存
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TtsStatus } from '@/composables/useTTS'

defineProps<{
  status: TtsStatus
  isPlaying: boolean
  playbackRate: number
}>()

defineEmits<{
  play: []
  pause: []
  stop: []
  setRate: [rate: number]
  clearCache: []
}>()
</script>

<style scoped>
.tts-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  gap: 8px;
}
.toolbar-left, .toolbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}
.line-info {
  font-size: 13px;
  color: #606266;
  margin-left: 8px;
}
.rate-label {
  font-size: 13px;
  color: #606266;
}
.rate-value {
  font-size: 13px;
  color: #409eff;
  width: 36px;
}
</style>

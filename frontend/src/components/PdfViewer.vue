<template>
  <div class="pdf-viewer" ref="containerRef">
    <iframe v-if="url" :src="iframeSrc" class="pdf-iframe" ref="iframeRef" />
    <div v-else class="pdf-placeholder">选择文档后显示PDF</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const props = defineProps<{
  url: string
  pageNumber: number
}>()

const containerRef = ref<HTMLElement>()
const iframeRef = ref<HTMLIFrameElement>()
const displayPage = ref(props.pageNumber)

const iframeSrc = computed(() => {
  if (!props.url) return ''
  return props.pageNumber > 1 ? `${props.url}#page=${props.pageNumber}` : props.url
})

let pageTimer: ReturnType<typeof setTimeout> | null = null

watch(() => props.pageNumber, (page) => {
  if (page > 0 && page !== displayPage.value) {
    displayPage.value = page
    if (pageTimer) clearTimeout(pageTimer)
    pageTimer = setTimeout(() => {
      if (iframeRef.value) {
        iframeRef.value.src = `${props.url}#page=${page}`
      }
    }, 300)
  }
})

defineExpose({ scrollToPage: (page: number) => {
  if (iframeRef.value) {
    iframeRef.value.src = `${props.url}#page=${page}`
  }
}})
</script>

<style scoped>
.pdf-viewer {
  width: 100%;
  height: 100%;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}
.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
}
.pdf-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #c0c4cc;
}
</style>

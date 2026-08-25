import { ref, computed, nextTick } from 'vue'

export type TtsStatus = 'idle' | 'playing' | 'paused'
export type TtsEngine = 'edge'

export function useTTS() {
  const status = ref<TtsStatus>('idle')
  const currentLineIndex = ref(0)
  const currentCharIndex = ref(-1)
  const playbackRate = ref(1.0)
  const engine = ref<TtsEngine>('edge')
  const lines = ref<string[]>([])

  const audioCache = new Map<string, Blob>()
  let currentAudio: HTMLAudioElement | null = null
  let animFrameId: number | null = null
  let generation = 0
  let playing = false

  // 并发控制：最多同时发起 MAX_CONCURRENT 个请求
  const MAX_CONCURRENT = 5
  let activeCount = 0
  const waitQueue: Array<() => void> = []

  function acquireSlot(): Promise<void> {
    if (activeCount < MAX_CONCURRENT) {
      activeCount++
      return Promise.resolve()
    }
    return new Promise(resolve => waitQueue.push(resolve))
  }

  function releaseSlot() {
    activeCount--
    if (waitQueue.length > 0) {
      activeCount++
      waitQueue.shift()!()
    }
  }

  const isPlaying = computed(() => status.value === 'playing')
  const isPaused = computed(() => status.value === 'paused')
  const progress = computed(() => {
    if (lines.value.length === 0) return 0
    return ((currentLineIndex.value + 1) / lines.value.length) * 100
  })

  function setLines(newLines: string[]) {
    stop()
    lines.value = newLines
    currentLineIndex.value = 0
    currentCharIndex.value = -1
  }

  function updateLines(newLines: string[], keepPosition = false) {
    const savedIndex = keepPosition ? currentLineIndex.value : 0
    lines.value = newLines
    currentLineIndex.value = Math.min(savedIndex, newLines.length - 1)
    currentCharIndex.value = -1
  }

  function audioKey(text: string) {
    return `${text}:${engine.value}`
  }

  async function fetchAudio(text: string): Promise<Blob> {
    const key = audioKey(text)
    if (audioCache.has(key)) return audioCache.get(key)!

    await acquireSlot()
    try {
      const res = await fetch('/api/tts/speak', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text, engine: engine.value })
      })
      if (!res.ok) throw new Error(`TTS request failed: ${res.status}`)
      const blob = await res.blob()
      audioCache.set(key, blob)
      return blob
    } finally {
      releaseSlot()
    }
  }

  function prefetchUpcoming(currentIdx: number) {
    const prefetchCount = Math.min(5, lines.value.length - currentIdx - 1)
    for (let i = 1; i <= prefetchCount; i++) {
      const idx = currentIdx + i
      if (idx < lines.value.length) {
        const text = lines.value[idx].trim()
        if (text && !audioCache.has(audioKey(text))) {
          fetchAudio(text).catch(() => {})
        }
      }
    }
  }

  function startCharTracking(text: string, audio: HTMLAudioElement) {
    stopCharTracking()
    const totalChars = text.length
    const update = () => {
      if (!audio || audio.paused) return
      const ratio = audio.currentTime / audio.duration
      currentCharIndex.value = Math.min(Math.floor(ratio * totalChars), totalChars - 1)
      animFrameId = requestAnimationFrame(update)
    }
    animFrameId = requestAnimationFrame(update)
  }

  function stopCharTracking() {
    if (animFrameId !== null) {
      cancelAnimationFrame(animFrameId)
      animFrameId = null
    }
    currentCharIndex.value = -1
  }

  function playOneLine(index: number, gen: number): Promise<void> {
    if (gen !== generation || !playing) return Promise.resolve()
    if (index < 0 || index >= lines.value.length) {
      status.value = 'idle'
      playing = false
      return Promise.resolve()
    }

    const text = lines.value[index].trim()
    if (!text) {
      currentLineIndex.value = index + 1
      return playOneLine(index + 1, gen)
    }

    currentLineIndex.value = index
    currentCharIndex.value = -1
    prefetchUpcoming(index)

    return fetchAudio(text).then((blob): Promise<void> => {
      if (gen !== generation || !playing) return Promise.resolve()

      return new Promise<void>((resolve) => {
        if (gen !== generation || !playing) { resolve(); return }

        const url = URL.createObjectURL(blob)
        const audio = new Audio(url)
        audio.playbackRate = playbackRate.value
        currentAudio = audio
        let done = false

        const finish = () => {
          if (done) return
          done = true
          stopCharTracking()
          URL.revokeObjectURL(url)
          if (currentAudio === audio) currentAudio = null
        }

        audio.onplay = () => {
          if (gen !== generation) return
          startCharTracking(text, audio)
        }

        audio.onended = () => {
          finish()
          if (gen !== generation || !playing) { resolve(); return }
          playOneLine(index + 1, gen).then(resolve)
        }

        audio.onerror = () => {
          finish()
          if (gen !== generation || !playing) { resolve(); return }
          playOneLine(index + 1, gen).then(resolve)
        }

        audio.play().catch(() => {
          finish()
          resolve()
        })
      })
    }).catch(() => {
      if (gen !== generation || !playing) return Promise.resolve()
      return playOneLine(index + 1, gen)
    })
  }

  function play() {
    if (status.value === 'paused' && currentAudio) {
      status.value = 'playing'
      playing = true
      currentAudio.playbackRate = playbackRate.value
      currentAudio.play()
      return
    }
    stop()
    status.value = 'playing'
    playing = true
    const gen = ++generation
    playOneLine(currentLineIndex.value, gen)
  }

  function pause() {
    if (status.value === 'playing' && currentAudio) {
      status.value = 'paused'
      playing = false
      currentAudio.pause()
      stopCharTracking()
    }
  }

  function stop() {
    generation++
    playing = false
    status.value = 'idle'
    if (currentAudio) {
      currentAudio.pause()
      currentAudio.currentTime = 0
      currentAudio = null
    }
    stopCharTracking()
  }

  function jumpToLine(index: number) {
    const wasPlaying = status.value === 'playing'
    stop()
    currentLineIndex.value = index
    currentCharIndex.value = -1
    if (wasPlaying) {
      nextTick(() => play())
    }
  }

  function nextLine() {
    if (currentLineIndex.value < lines.value.length - 1) {
      jumpToLine(currentLineIndex.value + 1)
    }
  }

  function prevLine() {
    if (currentLineIndex.value > 0) {
      jumpToLine(currentLineIndex.value - 1)
    }
  }

  function setEngine(e: TtsEngine) {
    if (engine.value !== e) {
      stop()
      engine.value = e
      audioCache.clear()
    }
  }

  function setRate(rate: number) {
    playbackRate.value = rate
    if (currentAudio) {
      currentAudio.playbackRate = rate
    }
  }

  function clearCache() {
    audioCache.clear()
    fetch('/api/tts/cache', { method: 'DELETE' }).catch(() => {})
  }

  return {
    status,
    currentLineIndex,
    currentCharIndex,
    playbackRate,
    engine,
    lines,
    isPlaying,
    isPaused,
    progress,
    setLines,
    updateLines,
    play,
    pause,
    stop,
    jumpToLine,
    nextLine,
    prevLine,
    setEngine,
    setRate,
    clearCache
  }
}

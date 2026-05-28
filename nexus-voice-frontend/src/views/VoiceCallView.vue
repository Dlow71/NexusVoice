<template>
  <div class="voice-call-page">
    <div class="voice-call-glow voice-call-glow-left"></div>
    <div class="voice-call-glow voice-call-glow-right"></div>

    <header class="voice-topbar glass-panel">
      <button class="topbar-chip topbar-chip-back" @click="goBack">
        返回聊天
      </button>

      <div class="topbar-meta">
        <div class="meta-label">语音通话</div>
        <div class="meta-title">
          {{ sessionActive ? '实时语音会话已建立' : '准备开启语音模式' }}
        </div>
      </div>

      <div class="topbar-actions">
        <button class="topbar-chip" @click="drawerVisible = true">
          高级设置
        </button>
        <div
          class="context-ring-shell"
          :title="contextTooltip"
          :style="contextRingStyle"
        >
          <div class="context-ring-core">
            {{ contextPercent }}%
          </div>
        </div>
      </div>
    </header>

    <main class="voice-layout">
      <section class="voice-stage glass-panel">
        <div class="voice-stage-head">
          <div class="stage-status-cluster">
            <span class="status-dot" :class="statusDotClass"></span>
            <span class="status-text">{{ stateLabel }}</span>
          </div>
          <div class="stage-session" v-if="voiceSessionId">
            会话 {{ shortSessionId }}
          </div>
        </div>

        <div class="voice-avatar-stage" :class="{ speaking: isListening, responding: isResponding }">
          <div class="avatar-orbit avatar-orbit-outer"></div>
          <div class="avatar-orbit avatar-orbit-inner"></div>
          <div class="avatar-core">
            <div class="avatar-core-label">AI</div>
            <div class="avatar-core-name">{{ currentModelLabel }}</div>
          </div>
        </div>

        <div class="voice-live-transcript">
          <div class="transcript-label">{{ transcriptCaptionLabel }}</div>
          <div v-if="transcriptSourceLabel" class="transcript-source">
            {{ transcriptSourceLabel }}
          </div>
          <div class="transcript-card">
            <div v-if="userPartialTranscript" class="transcript-partial">
              {{ userPartialTranscript }}
            </div>
            <div v-else-if="latestUserTranscript" class="transcript-final">
              {{ latestUserTranscript }}
            </div>
            <div v-else class="transcript-empty">
              {{ sessionActive ? '点击麦克风开始说话，或使用下方文本回路调试。' : '先建立语音会话。' }}
            </div>
          </div>
        </div>

        <div class="voice-controls">
          <button
            v-if="!sessionActive"
            class="control-button primary"
            :disabled="startingSession"
            @click="handleStartSession"
          >
            {{ startingSession ? startSessionLabel : '开始语音通话' }}
          </button>

          <template v-else>
            <button
              class="control-button"
              :class="{ active: isListening }"
              :disabled="!speechSupported || !realtimeConnected"
              @click="toggleListening"
            >
              {{ isListening ? '停止浏览器识别' : '浏览器识别' }}
            </button>

            <button
              class="control-button"
              :class="{ active: isRecordingAudio }"
              :disabled="!realtimeConnected || !recordingSupported || !asrAvailable"
              @click="toggleAudioRecording"
            >
              {{ isRecordingAudio ? `结束录音 ${recordingDurationLabel}` : '录音识别' }}
            </button>

            <button
              class="control-button"
              :class="{ active: audioUnlocked }"
              :disabled="audioUnlocking"
              @click="handleAudioUnlock"
            >
              {{ audioUnlocking ? '启用中...' : (audioUnlocked ? '语音已启用' : '启用语音输出') }}
            </button>

            <button
              class="control-button"
              :disabled="!realtimeConnected || !asrAvailable"
              @click="triggerAudioUpload"
            >
              上传音频
            </button>

            <button
              class="control-button warn"
              :disabled="!realtimeConnected"
              @click="handleInterrupt"
            >
              打断
            </button>

            <button class="control-button danger" @click="handleEndSession">
              挂断
            </button>
          </template>
        </div>

        <div class="voice-hint-row">
          <span class="hint-chip" :class="{ active: runtimeConfig.strictMode }">严格模式</span>
          <span class="hint-chip" :class="{ active: runtimeConfig.ragEnabled }">RAG</span>
          <span class="hint-chip" :class="{ active: runtimeConfig.policy.showThinking }">思考展示</span>
          <span class="hint-chip">{{ runtimeConfig.policy.contextStrategy || 'COMPACT' }}</span>
          <span class="hint-chip" :class="{ active: asrAvailable, warning: !asrAvailable }">
            {{ asrAvailable ? 'ASR 可用' : 'ASR 不可用' }}
          </span>
          <button
            class="hint-chip interactive-chip"
            :class="{ active: handsFreeMode }"
            @click="toggleHandsFreeMode"
          >
            {{ handsFreeMode ? '连续对话开' : '连续对话关' }}
          </button>
        </div>
      </section>

      <section class="voice-inspector">
        <article class="glass-panel inspector-card">
          <div class="inspector-head">
            <div>
              <div class="inspector-label">AI 回复</div>
              <div class="inspector-title">屏幕展示内容</div>
            </div>
            <div class="inspector-head-actions">
              <div class="inspector-badge" v-if="assistantAudioQueue.length">
                音频段 {{ assistantAudioQueue.length }}
              </div>
              <div
                v-if="audioStatusLabel"
                class="audio-status-badge"
                :class="audioStatusClass"
                :title="audioStatusTitle"
              >
                {{ audioStatusLabel }}
              </div>
              <button
                v-if="showAudioUnlockButton"
                class="collapse-chip"
                :disabled="audioUnlocking"
                @click="unlockAndReplayAudio"
              >
                {{ audioUnlocking ? '启用中...' : playbackBlocked ? '启用声音并重播' : '启用声音' }}
              </button>
              <button
                v-if="latestAudioSegment"
                class="collapse-chip"
                @click="replayCurrentAudio"
              >
                {{ playbackBlocked ? '重播音频' : '重播当前语音' }}
              </button>
            </div>
          </div>

          <div class="assistant-markdown" v-html="assistantDisplayHtml"></div>
        </article>

        <article class="glass-panel inspector-card">
          <div class="inspector-head">
            <div>
              <div class="inspector-label">来源依据</div>
              <div class="inspector-title">知识库命中来源</div>
            </div>
          </div>

          <div v-if="citations.length" class="citation-list">
            <div v-for="citation in citations" :key="citation.id || citation.label" class="citation-card">
              <div class="citation-title">
                {{ citation.knowledgeBaseName || citation.label || '来源' }}
              </div>
              <div class="citation-file">
                {{ citation.fileName || '未命名文档' }}
                <span v-if="citation.location"> · {{ citation.location }}</span>
              </div>
              <div
                class="citation-snippet"
                v-html="renderMarkdown(citation.snippet || '暂无摘录')"
              ></div>
            </div>
          </div>
          <div v-else class="empty-copy">当前轮次暂无结构化来源。</div>
        </article>

        <article class="glass-panel inspector-card">
          <div class="inspector-head">
            <div>
              <div class="inspector-label">深度思考</div>
              <div class="inspector-title">可展开的思考过程</div>
            </div>
            <button class="collapse-chip" @click="thinkingExpanded = !thinkingExpanded">
              {{ thinkingExpanded ? '收起' : '展开' }}
            </button>
          </div>

          <div v-if="thinkingExpanded" class="thinking-body">
            <pre v-if="reasoningContent" class="thinking-content">{{ reasoningContent }}</pre>
            <div v-else class="empty-copy">当前轮次没有返回思考内容。</div>
          </div>
        </article>
      </section>
    </main>

    <section class="voice-debug-composer glass-panel">
      <div class="composer-head">
        <div>
          <div class="inspector-label">文本回路</div>
          <div class="inspector-title">用于浏览器语音识别不可用时的调试入口</div>
        </div>
        <div class="composer-state">
          {{ realtimeConnected ? '实时通道已连接' : '实时通道未连接' }}
        </div>
      </div>

      <div class="composer-row">
        <textarea
          v-model="manualTranscript"
          class="composer-input"
          placeholder="输入一段文本来模拟一轮语音转写，例如：请根据知识库总结这份笔试题启发。"
        ></textarea>
        <button
          class="control-button primary composer-send"
          :disabled="!sessionActive || !realtimeConnected || !manualTranscript.trim() || sendingUtterance"
          @click="submitManualTranscript"
        >
          {{ sendingUtterance ? '处理中...' : '发送一轮' }}
        </button>
      </div>

      <input
        ref="audioFileInput"
        class="hidden-audio-input"
        type="file"
        accept="audio/*"
        @change="handleAudioFileSelected"
      />
      <audio ref="assistantAudioElement" class="hidden-audio-player" preload="auto" playsinline></audio>
    </section>

    <el-drawer
      v-model="drawerVisible"
      direction="rtl"
      size="420px"
      :with-header="false"
      class="voice-advanced-drawer"
    >
      <div class="voice-drawer">
        <div class="drawer-header">
          <div class="inspector-label">高级设置</div>
          <div class="drawer-title">语音模式运行策略</div>
        </div>

        <div class="drawer-body">
          <section class="drawer-section">
            <div class="drawer-section-title">响应约束</div>
            <div class="toggle-row">
              <button
                class="toggle-pill"
                :class="{ active: runtimeConfig.strictMode }"
                @click="runtimeConfig.strictMode = !runtimeConfig.strictMode"
              >
                严格模式
              </button>
              <button
                class="toggle-pill"
                :class="{ active: runtimeConfig.ragEnabled }"
                @click="runtimeConfig.ragEnabled = !runtimeConfig.ragEnabled"
              >
                启用 RAG
              </button>
              <button
                class="toggle-pill"
                :class="{ active: runtimeConfig.policy.showThinking }"
                @click="runtimeConfig.policy.showThinking = !runtimeConfig.policy.showThinking"
              >
                展示思考
              </button>
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">思考模式</div>
            <div class="chip-group">
              <button
                v-for="mode in thinkingModes"
                :key="mode.value"
                class="chip-choice"
                :class="{ active: runtimeConfig.policy.thinkingMode === mode.value }"
                @click="runtimeConfig.policy.thinkingMode = mode.value"
              >
                {{ mode.label }}
              </button>
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">上下文策略</div>
            <div class="chip-group">
              <button
                v-for="strategy in contextStrategies"
                :key="strategy.value"
                class="chip-choice"
                :class="{ active: runtimeConfig.policy.contextStrategy === strategy.value }"
                @click="runtimeConfig.policy.contextStrategy = strategy.value"
              >
                {{ strategy.label }}
              </button>
            </div>
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">温度</div>
            <el-slider
              v-model="runtimeConfig.policy.temperature"
              :min="0"
              :max="1.5"
              :step="0.05"
              show-input
            />
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">语音类型</div>
            <input
              v-model="runtimeConfig.voiceType"
              class="drawer-text-input"
              placeholder="例如 qiniu_zh_female_wwxkjx"
            />
          </section>

          <section class="drawer-section">
            <div class="drawer-section-title">ASR 模型</div>
            <div v-if="asrModels.length" class="chip-group">
              <button
                v-for="model in asrModels"
                :key="model"
                class="chip-choice"
                :class="{ active: selectedAsrModel === model }"
                @click="selectedAsrModel = model"
              >
                {{ model }}
              </button>
            </div>
            <div v-else class="empty-copy">暂无可用 ASR 模型。</div>
          </section>
        </div>

        <div class="drawer-footer">
          <button
            class="control-button primary drawer-save"
            :disabled="!sessionActive || savingConfig"
            @click="handleSaveConfig"
          >
            {{ savingConfig ? '保存中...' : '保存设置' }}
          </button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElDrawer, ElMessage } from 'element-plus'
import DOMPurify from 'dompurify'
import { marked } from 'marked'
import { useAuthStore } from '../stores/auth'
import { useAudioRecorder, RecordingState } from '../composables/useAudioRecorder'
import asrService from '../services/asr'
import {
  endVoiceSession,
  interruptVoiceSession,
  startVoiceSession,
  updateVoiceRuntimeConfig
} from '../services/voice'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const speechSupported = ref(false)
const sessionActive = ref(false)
const realtimeConnected = ref(false)
const startingSession = ref(false)
const savingConfig = ref(false)
const sendingUtterance = ref(false)
const drawerVisible = ref(false)
const thinkingExpanded = ref(true)
const voiceSessionId = ref('')
const conversationId = ref('')
const sessionState = ref('PREPARING')
const userPartialTranscript = ref('')
const latestUserTranscript = ref('')
const manualTranscript = ref('')
const audioFileInput = ref(null)
const assistantAudioElement = ref(null)
const assistantDisplayText = ref('')
const reasoningContent = ref('')
const citations = ref([])
const assistantAudioQueue = ref([])
const isListening = ref(false)
const asrModels = ref([])
const selectedAsrModel = ref('siliconflow:telespeech-asr')
const asrAvailable = ref(false)
const hasFetchedAsrModels = ref(false)
const streamingAudioUpload = ref(false)
const liveRecordingMeta = ref(null)
const localCaptionActive = ref(false)
const localCaptionDraft = ref('')
const transcriptSource = ref('')
const awaitingAuthoritativeTranscript = ref(false)
const currentAudioInputMode = ref('')
const handsFreeMode = ref(false)
const playbackBlocked = ref(false)
const audioPlaybackState = ref('idle')
const audioBackendState = ref('idle')
const audioStatusMessage = ref('')
const audioUnlocked = ref(false)
const audioUnlocking = ref(false)
const microphonePermissionState = ref('unknown')

let recognition = null
let voiceSocket = null
let currentAudio = null
const playbackQueue = []
let autoListenTimer = null

const SILENT_AUDIO_DATA_URI = 'data:audio/wav;base64,UklGRlQAAABXQVZFZm10IBAAAAABAAEAIlYAAESsAAACABAAZGF0YTAAAAAA'

const runtimeConfig = reactive({
  strictMode: true,
  ragEnabled: false,
  voiceType: '',
  knowledgeBaseIds: [],
  policy: {
    thinkingMode: 'disabled',
    showThinking: false,
    contextStrategy: 'COMPACT',
    temperature: 0.7
  },
  contextSnapshot: {
    estimatedInputTokens: 0,
    modelContextWindow: 0
  }
})

const {
  recordingState,
  recordingDuration,
  startRecording,
  stopRecording,
  isRecordingSupported,
  formatDuration
} = useAudioRecorder({
  maxDuration: 45,
  onRecordComplete: async () => {
    if (recognition && localCaptionActive.value) {
      localCaptionActive.value = false
      try {
        recognition.stop()
      } catch (error) {
        console.error(error)
      }
      if (localCaptionDraft.value) {
        userPartialTranscript.value = localCaptionDraft.value
      }
    }
    if (liveRecordingMeta.value) {
      sendRealtimeEvent({
        type: 'AUDIO_END',
        voiceSessionId: voiceSessionId.value,
        ts: Date.now(),
        payload: {
          filename: liveRecordingMeta.value.filename,
          contentType: liveRecordingMeta.value.contentType,
          asrModelKey: selectedAsrModel.value
        }
      })
      liveRecordingMeta.value = null
    }
  },
  onDataChunk: async (audioChunk) => {
    if (!liveRecordingMeta.value || !voiceSessionId.value || !realtimeConnected.value) {
      return
    }
    try {
      await sendAudioChunk(audioChunk, liveRecordingMeta.value)
    } catch (error) {
      console.error(error)
    }
  },
  onError: (error) => {
    ElMessage.error(error.message || '录音失败')
  }
})

const thinkingModes = [
  { value: 'disabled', label: '关闭' },
  { value: 'auto', label: '自动' },
  { value: 'enabled', label: '开启' }
]

const contextStrategies = [
  { value: 'AUTO', label: '自动' },
  { value: 'WINDOW_ONLY', label: '窗口' },
  { value: 'COMPACT', label: '压缩' }
]

const stateLabel = computed(() => {
  const mapping = {
    PREPARING: '准备中',
    READY: '待说话',
    USER_SPEAKING: '用户说话中',
    UNDERSTANDING: '理解中',
    RETRIEVING: '检索中',
    REASONING: '思考中',
    RESPONDING_TEXT: '生成回复中',
    RESPONDING_AUDIO: '语音播报中',
    INTERRUPTING: '打断中',
    DEGRADED: '降级中',
    TERMINATED: '已结束'
  }
  return mapping[sessionState.value] || sessionState.value
})

const statusDotClass = computed(() => {
  if (sessionState.value === 'RESPONDING_AUDIO') return 'responding'
  if (sessionState.value === 'READY') return 'ready'
  if (sessionState.value === 'DEGRADED') return 'degraded'
  return 'processing'
})

const isResponding = computed(() => sessionState.value === 'RESPONDING_AUDIO')
const isRecordingAudio = computed(() => recordingState.value === RecordingState.RECORDING)
const recordingSupported = computed(() => isRecordingSupported())
const recordingDurationLabel = computed(() => formatDuration(recordingDuration.value))
const transcriptCaptionLabel = computed(() => localCaptionActive.value ? '实时字幕（本地辅助）' : '实时字幕')
const transcriptSourceLabel = computed(() => {
  const mapping = {
    server_asr: '来源：后端 ASR',
    browser_live: '来源：浏览器本地识别',
    local_fallback: '来源：本地字幕兜底'
  }
  return mapping[transcriptSource.value] || ''
})

const currentModelLabel = computed(() => runtimeConfig.modelName || 'Voice Agent')

const shortSessionId = computed(() => {
  if (!voiceSessionId.value) return ''
  return `${voiceSessionId.value.slice(0, 8)}...`
})

const contextRatio = computed(() => {
  const used = runtimeConfig.contextSnapshot.estimatedInputTokens || 0
  const total = runtimeConfig.contextSnapshot.modelContextWindow || 0
  if (!total) return 0
  return Math.max(0, Math.min(1, used / total))
})

const contextPercent = computed(() => Math.round(contextRatio.value * 100))

const contextRingStyle = computed(() => ({
  background: `conic-gradient(rgba(14, 165, 233, 0.92) ${contextPercent.value * 3.6}deg, rgba(148, 163, 184, 0.18) 0deg)`
}))

const contextTooltip = computed(() => {
  const used = runtimeConfig.contextSnapshot.estimatedInputTokens || 0
  const total = runtimeConfig.contextSnapshot.modelContextWindow || 0
  const strategy = runtimeConfig.policy.contextStrategy || 'COMPACT'
  return `上下文已用 ${used} / ${total || 0}，策略 ${strategy}`
})

const assistantDisplayHtml = computed(() => renderMarkdown(assistantDisplayText.value || ''))
const latestAudioSegment = computed(() => assistantAudioQueue.value.at(-1) || null)
const audioStatusLabel = computed(() => {
  if (audioPlaybackState.value === 'playing') return '播放中'
  if (audioPlaybackState.value === 'blocked') return '自动播放受限'
  if (audioPlaybackState.value === 'error') return '播放失败'
  if (audioPlaybackState.value === 'completed' && audioBackendState.value === 'ready') return '播放完成'
  if (audioBackendState.value === 'generating') return '语音生成中'
  if (audioBackendState.value === 'failed') return '语音生成失败'
  if (audioBackendState.value === 'ready') return '语音已生成'
  return ''
})
const audioStatusClass = computed(() => {
  return {
    active: audioPlaybackState.value === 'playing'
      || (audioPlaybackState.value === 'completed' && audioBackendState.value === 'ready')
      || audioBackendState.value === 'ready',
    warning: audioPlaybackState.value === 'blocked',
    error: audioPlaybackState.value === 'error' || audioBackendState.value === 'failed'
  }
})
const audioStatusTitle = computed(() => {
  if (audioPlaybackState.value === 'blocked') {
    return '浏览器阻止了自动播放，需要用户手势再次启用声音'
  }
  return audioStatusMessage.value || ''
})
const showAudioUnlockButton = computed(() => {
  if (!sessionActive.value) return false
  return playbackBlocked.value || (!audioUnlocked.value && assistantAudioQueue.value.length > 0)
})
const startSessionLabel = computed(() => {
  if (audioUnlocking.value) return '启用语音输出中...'
  if (startingSession.value && microphonePermissionState.value === 'requesting') return '请求麦克风权限中...'
  return '建立通话中...'
})

const renderMarkdown = (value) => {
  if (!value) return '<p class="empty-copy">等待语音轮次返回内容。</p>'
  const rendered = marked.parse(value, {
    breaks: true,
    gfm: true
  })
  return DOMPurify.sanitize(rendered)
}

const goBack = () => {
  router.back()
}

const syncRuntimeConfig = (payload) => {
  if (!payload) return
  voiceSessionId.value = payload.voiceSessionId || voiceSessionId.value
  conversationId.value = payload.conversationId || conversationId.value
  runtimeConfig.strictMode = payload.strictMode ?? runtimeConfig.strictMode
  runtimeConfig.ragEnabled = payload.ragEnabled ?? runtimeConfig.ragEnabled
  runtimeConfig.voiceType = payload.voiceType || runtimeConfig.voiceType
  if (payload.modelName) {
    runtimeConfig.modelName = payload.modelName
  }
  if (payload.runtimeConfig?.policy) {
    runtimeConfig.policy = {
      ...runtimeConfig.policy,
      ...payload.runtimeConfig.policy
    }
  }
  if (payload.runtimeConfig?.contextSnapshot) {
    runtimeConfig.contextSnapshot = {
      ...runtimeConfig.contextSnapshot,
      ...payload.runtimeConfig.contextSnapshot
    }
  }
}

const connectVoiceSocket = (realtimeUrl) => {
  cleanupSocket()
  voiceSocket = new WebSocket(`${realtimeUrl}?token=${authStore.token}`)

  voiceSocket.onopen = () => {
    realtimeConnected.value = true
    voiceSocket.send(JSON.stringify({
      type: 'SESSION_INIT',
      voiceSessionId: voiceSessionId.value,
      ts: Date.now(),
      payload: {
        conversationId: conversationId.value,
        clientCapabilities: {
          speechRecognition: speechSupported.value,
          audioPlayback: true
        }
      }
    }))
  }

  voiceSocket.onmessage = (event) => {
    const data = JSON.parse(event.data)
    handleRealtimeEvent(data)
  }

  voiceSocket.onerror = () => {
    ElMessage.error('语音实时通道连接失败')
  }

  voiceSocket.onclose = () => {
    realtimeConnected.value = false
  }
}

const handleRealtimeEvent = (event) => {
  switch (event.type) {
    case 'SESSION_READY':
      sessionState.value = event.payload?.state || 'READY'
      sessionActive.value = true
      scheduleAutoListening(420)
      break
    case 'STATE_CHANGED':
      sessionState.value = event.payload?.to || sessionState.value
      if (sessionState.value === 'READY') {
        sendingUtterance.value = false
        if (!currentAudio && playbackQueue.length === 0) {
          scheduleAutoListening(520)
        }
      }
      break
    case 'USER_TRANSCRIPT_PARTIAL':
      userPartialTranscript.value = event.payload?.text || ''
      break
    case 'USER_TRANSCRIPT_FINAL':
      localCaptionActive.value = false
      localCaptionDraft.value = ''
      awaitingAuthoritativeTranscript.value = false
      currentAudioInputMode.value = ''
      transcriptSource.value = 'server_asr'
      playbackBlocked.value = false
      audioPlaybackState.value = 'idle'
      audioBackendState.value = 'generating'
      audioStatusMessage.value = '语音生成中'
      userPartialTranscript.value = ''
      latestUserTranscript.value = event.payload?.text || ''
      assistantDisplayText.value = ''
      reasoningContent.value = ''
      citations.value = []
      assistantAudioQueue.value = []
      playbackQueue.length = 0
      break
    case 'ASSISTANT_AUDIO_GENERATING':
      playbackBlocked.value = false
      audioBackendState.value = 'generating'
      audioStatusMessage.value = event.payload?.message || '语音生成中'
      break
    case 'ASSISTANT_AUDIO_READY':
      audioBackendState.value = 'ready'
      audioStatusMessage.value = event.payload?.message || '语音已生成'
      if (audioPlaybackState.value !== 'playing' && audioPlaybackState.value !== 'completed') {
        audioPlaybackState.value = 'ready'
      }
      break
    case 'ASSISTANT_AUDIO_FAILED':
      playbackBlocked.value = false
      audioBackendState.value = 'failed'
      audioPlaybackState.value = 'error'
      audioStatusMessage.value = event.payload?.message || '语音生成失败'
      break
    case 'THINKING_DELTA':
      reasoningContent.value = `${reasoningContent.value}${event.payload?.delta || ''}`.trim()
      break
    case 'ASSISTANT_TEXT_FINAL':
      assistantDisplayText.value = event.payload?.displayText || ''
      citations.value = event.payload?.citations || []
      break
    case 'ASSISTANT_AUDIO_SEGMENT':
      if (event.payload?.audioUrl) {
        clearAutoListenTimer()
        assistantAudioQueue.value = [...assistantAudioQueue.value, event.payload]
        playbackQueue.push(event.payload)
        playbackBlocked.value = false
        audioBackendState.value = 'ready'
        audioPlaybackState.value = 'ready'
        playNextAudio()
      }
      break
    case 'ASSISTANT_AUDIO_END':
      if (!currentAudio && playbackQueue.length === 0) {
        if (audioBackendState.value === 'ready') {
          audioPlaybackState.value = 'completed'
        }
        scheduleAutoListening(720)
      }
      break
    case 'CONTEXT_STATUS':
      runtimeConfig.contextSnapshot.estimatedInputTokens = event.payload?.estimatedUsedTokens || 0
      runtimeConfig.contextSnapshot.modelContextWindow = event.payload?.windowTokens || 0
      if (event.payload?.strategy) {
        runtimeConfig.policy.contextStrategy = event.payload.strategy
      }
      break
    case 'RUNTIME_CONFIG_UPDATED':
      syncRuntimeConfig(event.payload)
      ElMessage.success('语音设置已更新')
      break
    case 'ERROR':
      if (awaitingAuthoritativeTranscript.value
        && currentAudioInputMode.value === 'recording'
        && localCaptionDraft.value.trim()) {
        const fallbackText = localCaptionDraft.value.trim()
        awaitingAuthoritativeTranscript.value = false
        currentAudioInputMode.value = ''
        transcriptSource.value = 'local_fallback'
        userPartialTranscript.value = ''
        latestUserTranscript.value = fallbackText
        assistantDisplayText.value = ''
        reasoningContent.value = ''
        citations.value = []
        assistantAudioQueue.value = []
        ElMessage.warning('后端 ASR 失败，已回退到本地实时字幕结果')
        sendTextTurn(fallbackText)
        return
      }
      awaitingAuthoritativeTranscript.value = false
      currentAudioInputMode.value = ''
      sendingUtterance.value = false
      ElMessage.error(event.payload?.message || '语音模式发生错误')
      break
    default:
      break
  }
}

const playNextAudio = async (userInitiated = false) => {
  if (currentAudio || playbackQueue.length === 0) return
  if (!audioUnlocked.value && !userInitiated) {
    playbackBlocked.value = true
    audioPlaybackState.value = 'blocked'
    return
  }
  const next = playbackQueue.shift()
  if (!next?.audioUrl) return
  const audioElement = assistantAudioElement.value
  if (!audioElement) {
    playbackBlocked.value = true
    audioPlaybackState.value = 'error'
    audioStatusMessage.value = '音频播放器初始化失败'
    return
  }

  currentAudio = audioElement
  audioElement.pause()
  audioElement.src = next.audioUrl
  audioElement.currentTime = 0
  audioElement.volume = 1
  audioPlaybackState.value = 'playing'
  audioElement.onended = () => {
    currentAudio = null
    if (playbackQueue.length === 0) {
      audioPlaybackState.value = 'completed'
      scheduleAutoListening(720)
    }
    playNextAudio()
  }
  audioElement.onerror = () => {
    currentAudio = null
    playbackBlocked.value = true
    audioPlaybackState.value = 'error'
    audioStatusMessage.value = '音频资源加载或播放失败'
    if (playbackQueue.length === 0) {
      scheduleAutoListening(720)
    }
    playNextAudio()
  }
  try {
    await audioElement.play()
    audioUnlocked.value = true
    playbackBlocked.value = false
  } catch (error) {
    currentAudio = null
    playbackBlocked.value = true
    audioPlaybackState.value = 'blocked'
    audioStatusMessage.value = '浏览器阻止了自动播放，请点击“启用声音并重播”'
    ElMessage.warning('浏览器阻止了自动播放，请先启用声音再重播。')
  }
}

const stopPlayback = ({ clearSegments = false } = {}) => {
  playbackQueue.length = 0
  if (currentAudio) {
    currentAudio.pause()
    currentAudio.currentTime = 0
    currentAudio.onended = null
    currentAudio.onerror = null
    currentAudio = null
  }
  if (assistantAudioElement.value) {
    assistantAudioElement.value.pause()
    assistantAudioElement.value.removeAttribute('src')
    assistantAudioElement.value.load()
    assistantAudioElement.value.onended = null
    assistantAudioElement.value.onerror = null
  }
  if (clearSegments) {
    assistantAudioQueue.value = []
  }
}

const clearAutoListenTimer = () => {
  if (autoListenTimer) {
    window.clearTimeout(autoListenTimer)
    autoListenTimer = null
  }
}

const scheduleAutoListening = (delay = 320) => {
  clearAutoListenTimer()
  if (!handsFreeMode.value || !sessionActive.value || !speechSupported.value || !recognition) {
    return
  }
  if (isListening.value || isRecordingAudio.value || localCaptionActive.value) {
    return
  }
  if (sessionState.value !== 'READY') {
    return
  }
  if (currentAudio || playbackQueue.length > 0) {
    return
  }
  autoListenTimer = window.setTimeout(() => {
    if (!handsFreeMode.value || !sessionActive.value || isListening.value || isRecordingAudio.value || localCaptionActive.value) {
      return
    }
    if (currentAudio || playbackQueue.length > 0) {
      return
    }
    try {
      recognition.start()
    } catch (error) {
      console.error(error)
    }
  }, delay)
}

const sendRealtimeEvent = (payload) => {
  if (!voiceSocket || voiceSocket.readyState !== WebSocket.OPEN) {
    throw new Error('实时语音通道未连接')
  }
  voiceSocket.send(JSON.stringify(payload))
}

const submitManualTranscript = async () => {
  if (!manualTranscript.value.trim()) return
  const text = manualTranscript.value.trim()
  manualTranscript.value = ''
  await sendTextTurn(text)
}

const sendTextTurn = async (text) => {
  sendingUtterance.value = true
  try {
    await interruptCurrentPlayback(true)
    sendRealtimeEvent({
      type: 'TEXT_UTTERANCE',
      voiceSessionId: voiceSessionId.value,
      ts: Date.now(),
      payload: {
        text
      }
    })
  } catch (error) {
    ElMessage.error(error.message || '发送语音轮次失败')
  }
}

const interruptCurrentPlayback = async (silent = false) => {
  const hasPlayingAudio = Boolean(currentAudio) || playbackQueue.length > 0 || sessionState.value === 'RESPONDING_AUDIO'
  if (!hasPlayingAudio || !voiceSessionId.value) {
    return
  }
  try {
    await interruptVoiceSession(voiceSessionId.value)
  } catch (error) {
    if (!silent) {
      ElMessage.warning('自动打断当前播放失败')
    }
  }
  stopPlayback()
  try {
    sendRealtimeEvent({
      type: 'INTERRUPT',
      voiceSessionId: voiceSessionId.value,
      ts: Date.now(),
      payload: { mode: 'hard', reason: 'auto_barge_in' }
    })
  } catch (error) {
    console.error(error)
  }
}

const transcribeAudioAndSend = async (audioFile) => {
  if (!sessionActive.value || !realtimeConnected.value) {
    ElMessage.warning('请先建立语音会话')
    return
  }
  if (!asrAvailable.value) {
    ElMessage.warning('当前 ASR 模型不可用，请改用浏览器识别或文本回路。')
    return
  }

  sendingUtterance.value = true
  try {
    await streamAudioBlob(audioFile, {
      filename: audioFile.name || `voice-upload-${Date.now()}.webm`,
      contentType: audioFile.type || 'application/octet-stream',
      source: 'upload'
    })
  } catch (error) {
    sendingUtterance.value = false
    ElMessage.error(error.message || '语音识别失败')
  }
}

const streamAudioBlob = async (audioBlob, metadata = {}) => {
  if (!voiceSocket || voiceSocket.readyState !== WebSocket.OPEN) {
    throw new Error('实时语音通道未连接')
  }

  streamingAudioUpload.value = true
  currentAudioInputMode.value = metadata.source || 'upload'
  awaitingAuthoritativeTranscript.value = true
  const chunkSize = 48 * 1024
  try {
    for (let offset = 0; offset < audioBlob.size; offset += chunkSize) {
      const chunk = audioBlob.slice(offset, offset + chunkSize)
      const base64Audio = await blobToBase64(chunk)
      sendRealtimeEvent({
        type: 'AUDIO_CHUNK',
        voiceSessionId: voiceSessionId.value,
        ts: Date.now(),
        payload: {
          base64Audio,
          filename: metadata.filename,
          contentType: metadata.contentType
        }
      })
    }

    sendRealtimeEvent({
      type: 'AUDIO_END',
      voiceSessionId: voiceSessionId.value,
      ts: Date.now(),
      payload: {
        filename: metadata.filename,
        contentType: metadata.contentType,
        asrModelKey: selectedAsrModel.value
      }
    })
  } finally {
    streamingAudioUpload.value = false
  }
}

const sendAudioChunk = async (audioBlob, metadata = {}) => {
  const base64Audio = await blobToBase64(audioBlob)
  sendRealtimeEvent({
    type: 'AUDIO_CHUNK',
    voiceSessionId: voiceSessionId.value,
    ts: Date.now(),
    payload: {
      base64Audio,
      filename: metadata.filename,
      contentType: metadata.contentType
    }
  })
}

const blobToBase64 = async (blob) => {
  const buffer = await blob.arrayBuffer()
  const bytes = new Uint8Array(buffer)
  let binary = ''
  const chunkLength = 0x8000
  for (let i = 0; i < bytes.length; i += chunkLength) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunkLength))
  }
  return btoa(binary)
}

const requestMicrophonePermission = async () => {
  if (!navigator.mediaDevices?.getUserMedia) {
    microphonePermissionState.value = 'unsupported'
    return false
  }
  microphonePermissionState.value = 'requesting'
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: {
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    })
    stream.getTracks().forEach(track => track.stop())
    microphonePermissionState.value = 'granted'
    return true
  } catch (error) {
    microphonePermissionState.value = error?.name === 'NotAllowedError' ? 'denied' : 'unavailable'
    return false
  }
}

const handleStartSession = async () => {
  startingSession.value = true
  try {
    const microphoneReady = await requestMicrophonePermission()
    const audioReady = await unlockAudioPlayback(true)
    handsFreeMode.value = Boolean(speechSupported.value && recognition && microphoneReady)

    const response = await startVoiceSession({
      conversationId: route.query.conversationId ? Number(route.query.conversationId) : undefined,
      roleId: route.query.roleId ? Number(route.query.roleId) : undefined,
      modelName: route.query.modelName || undefined,
      strictMode: runtimeConfig.strictMode,
      ragEnabled: runtimeConfig.ragEnabled,
      thinkingMode: runtimeConfig.policy.thinkingMode,
      showThinking: runtimeConfig.policy.showThinking,
      contextStrategy: runtimeConfig.policy.contextStrategy,
      temperature: runtimeConfig.policy.temperature,
      asrModelKey: selectedAsrModel.value
    })

    if (response.code !== 200) {
      throw new Error(response.message || '创建语音会话失败')
    }

    const data = response.data
    voiceSessionId.value = data.voiceSessionId
    conversationId.value = data.conversationId
    sessionState.value = data.state || 'PREPARING'
    syncRuntimeConfig(data.runtimeConfig)
    connectVoiceSocket(data.realtimeUrl)

    if (audioReady && handsFreeMode.value) {
      ElMessage.success('语音通话已就绪，可直接开始说话')
    } else if (audioReady) {
      ElMessage.warning('通话已建立，但麦克风或浏览器语音识别不可用，请使用录音识别或文本回路')
    } else {
      ElMessage.warning('通话已建立，但浏览器尚未放行语音播放，建议先点“启用语音输出”')
    }
  } catch (error) {
    handsFreeMode.value = false
    ElMessage.error(error.message || '启动语音会话失败')
  } finally {
    startingSession.value = false
    if (microphonePermissionState.value === 'requesting') {
      microphonePermissionState.value = 'unknown'
    }
  }
}

const handleSaveConfig = async () => {
  if (!voiceSessionId.value) return
  savingConfig.value = true
  try {
    const response = await updateVoiceRuntimeConfig(voiceSessionId.value, {
      strictMode: runtimeConfig.strictMode,
      ragEnabled: runtimeConfig.ragEnabled,
      voiceType: runtimeConfig.voiceType,
      asrModelKey: selectedAsrModel.value,
      policy: {
        thinkingMode: runtimeConfig.policy.thinkingMode,
        showThinking: runtimeConfig.policy.showThinking,
        contextStrategy: runtimeConfig.policy.contextStrategy,
        temperature: runtimeConfig.policy.temperature
      }
    })

    if (response.code !== 200) {
      throw new Error(response.message || '保存失败')
    }

    syncRuntimeConfig(response.data)
    drawerVisible.value = false
    ElMessage.success('语音设置已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存语音设置失败')
  } finally {
    savingConfig.value = false
  }
}

const handleInterrupt = async () => {
  if (!voiceSessionId.value) return
  try {
    await interruptVoiceSession(voiceSessionId.value)
    if (isRecordingAudio.value) {
      stopRecording()
    }
    stopPlayback()
    sendRealtimeEvent({
      type: 'INTERRUPT',
      voiceSessionId: voiceSessionId.value,
      ts: Date.now(),
      payload: { mode: 'hard' }
    })
  } catch (error) {
    ElMessage.error(error.message || '打断失败')
  }
}

const handleEndSession = async () => {
  if (!voiceSessionId.value) return
  try {
    await endVoiceSession(voiceSessionId.value)
  } catch (error) {
    console.error(error)
  } finally {
    cleanupAll()
  }
}

const cleanupSocket = () => {
  if (voiceSocket) {
    voiceSocket.close(1000, 'cleanup')
    voiceSocket = null
  }
}

const cleanupRecognition = () => {
  if (recognition) {
    recognition.onstart = null
    recognition.onend = null
    recognition.onresult = null
    recognition.onerror = null
    try {
      recognition.stop()
    } catch (error) {
      console.error(error)
    }
    recognition = null
  }
}

const cleanupAll = () => {
  clearAutoListenTimer()
  cleanupSocket()
  cleanupRecognition()
  if (isRecordingAudio.value) {
    stopRecording()
  }
  stopPlayback({ clearSegments: true })
  sessionActive.value = false
  realtimeConnected.value = false
  isListening.value = false
  voiceSessionId.value = ''
  conversationId.value = ''
  sessionState.value = 'TERMINATED'
  localCaptionActive.value = false
  localCaptionDraft.value = ''
  transcriptSource.value = ''
  awaitingAuthoritativeTranscript.value = false
  currentAudioInputMode.value = ''
  playbackBlocked.value = false
  audioPlaybackState.value = 'idle'
  audioBackendState.value = 'idle'
  audioStatusMessage.value = ''
  audioUnlocked.value = false
}

const replayCurrentAudio = async () => {
  if (!assistantAudioQueue.value.length) {
    return
  }
  stopPlayback()
  for (const segment of assistantAudioQueue.value) {
    if (segment?.audioUrl) {
      playbackQueue.push(segment)
    }
  }
  playbackBlocked.value = false
  audioPlaybackState.value = 'ready'
  await playNextAudio(true)
}

const unlockAudioPlayback = async (force = false) => {
  if (audioUnlocked.value && !force) {
    return true
  }
  if (audioUnlocking.value) {
    return false
  }
  audioUnlocking.value = true
  try {
    const audioElement = assistantAudioElement.value
    if (!audioElement) {
      return false
    }
    audioElement.pause()
    audioElement.src = SILENT_AUDIO_DATA_URI
    audioElement.currentTime = 0
    audioElement.volume = 0.01
    await audioElement.play()
    audioElement.pause()
    audioElement.currentTime = 0
    audioElement.removeAttribute('src')
    audioElement.load()
    audioUnlocked.value = true
    playbackBlocked.value = false
    if (audioPlaybackState.value === 'blocked') {
      audioPlaybackState.value = audioBackendState.value === 'ready' ? 'ready' : 'idle'
    }
    return true
  } catch (error) {
    audioUnlocked.value = false
    return false
  } finally {
    audioUnlocking.value = false
  }
}

const handleAudioUnlock = async () => {
  const unlocked = await unlockAudioPlayback(true)
  if (!unlocked) {
    ElMessage.warning('浏览器仍未允许语音输出，请再次点击或检查站点声音权限')
    return
  }
  ElMessage.success('语音输出已启用')
}

const unlockAndReplayAudio = async () => {
  if (assistantAudioQueue.value.length) {
    await replayCurrentAudio()
    return
  }
  const unlocked = await unlockAudioPlayback(true)
  if (!unlocked) {
    ElMessage.warning('浏览器仍未允许声音播放，请再次点击或检查站点音频权限')
    return
  }
  ElMessage.success('声音播放已启用')
}

const toggleHandsFreeMode = () => {
  handsFreeMode.value = !handsFreeMode.value
  if (handsFreeMode.value) {
    scheduleAutoListening(200)
  } else {
    clearAutoListenTimer()
    if (isListening.value && !localCaptionActive.value) {
      try {
        recognition?.stop()
      } catch (error) {
        console.error(error)
      }
    }
  }
}

const startLocalCaption = () => {
  if (!speechSupported.value || !recognition || localCaptionActive.value) {
    return
  }
  localCaptionActive.value = true
  localCaptionDraft.value = ''
  try {
    recognition.start()
  } catch (error) {
    localCaptionActive.value = false
    localCaptionDraft.value = ''
  }
}

const stopLocalCaption = () => {
  if (!recognition) {
    localCaptionActive.value = false
    localCaptionDraft.value = ''
    return
  }
  localCaptionActive.value = false
  localCaptionDraft.value = ''
  try {
    recognition.stop()
  } catch (error) {
    console.error(error)
  }
}

const initializeSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    speechSupported.value = false
    return
  }

  speechSupported.value = true
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = false
  recognition.interimResults = true

  recognition.onstart = () => {
    isListening.value = true
    userPartialTranscript.value = localCaptionActive.value ? '正在记录本地实时字幕...' : '正在聆听...'
  }

  recognition.onresult = (event) => {
    let interim = ''
    let finalText = ''
    for (let i = event.resultIndex; i < event.results.length; i += 1) {
      const transcript = event.results[i][0].transcript
      if (event.results[i].isFinal) {
        finalText += transcript
      } else {
        interim += transcript
      }
    }
    if (localCaptionActive.value) {
      const mergedDraft = [finalText.trim(), interim.trim()].filter(Boolean).join(' ')
      localCaptionDraft.value = mergedDraft
      transcriptSource.value = mergedDraft ? 'browser_live' : transcriptSource.value
      userPartialTranscript.value = mergedDraft || '正在记录本地实时字幕...'
      return
    }

    userPartialTranscript.value = interim.trim()
    if (finalText.trim()) {
      latestUserTranscript.value = finalText.trim()
      sendTextTurn(finalText.trim())
    }
  }

  recognition.onend = () => {
    isListening.value = false
    if (localCaptionActive.value) {
      userPartialTranscript.value = localCaptionDraft.value || '正在处理录音...'
      return
    }
    userPartialTranscript.value = ''
    scheduleAutoListening(520)
  }

  recognition.onerror = (event) => {
    isListening.value = false
    if (localCaptionActive.value) {
      localCaptionActive.value = false
      localCaptionDraft.value = ''
      userPartialTranscript.value = ''
      return
    }
    userPartialTranscript.value = ''
    if (event.error !== 'no-speech') {
      ElMessage.warning(`语音识别不可用：${event.error}`)
    }
  }
}

const toggleListening = () => {
  if (!speechSupported.value || !recognition) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用下方文本回路调试。')
    return
  }
  if (isListening.value) {
    localCaptionActive.value = false
    localCaptionDraft.value = ''
    recognition.stop()
  } else {
    unlockAudioPlayback()
    interruptCurrentPlayback(true)
    recognition.start()
  }
}

const toggleAudioRecording = async () => {
  if (!recordingSupported.value) {
    ElMessage.warning('当前浏览器不支持录音上传，请使用文本回路。')
    return
  }
  if (isRecordingAudio.value) {
    stopLocalCaption()
    stopRecording()
    return
  }
  try {
    await unlockAudioPlayback()
    await interruptCurrentPlayback(true)
    liveRecordingMeta.value = {
      filename: `voice-recording-${Date.now()}.webm`,
      contentType: 'audio/webm'
    }
    startLocalCaption()
    await startRecording()
    ElMessage.success('开始录音，结束后会自动识别并发送')
  } catch (error) {
    liveRecordingMeta.value = null
    stopLocalCaption()
    ElMessage.error(error.message || '无法开始录音')
  }
}

const triggerAudioUpload = () => {
  audioFileInput.value?.click()
}

const handleAudioFileSelected = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  try {
    await interruptCurrentPlayback(true)
    await transcribeAudioAndSend(file)
  } finally {
    event.target.value = ''
  }
}

const fetchAsrModels = async () => {
  if (!authStore.token || hasFetchedAsrModels.value) {
    return
  }
  try {
    const models = await asrService.getAvailableModels()
    hasFetchedAsrModels.value = true
    if (Array.isArray(models) && models.length) {
      asrModels.value = models
      if (models.includes('siliconflow:telespeech-asr')) {
        selectedAsrModel.value = 'siliconflow:telespeech-asr'
      } else {
        selectedAsrModel.value = models[0]
      }
      await refreshAsrHealth()
    }
  } catch (error) {
    console.error('获取ASR模型失败', error)
  }
}

const refreshAsrHealth = async () => {
  if (!selectedAsrModel.value) {
    asrAvailable.value = false
    return
  }
  try {
    const response = await asrService.healthCheck(selectedAsrModel.value)
    asrAvailable.value = Boolean(response?.data)
  } catch (error) {
    asrAvailable.value = false
  }
}

onMounted(() => {
  initializeSpeechRecognition()
  fetchAsrModels()
})

watch(selectedAsrModel, () => {
  refreshAsrHealth()
})

watch(
  () => authStore.token,
  (token) => {
    if (token) {
      fetchAsrModels()
    }
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  cleanupAll()
})
</script>

<style scoped>
.voice-call-page {
  position: relative;
  min-height: 100vh;
  padding: 24px;
  background:
    linear-gradient(rgba(15, 23, 42, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(15, 23, 42, 0.05) 1px, transparent 1px),
    linear-gradient(180deg, #f7fbff 0%, #eef8ff 100%);
  background-size: 48px 48px, 48px 48px, auto;
  overflow: hidden;
}

.voice-call-glow {
  position: absolute;
  width: 420px;
  height: 420px;
  border-radius: 999px;
  filter: blur(90px);
  pointer-events: none;
}

.voice-call-glow-left {
  top: -80px;
  left: -80px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.16) 0%, transparent 70%);
}

.voice-call-glow-right {
  right: -120px;
  bottom: 80px;
  background: radial-gradient(circle, rgba(6, 182, 212, 0.14) 0%, transparent 72%);
}

.glass-panel {
  position: relative;
  border: 1px solid rgba(203, 213, 225, 0.78);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(18px);
}

.voice-topbar {
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 18px;
  border-radius: 24px;
}

.topbar-chip {
  height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(125, 211, 252, 0.52);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  color: #0f172a;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.topbar-chip:hover {
  border-color: rgba(14, 165, 233, 0.62);
  background: rgba(240, 249, 255, 0.96);
}

.topbar-chip-back {
  flex-shrink: 0;
}

.topbar-meta {
  flex: 1;
  min-width: 0;
}

.meta-label,
.inspector-label {
  color: #64748b;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.meta-title,
.inspector-title,
.drawer-title {
  margin-top: 4px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 600;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.context-ring-shell {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 999px;
  padding: 4px;
}

.context-ring-core {
  display: grid;
  place-items: center;
  width: 100%;
  height: 100%;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  color: #0284c7;
  font-size: 10px;
  font-weight: 700;
}

.voice-layout {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(320px, 0.94fr) minmax(320px, 1.06fr);
  gap: 20px;
  margin-top: 20px;
}

.voice-stage,
.inspector-card,
.voice-debug-composer {
  border-radius: 28px;
}

.voice-stage {
  padding: 22px;
}

.voice-stage-head,
.inspector-head,
.composer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.stage-status-cluster {
  display: flex;
  align-items: center;
  gap: 10px;
}

.status-dot {
  width: 11px;
  height: 11px;
  border-radius: 999px;
}

.status-dot.ready {
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.12);
}

.status-dot.responding {
  background: #0ea5e9;
  box-shadow: 0 0 0 6px rgba(14, 165, 233, 0.14);
}

.status-dot.degraded {
  background: #ef4444;
  box-shadow: 0 0 0 6px rgba(239, 68, 68, 0.12);
}

.status-dot.processing {
  background: #f59e0b;
  box-shadow: 0 0 0 6px rgba(245, 158, 11, 0.12);
}

.status-text,
.stage-session,
.composer-state,
.citation-file,
.empty-copy {
  color: #64748b;
  font-size: 13px;
}

.voice-avatar-stage {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 300px;
  margin-top: 20px;
}

.avatar-orbit {
  position: absolute;
  border-radius: 999px;
  border: 1px solid rgba(125, 211, 252, 0.5);
  transition: transform 0.32s ease, opacity 0.32s ease;
}

.avatar-orbit-outer {
  width: 260px;
  height: 260px;
}

.avatar-orbit-inner {
  width: 210px;
  height: 210px;
}

.voice-avatar-stage.speaking .avatar-orbit-outer,
.voice-avatar-stage.responding .avatar-orbit-outer {
  transform: scale(1.08);
}

.voice-avatar-stage.speaking .avatar-orbit-inner,
.voice-avatar-stage.responding .avatar-orbit-inner {
  transform: scale(0.94);
}

.avatar-core {
  display: grid;
  place-items: center;
  width: 160px;
  height: 160px;
  border-radius: 36px;
  background: linear-gradient(180deg, rgba(240, 249, 255, 0.96) 0%, rgba(224, 242, 254, 0.9) 100%);
  border: 1px solid rgba(125, 211, 252, 0.6);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.86), 0 18px 32px rgba(14, 165, 233, 0.12);
}

.avatar-core-label {
  color: #0891b2;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
}

.avatar-core-name {
  width: 108px;
  margin-top: 8px;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.5;
  text-align: center;
}

.voice-live-transcript {
  margin-top: 12px;
}

.transcript-label,
.drawer-section-title,
.citation-title {
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.transcript-source {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.transcript-card {
  min-height: 88px;
  margin-top: 10px;
  padding: 16px;
  border: 1px solid rgba(203, 213, 225, 0.76);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
}

.transcript-partial,
.transcript-final {
  color: #0f172a;
  font-size: 15px;
  line-height: 1.7;
}

.transcript-partial {
  opacity: 0.76;
}

.transcript-empty {
  color: #94a3b8;
  font-size: 14px;
  line-height: 1.7;
}

.voice-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.control-button {
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid rgba(203, 213, 225, 0.82);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.control-button:hover:not(:disabled) {
  transform: translateY(-1px);
  border-color: rgba(14, 165, 233, 0.48);
}

.control-button:disabled {
  opacity: 0.56;
  cursor: not-allowed;
}

.control-button.primary {
  border-color: rgba(14, 165, 233, 0.38);
  background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%);
  color: #f8fafc;
}

.control-button.warn {
  border-color: rgba(245, 158, 11, 0.32);
  background: rgba(255, 251, 235, 0.96);
  color: #b45309;
}

.control-button.danger {
  border-color: rgba(248, 113, 113, 0.34);
  background: rgba(254, 242, 242, 0.96);
  color: #dc2626;
}

.control-button.active {
  border-color: rgba(14, 165, 233, 0.52);
  background: rgba(224, 242, 254, 0.96);
  color: #0284c7;
}

.voice-hint-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hint-chip,
.inspector-badge,
.collapse-chip,
.toggle-pill,
.chip-choice {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid rgba(203, 213, 225, 0.72);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  color: #475569;
  font-size: 13px;
  cursor: pointer;
}

.hint-chip.active,
.toggle-pill.active,
.chip-choice.active {
  border-color: rgba(14, 165, 233, 0.44);
  background: rgba(224, 242, 254, 0.94);
  color: #0369a1;
}

.hint-chip.warning {
  border-color: rgba(245, 158, 11, 0.34);
  background: rgba(255, 251, 235, 0.96);
  color: #b45309;
}

.interactive-chip {
  cursor: pointer;
}

.voice-inspector {
  display: grid;
  gap: 16px;
}

.inspector-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.audio-status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(203, 213, 225, 0.72);
  background: rgba(255, 255, 255, 0.82);
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.audio-status-badge.active {
  border-color: rgba(14, 165, 233, 0.34);
  background: rgba(224, 242, 254, 0.9);
  color: #0369a1;
}

.audio-status-badge.warning {
  border-color: rgba(245, 158, 11, 0.34);
  background: rgba(255, 251, 235, 0.94);
  color: #b45309;
}

.audio-status-badge.error {
  border-color: rgba(248, 113, 113, 0.34);
  background: rgba(254, 242, 242, 0.94);
  color: #b91c1c;
}

.inspector-card {
  padding: 20px;
}

.assistant-markdown,
.citation-snippet,
.thinking-content {
  margin-top: 14px;
  color: #334155;
  font-size: 14px;
  line-height: 1.8;
}

.assistant-markdown :deep(p),
.citation-snippet :deep(p) {
  margin: 0 0 10px;
}

.citation-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.citation-card {
  padding: 14px;
  border: 1px solid rgba(203, 213, 225, 0.72);
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.82);
}

.thinking-body {
  margin-top: 12px;
}

.thinking-content {
  margin: 0;
  white-space: pre-wrap;
  font-family: inherit;
}

.voice-debug-composer {
  position: relative;
  z-index: 1;
  margin-top: 20px;
  padding: 20px;
}

.composer-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  margin-top: 14px;
}

.composer-input,
.drawer-text-input {
  width: 100%;
  border: 1px solid rgba(203, 213, 225, 0.88);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  color: #0f172a;
  font-size: 14px;
  outline: none;
}

.composer-input {
  min-height: 108px;
  padding: 16px;
  resize: vertical;
}

.drawer-text-input {
  min-height: 46px;
  padding: 0 14px;
}

.composer-input:focus,
.drawer-text-input:focus {
  border-color: rgba(14, 165, 233, 0.48);
  box-shadow: 0 0 0 4px rgba(125, 211, 252, 0.16);
}

.composer-send {
  align-self: end;
}

.hidden-audio-input {
  display: none;
}

.hidden-audio-player {
  display: none;
}

:global(.voice-advanced-drawer .el-drawer__body) {
  padding: 0;
  background: linear-gradient(180deg, #f8fcff 0%, #eef8ff 100%);
}

.voice-drawer {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  max-height: 100dvh;
  box-sizing: border-box;
}

.drawer-header {
  padding: 24px 24px 16px;
}

.drawer-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 24px 32px;
}

.drawer-section + .drawer-section {
  margin-top: 18px;
}

.toggle-row,
.chip-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}

.drawer-footer {
  position: sticky;
  bottom: 0;
  padding: 16px 24px calc(20px + env(safe-area-inset-bottom));
  border-top: 1px solid rgba(203, 213, 225, 0.72);
  background: rgba(248, 252, 255, 0.94);
  backdrop-filter: blur(16px);
}

.drawer-save {
  width: 100%;
}

@media (max-width: 1080px) {
  .voice-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .voice-call-page {
    padding: 14px;
  }

  .voice-topbar,
  .voice-stage,
  .inspector-card,
  .voice-debug-composer {
    border-radius: 22px;
  }

  .voice-topbar {
    flex-wrap: wrap;
  }

  .topbar-actions {
    width: 100%;
    justify-content: space-between;
  }

  .composer-row {
    grid-template-columns: 1fr;
  }
}
</style>

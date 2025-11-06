<template>
  <div class="rtc-voice-container">
    <!-- 顶部状态栏 -->
    <div class="status-bar">
      <el-row :gutter="20" align="middle">
        <el-col :span="8">
          <div class="connection-status">
            <el-tag :type="connectionStatusType" size="large">
              <el-icon><Connection /></el-icon>
              {{ connectionStatusText }}
            </el-tag>
          </div>
        </el-col>
        <el-col :span="8" class="text-center">
          <div class="session-info">
            <span class="label">会话ID:</span>
            <span class="value">{{ sessionId || '未创建' }}</span>
          </div>
        </el-col>
        <el-col :span="8" class="text-right">
          <div class="rtc-state">
            <el-tag :type="rtcStateType" size="large">
              {{ rtcStateText }}
            </el-tag>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧：音频可视化 -->
      <div class="audio-visual">
        <div class="visual-card">
          <h3>语音可视化</h3>
          <canvas ref="audioCanvas" width="400" height="200"></canvas>
          
          <!-- 音量指示器 -->
          <div class="volume-indicator">
            <el-progress
              :percentage="audioVolume"
              :color="volumeColor"
              :stroke-width="20"
            />
            <div class="volume-label">音量: {{ audioVolume }}%</div>
          </div>
        </div>

        <!-- 网络统计 -->
        <div class="stats-card">
          <h3>连接统计</h3>
          <div class="stat-item">
            <span class="stat-label">RTT:</span>
            <span class="stat-value">{{ stats.rtt }} ms</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">抖动:</span>
            <span class="stat-value">{{ stats.jitter }} ms</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">丢包:</span>
            <span class="stat-value">{{ stats.packetLoss }}%</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">码率:</span>
            <span class="stat-value">{{ stats.bitrate }} kbps</span>
          </div>
        </div>
      </div>

      <!-- 右侧：对话记录 -->
      <div class="conversation-area">
        <div class="messages-container" ref="messagesContainer">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="['message-item', message.type]"
          >
            <div class="message-avatar">
              <el-icon v-if="message.type === 'user'" size="32"><User /></el-icon>
              <el-icon v-else size="32"><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-text">{{ message.text }}</div>
              <div class="message-meta">{{ message.timestamp }}</div>
            </div>
          </div>

          <!-- 空状态 -->
          <el-empty v-if="messages.length === 0" description="开始你的语音对话吧">
            <el-icon :size="64" color="#409EFF"><Microphone /></el-icon>
          </el-empty>
        </div>

        <!-- 当前识别文本 -->
        <div v-if="currentRecognitionText" class="recognition-text">
          <el-icon><Loading /></el-icon>
          正在识别: {{ currentRecognitionText }}
        </div>
      </div>
    </div>

    <!-- 底部控制栏 -->
    <div class="control-bar">
      <el-row :gutter="20" align="middle" justify="center">
        <!-- 开始/结束按钮 -->
        <el-col :span="6">
          <el-button
            v-if="!isSessionActive"
            type="primary"
            size="large"
            :loading="isConnecting"
            :disabled="!isBrowserSupported"
            @click="startSession"
            round
          >
            <el-icon><VideoPlay /></el-icon>
            开始语音对话
          </el-button>
          <el-button
            v-else
            type="danger"
            size="large"
            @click="endSession"
            round
          >
            <el-icon><SwitchButton /></el-icon>
            结束对话
          </el-button>
        </el-col>

        <!-- 打断按钮 -->
        <el-col :span="6">
          <el-button
            type="warning"
            size="large"
            :disabled="!canInterrupt"
            @click="performInterrupt('SOFT')"
            round
          >
            <el-icon><Remove /></el-icon>
            打断（软）
          </el-button>
        </el-col>

        <!-- 强制打断按钮 -->
        <el-col :span="6">
          <el-button
            type="danger"
            size="large"
            :disabled="!canInterrupt"
            @click="performInterrupt('HARD')"
            plain
            round
          >
            <el-icon><CloseBold /></el-icon>
            打断（硬）
          </el-button>
        </el-col>

        <!-- 设置按钮 -->
        <el-col :span="6">
          <el-button
            size="large"
            @click="showSettings"
            circle
          >
            <el-icon><Setting /></el-icon>
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 设置抽屉 -->
    <el-drawer
      v-model="settingsVisible"
      title="RTC设置"
      direction="rtl"
      size="400px"
    >
      <el-form label-width="100px">
        <el-form-item label="角色">
          <el-select v-model="selectedRoleId" placeholder="选择角色（可选）" clearable>
            <el-option label="无角色" :value="null" />
            <!-- TODO: 动态加载角色列表 -->
          </el-select>
        </el-form-item>

        <el-form-item label="模型">
          <el-select v-model="selectedModel" placeholder="选择模型">
            <el-option label="GPT-4o Mini" value="openai:gpt-4o-mini" />
            <el-option label="GPT-4o" value="openai:gpt-4o" />
            <el-option label="DeepSeek V3" value="deepseek:deepseek-v3.1" />
          </el-select>
        </el-form-item>

        <el-form-item label="STUN服务器">
          <el-input v-model="stunServer" placeholder="stun:stun.l.google.com:19302" />
        </el-form-item>

        <el-form-item label="调试模式">
          <el-switch v-model="debugMode" />
        </el-form-item>
      </el-form>
    </el-drawer>

    <!-- 隐藏的远端音频元素 -->
    <audio ref="remoteAudio" autoplay></audio>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Connection, User, ChatDotRound, Microphone, VideoPlay, 
  SwitchButton, Remove, CloseBold, Setting, Loading 
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { WebRtcManager } from '../utils/WebRtcManager'
import { RtcSignalingClient } from '../utils/RtcSignalingClient'
import { createRtcSession, interruptRtc, endRtcSession as endRtcSessionApi } from '../services/rtc'

// ==================== 响应式状态 ====================

const authStore = useAuthStore()

// RTC会话信息
const sessionId = ref(null)
const rtcState = ref('IDLE') // IDLE/CONNECTING/LISTENING/RECOGNIZING/THINKING/SPEAKING/INTERRUPTED
const isSessionActive = ref(false)
const isConnecting = ref(false)

// WebRTC连接状态
const connectionState = ref('disconnected') // disconnected/connecting/connected/failed
const signalingState = ref('disconnected')

// WebRTC实例
let webrtcManager = null
let signalingClient = null

// 消息记录
const messages = ref([])
const currentRecognitionText = ref('')

// 音频可视化
const audioCanvas = ref(null)
const audioVolume = ref(0)
const remoteAudio = ref(null)

// 网络统计
const stats = ref({
  rtt: 0,
  jitter: 0,
  packetLoss: 0,
  bitrate: 0
})

// 设置
const settingsVisible = ref(false)
const selectedRoleId = ref(null)
const selectedModel = ref('openai:gpt-4o-mini')
const stunServer = ref('stun:stun.l.google.com:19302')
const debugMode = ref(false)

// 其他
const messagesContainer = ref(null)
const isBrowserSupported = ref(true)

// 定时器
let statsTimer = null

// ==================== 计算属性 ====================

const connectionStatusType = computed(() => {
  switch (connectionState.value) {
    case 'connected': return 'success'
    case 'connecting': return 'warning'
    case 'failed': return 'danger'
    default: return 'info'
  }
})

const connectionStatusText = computed(() => {
  switch (connectionState.value) {
    case 'connected': return '已连接'
    case 'connecting': return '连接中...'
    case 'failed': return '连接失败'
    default: return '未连接'
  }
})

const rtcStateType = computed(() => {
  switch (rtcState.value) {
    case 'SPEAKING': return 'success'
    case 'LISTENING': return 'primary'
    case 'THINKING': return 'warning'
    case 'INTERRUPTED': return 'danger'
    default: return 'info'
  }
})

const rtcStateText = computed(() => {
  const stateMap = {
    'IDLE': '空闲',
    'CONNECTING': '连接中',
    'LISTENING': '聆听中',
    'RECOGNIZING': '识别中',
    'THINKING': '思考中',
    'SPEAKING': '播放中',
    'INTERRUPTED': '已打断',
    'ERROR': '错误',
    'TERMINATED': '已结束'
  }
  return stateMap[rtcState.value] || rtcState.value
})

const canInterrupt = computed(() => {
  return isSessionActive.value && (rtcState.value === 'SPEAKING' || rtcState.value === 'THINKING')
})

const volumeColor = computed(() => {
  if (audioVolume.value > 80) return '#F56C6C'
  if (audioVolume.value > 50) return '#E6A23C'
  return '#67C23A'
})

// ==================== 核心方法 ====================

/**
 * 开始RTC会话
 */
const startSession = async () => {
  console.log('[RTC] 开始创建会话...')
  isConnecting.value = true

  try {
    // 1. 检查浏览器支持
    if (!WebRtcManager.isSupported()) {
      throw new Error('您的浏览器不支持WebRTC，请使用Chrome/Firefox/Safari最新版本')
    }

    // 2. 创建RTC会话
    const response = await createRtcSession({
      roleId: selectedRoleId.value,
      modelName: selectedModel.value
    })

    if (response.code !== 200) {
      throw new Error(response.message || '创建会话失败')
    }

    const sessionData = response.data
    sessionId.value = sessionData.sessionId
    
    console.log('[RTC] 会话创建成功:', sessionData)
    
    // 3. 初始化WebRTC管理器
    webrtcManager = new WebRtcManager()
    await webrtcManager.initialize()
    
    // 设置远端音频元素
    webrtcManager.setRemoteAudioElement(remoteAudio.value)
    
    // 设置ICE候选者回调
    webrtcManager.onIceCandidate = (candidate) => {
      if (signalingClient && signalingClient.isConnected()) {
        signalingClient.sendIceCandidate(sessionId.value, candidate)
      }
    }
    
    // 设置连接状态回调
    webrtcManager.onConnectionStateChange = (state) => {
      connectionState.value = state
      if (state === 'connected') {
        ElMessage.success('WebRTC连接成功')
        rtcState.value = 'LISTENING'
      } else if (state === 'failed') {
        ElMessage.error('WebRTC连接失败')
      }
    }

    // 4. 获取麦克风
    await webrtcManager.getLocalAudioStream()
    ElMessage.success('麦克风已授权')

    // 5. 连接信令WebSocket
    const wsUrl = sessionData.signalingUrl.replace('http://', 'ws://').replace('https://', 'wss://')
    signalingClient = new RtcSignalingClient(wsUrl, authStore.token)
    
    // 设置回调
    signalingClient.onConnected = () => {
      console.log('[Signaling] 信令连接成功')
      signalingState.value = 'connected'
      ElMessage.success('信令连接成功')
      
      // 连接成功后创建Offer
      createAndSendOffer()
    }
    
    signalingClient.onAnswer = (sdp) => {
      console.log('[Signaling] 收到SDP Answer')
      webrtcManager.handleAnswer(sdp)
    }
    
    signalingClient.onStateUpdate = (state) => {
      console.log('[Signaling] 状态更新:', state)
      rtcState.value = state
    }
    
    signalingClient.onError = (errorMsg) => {
      ElMessage.error(`信令错误: ${errorMsg}`)
    }
    
    signalingClient.onDisconnected = () => {
      signalingState.value = 'disconnected'
      ElMessage.warning('信令连接断开')
    }
    
    await signalingClient.connect()
    
    isSessionActive.value = true
    connectionState.value = 'connecting'
    
    // 启动统计定时器
    startStatsCollection()
    
  } catch (error) {
    console.error('[RTC] 启动会话失败:', error)
    ElMessage.error(error.message || '启动失败')
    cleanup()
  } finally {
    isConnecting.value = false
  }
}

/**
 * 创建并发送SDP Offer
 */
const createAndSendOffer = async () => {
  try {
    console.log('[RTC] 创建SDP Offer...')
    const sdp = await webrtcManager.createOffer()
    
    // 发送Offer到信令服务器
    signalingClient.sendOffer(sessionId.value, sdp)
    
    console.log('[RTC] SDP Offer已发送')
  } catch (error) {
    console.error('[RTC] 创建Offer失败:', error)
    ElMessage.error('创建SDP Offer失败')
  }
}

/**
 * 执行打断
 */
const performInterrupt = async (mode) => {
  if (!canInterrupt.value) {
    return
  }

  try {
    console.log(`[RTC] 执行${mode}中断...`)
    
    // 调用后端API
    await interruptRtc(sessionId.value, mode, 'user_click')
    
    // 同时通过信令发送
    if (signalingClient && signalingClient.isConnected()) {
      signalingClient.sendInterrupt(sessionId.value, mode, 'user_click')
    }
    
    ElMessage.success(`${mode === 'SOFT' ? '软' : '硬'}中断已发送`)
    
  } catch (error) {
    console.error('[RTC] 打断失败:', error)
    ElMessage.error('打断失败')
  }
}

/**
 * 结束会话
 */
const endSession = async () => {
  try {
    await ElMessageBox.confirm('确定要结束语音对话吗？', '确认', {
      type: 'warning'
    })

    console.log('[RTC] 结束会话...')

    // 调用后端API结束会话
    if (sessionId.value) {
      try {
        await endRtcSessionApi(sessionId.value)
      } catch (error) {
        console.error('[RTC] 调用结束API失败:', error)
      }
    }

    // 清理资源
    cleanup()

    ElMessage.success('会话已结束')
  } catch {
    // 用户取消
  }
}

/**
 * 清理资源
 */
const cleanup = () => {
  console.log('[RTC] 清理资源...')

  // 停止统计定时器
  stopStatsCollection()

  // 关闭信令连接
  if (signalingClient) {
    signalingClient.disconnect()
    signalingClient = null
  }

  // 关闭WebRTC连接
  if (webrtcManager) {
    webrtcManager.close()
    webrtcManager = null
  }

  // 重置状态
  isSessionActive.value = false
  sessionId.value = null
  rtcState.value = 'IDLE'
  connectionState.value = 'disconnected'
  signalingState.value = 'disconnected'
  currentRecognitionText.value = ''
  audioVolume.value = 0

  console.log('[RTC] 资源清理完成')
}

/**
 * 启动统计信息收集
 */
const startStatsCollection = () => {
  statsTimer = setInterval(async () => {
    if (webrtcManager) {
      const newStats = await webrtcManager.getStats()
      if (newStats) {
        stats.value = {
          rtt: Math.round(newStats.rtt),
          jitter: Math.round(newStats.jitter * 1000),
          packetLoss: newStats.packetLoss,
          bitrate: Math.round(newStats.bitrate / 1000)
        }
      }
    }
  }, 5000) // 每5秒更新一次
}

/**
 * 停止统计信息收集
 */
const stopStatsCollection = () => {
  if (statsTimer) {
    clearInterval(statsTimer)
    statsTimer = null
  }
}

/**
 * 显示设置
 */
const showSettings = () => {
  settingsVisible.value = true
}

/**
 * 添加消息到记录
 */
const addMessage = (type, text) => {
  const timestamp = new Date().toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })

  messages.value.push({
    type, // 'user' | 'assistant'
    text,
    timestamp
  })

  // 自动滚动到底部
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// ==================== 生命周期 ====================

onMounted(() => {
  console.log('[RTC] 组件已挂载')

  // 检查浏览器支持
  if (!WebRtcManager.isSupported()) {
    isBrowserSupported.value = false
    ElMessage.error('您的浏览器不支持WebRTC')
  }
})

onUnmounted(() => {
  console.log('[RTC] 组件销毁，清理资源')
  cleanup()
})
</script>

<style scoped>
.rtc-voice-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.status-bar {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 16px 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.session-info {
  font-size: 14px;
}

.session-info .label {
  color: #909399;
  margin-right: 8px;
}

.session-info .value {
  color: #303133;
  font-weight: 500;
  font-family: 'Monaco', monospace;
}

.main-content {
  display: flex;
  gap: 20px;
  flex: 1;
  min-height: 0;
}

.audio-visual {
  flex: 0 0 450px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.visual-card,
.stats-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.visual-card h3,
.stats-card h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
  font-weight: 600;
}

canvas {
  width: 100%;
  background: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 16px;
}

.volume-indicator {
  margin-top: 12px;
}

.volume-label {
  text-align: center;
  margin-top: 8px;
  font-size: 14px;
  color: #606266;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #EBEEF5;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-label {
  color: #909399;
  font-size: 14px;
}

.stat-value {
  color: #303133;
  font-weight: 500;
  font-size: 14px;
}

.conversation-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-height: 0;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  min-height: 0;
}

.message-item {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-item.user {
  flex-direction: row-reverse;
}

.message-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #409EFF;
  color: white;
}

.message-item.assistant .message-avatar {
  background: #67C23A;
}

.message-content {
  flex: 1;
  background: #F0F2F5;
  padding: 12px 16px;
  border-radius: 12px;
  max-width: 70%;
}

.message-item.user .message-content {
  background: #409EFF;
  color: white;
}

.message-text {
  font-size: 15px;
  line-height: 1.6;
  word-wrap: break-word;
}

.message-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.message-item.user .message-meta {
  color: rgba(255, 255, 255, 0.8);
}

.recognition-text {
  padding: 12px;
  background: #FFF7E6;
  border-radius: 8px;
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #E6A23C;
}

.control-bar {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  margin-top: 20px;
}

.text-center {
  text-align: center;
}

.text-right {
  text-align: right;
}

/* 滚动条样式 */
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.messages-container::-webkit-scrollbar-thumb {
  background: #DCDFE6;
  border-radius: 3px;
}

.messages-container::-webkit-scrollbar-thumb:hover {
  background: #C0C4CC;
}
</style>







<template>
  <div class="lab-container">
    <!-- 顶部状态栏 -->
    <header class="lab-header">
      <div class="header-left">
        <button @click="goBack" class="back-button">← 返回首页</button>
        <h1 class="title">🧪 流式聊天实验室</h1>
      </div>
      <div class="header-right">
        <div class="ws-status" :class="wsStatusClass">
          <span class="status-dot"></span>
          <span class="status-text">{{ wsStatusText }}</span>
        </div>
      </div>
    </header>

    <!-- 消息列表区域 -->
    <main class="message-area" ref="messageAreaRef" @scroll="handleScroll">
      <div v-if="messages.length === 0" class="empty-state">
        <p>开始一段新的对话...</p>
        <p class="hint">支持实时流式输出、分段语音合成</p>
      </div>
      
      <div v-for="(message, index) in messages" :key="message.id" class="message-item" :class="message.type">
        <div class="message-content">
          <div v-if="message.type === 'user'" class="user-message">
            {{ message.content }}
          </div>
          
          <div v-else-if="message.type === 'assistant'" class="assistant-message">
            <!-- 流式输出时显示原始文本，完成后显示渲染的Markdown -->
            <div v-if="message.isStreaming" class="streaming-text">
              {{ message.content }}
              <span class="cursor-blink">│</span>
            </div>
            <div v-else class="rendered-content" v-html="message.renderedContent || message.content"></div>
            
            <!-- 音频播放进度 -->
            <div v-if="message.audioSegments && message.audioSegments.length > 0" class="audio-progress">
              <span v-if="currentPlayingMessageId === message.id">
                🔊 播放中 ({{ currentPlayingIndex + 1 }}/{{ message.audioSegments.length }})
              </span>
              <span v-else>
                🔇 {{ message.audioSegments.length }} 个音频片段
              </span>
            </div>
          </div>
          
          <div v-else-if="message.type === 'system'" class="system-message">
            ⚠️ {{ message.content }}
          </div>
        </div>
        
        <div class="message-time">
          {{ formatTime(message.timestamp) }}
        </div>
      </div>
    </main>

    <!-- 底部控制区域 -->
    <footer class="lab-footer">
      <div class="controls">
        <div class="switch-group">
          <label class="switch">
            <input type="checkbox" v-model="enableWebSearch" />
            <span class="slider round"></span>
          </label>
          <span>联网搜索</span>
        </div>
        <div class="switch-group">
          <label class="switch">
            <input type="checkbox" v-model="enableAudio" />
            <span class="slider round"></span>
          </label>
          <span>语音回复</span>
        </div>
        <button v-if="isPlaying" @click="stopAudioPlayback" class="stop-audio-btn">
          ⏹ 停止播放
        </button>
      </div>
      
      <div class="input-area">
        <input 
          v-model="inputMessage" 
          @keypress.enter="sendMessage"
          :disabled="!isConnected || isSending"
          placeholder="输入消息，按Enter发送..."
          class="message-input"
        />
        <button 
          @click="sendMessage" 
          :disabled="!isConnected || isSending || !inputMessage.trim()"
          class="send-btn"
        >
          {{ isSending ? '发送中...' : '发送' }}
        </button>
      </div>
      
      <div class="footer-info">
        <span v-if="connectionError" class="error-info">
          {{ connectionError }}
          <button @click="reconnect" class="reconnect-btn">重新连接</button>
        </span>
        <button v-if="conversationId" @click="clearConversation" class="clear-btn">
          清空对话
        </button>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { ElMessage } from 'element-plus';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

// 路由和认证
const router = useRouter();
const authStore = useAuthStore();

// WebSocket相关
const ws = ref(null);
const wsStatus = ref('disconnected'); // disconnected, connecting, connected
const connectionError = ref('');
const reconnectAttempts = ref(0);
const maxReconnectAttempts = 3;
const reconnectTimeouts = [1000, 2000, 4000]; // 指数退避

// 对话相关
const messages = ref([]);
const inputMessage = ref('');
const isSending = ref(false);
const conversationId = ref(null);
const roleId = '1971856543655460865'; // 固定角色ID

// 开关选项
const enableWebSearch = ref(false);
const enableAudio = ref(true);

// 音频播放相关
const audioQueue = ref([]);
const currentAudio = ref(null);
const currentPlayingMessageId = ref(null);
const currentPlayingIndex = ref(0);
const isPlaying = ref(false);

// 自动滚动控制
const messageAreaRef = ref(null);
const userScrolled = ref(false);
let scrollCheckTimer = null;

// WebSocket状态计算属性
const isConnected = computed(() => wsStatus.value === 'connected');
const wsStatusClass = computed(() => ({
  'status-connected': wsStatus.value === 'connected',
  'status-connecting': wsStatus.value === 'connecting',
  'status-disconnected': wsStatus.value === 'disconnected'
}));
const wsStatusText = computed(() => {
  switch (wsStatus.value) {
    case 'connected': return '已连接';
    case 'connecting': return '连接中...';
    case 'disconnected': return '未连接';
    default: return '未知';
  }
});

// 初始化WebSocket连接
const initWebSocket = () => {
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    return; // 已经连接
  }
  
  wsStatus.value = 'connecting';
  connectionError.value = '';
  
  const token = authStore.token;
  if (!token) {
    connectionError.value = '未找到认证Token';
    authStore.logout();
    return;
  }
  
  const wsUrl = `ws://localhost:8081/ws/chat/stream?token=${token}`;
  
  try {
    ws.value = new WebSocket(wsUrl);
    
    ws.value.onopen = () => {
      console.log('WebSocket连接已建立');
      wsStatus.value = 'connected';
      connectionError.value = '';
      reconnectAttempts.value = 0;
      
      // 从sessionStorage恢复conversationId
      const savedConversationId = sessionStorage.getItem('lab-conversation-id');
      if (savedConversationId) {
        conversationId.value = parseInt(savedConversationId);
      }
    };
    
    ws.value.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        handleWebSocketMessage(data);
      } catch (error) {
        console.error('解析WebSocket消息失败:', error);
      }
    };
    
    ws.value.onerror = (error) => {
      console.error('WebSocket错误:', error);
      connectionError.value = '连接出现错误';
    };
    
    ws.value.onclose = (event) => {
      console.log('WebSocket连接已关闭', event);
      wsStatus.value = 'disconnected';
      
      // 检查是否是认证失败
      if (event.code === 1008 || event.code === 1003) {
        connectionError.value = 'Token认证失败，请重新登录';
        setTimeout(() => {
          authStore.logout();
        }, 2000);
        return;
      }
      
      // 自动重连
      if (reconnectAttempts.value < maxReconnectAttempts) {
        const timeout = reconnectTimeouts[reconnectAttempts.value] || 5000;
        connectionError.value = `连接断开，${timeout/1000}秒后重试...`;
        setTimeout(() => {
          reconnectAttempts.value++;
          initWebSocket();
        }, timeout);
      } else {
        connectionError.value = '连接失败，请检查网络';
      }
    };
  } catch (error) {
    console.error('创建WebSocket失败:', error);
    connectionError.value = '创建连接失败';
    wsStatus.value = 'disconnected';
  }
};

// 处理WebSocket消息
const handleWebSocketMessage = (data) => {
  switch (data.type) {
    case 'START':
      handleStartMessage(data);
      break;
    case 'CONTENT':
      handleContentMessage(data);
      break;
    case 'TTS_SEGMENT':
      handleTTSSegment(data);
      break;
    case 'TTS_SEGMENT_UPDATE':
      handleTTSSegmentUpdate(data);
      break;
    case 'END':
      handleEndMessage(data);
      break;
    case 'ERROR':
      handleErrorMessage(data);
      break;
    case 'HEARTBEAT':
      // 心跳消息，不需要处理
      break;
    default:
      console.log('未知消息类型:', data.type);
  }
};

// 处理START消息
const handleStartMessage = (data) => {
  // 添加AI消息占位
  const aiMessage = {
    id: `msg-${Date.now()}`,
    type: 'assistant',
    content: '',
    isStreaming: true,
    audioSegments: [],
    timestamp: new Date(),
    model: data.model
  };
  messages.value.push(aiMessage);
  scrollToBottom();
};

// 处理CONTENT消息
const handleContentMessage = (data) => {
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'assistant' && lastMessage.isStreaming) {
    lastMessage.content += data.delta || '';
    scrollToBottom();
  }
};

// 处理TTS_SEGMENT消息
const handleTTSSegment = (data) => {
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'assistant') {
    if (!lastMessage.audioSegments) {
      lastMessage.audioSegments = [];
    }
    
    // 添加或更新音频片段
    const segment = {
      index: data.index,
      text: data.delta,
      audioUrl: data.audioUrl,
      groupId: data.ttsGroupId
    };
    
    // 确保按index排序插入
    const existingIndex = lastMessage.audioSegments.findIndex(s => s.index === data.index);
    if (existingIndex >= 0) {
      lastMessage.audioSegments[existingIndex] = segment;
    } else {
      lastMessage.audioSegments.push(segment);
      lastMessage.audioSegments.sort((a, b) => a.index - b.index);
    }
    
    // 如果启用了音频且有音频URL，开始播放
    if (enableAudio.value && data.audioUrl && !isPlaying.value) {
      startAudioPlayback(lastMessage);
    }
  }
};

// 处理TTS_SEGMENT_UPDATE消息
const handleTTSSegmentUpdate = (data) => {
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'assistant' && lastMessage.audioSegments) {
    const segment = lastMessage.audioSegments.find(s => s.index === data.index);
    if (segment && !segment.audioUrl) {
      segment.audioUrl = data.audioUrl;
    }
  }
};

// 处理END消息
const handleEndMessage = (data) => {
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'assistant') {
    lastMessage.isStreaming = false;
    
    // 保存conversationId
    if (data.conversationId) {
      conversationId.value = data.conversationId;
      sessionStorage.setItem('lab-conversation-id', data.conversationId.toString());
    }
    
    // 渲染Markdown
    if (lastMessage.content) {
      lastMessage.renderedContent = renderMarkdown(lastMessage.content);
    }
    
    // 更新消息元数据
    if (data.messageId) {
      lastMessage.messageId = data.messageId;
    }
    if (data.responseTimeMs) {
      lastMessage.responseTime = data.responseTimeMs;
    }
  }
  isSending.value = false;
};

// 处理ERROR消息
const handleErrorMessage = (data) => {
  messages.value.push({
    id: `msg-${Date.now()}`,
    type: 'system',
    content: data.errorMessage || '发生未知错误',
    timestamp: new Date()
  });
  isSending.value = false;
  scrollToBottom();
};

// 发送消息
const sendMessage = () => {
  if (!inputMessage.value.trim() || !isConnected.value || isSending.value) {
    return;
  }
  
  const messageText = inputMessage.value.trim();
  inputMessage.value = '';
  
  // 添加用户消息到列表
  messages.value.push({
    id: `msg-${Date.now()}`,
    type: 'user',
    content: messageText,
    timestamp: new Date()
  });
  
  // 停止任何正在播放的音频
  stopAudioPlayback();
  
  // 构建请求消息 - 注意roleId应该是数字类型
  const requestData = {
    conversationId: conversationId.value,
    message: messageText,
    roleId: parseInt(roleId),
    enableWebSearch: enableWebSearch.value,
    enableAudio: enableAudio.value
  };
  
  // 发送到WebSocket
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    ws.value.send(JSON.stringify(requestData));
    isSending.value = true;
  } else {
    ElMessage.error('WebSocket未连接');
  }
  
  scrollToBottom();
};

// 音频播放管理
const startAudioPlayback = (message) => {
  if (!message.audioSegments || message.audioSegments.length === 0) {
    return;
  }
  
  // 停止之前的播放
  stopAudioPlayback();
  
  // 筛选出有音频的片段
  const playableSegments = message.audioSegments.filter(s => s.audioUrl);
  if (playableSegments.length === 0) {
    return;
  }
  
  currentPlayingMessageId.value = message.id;
  audioQueue.value = [...playableSegments];
  isPlaying.value = true;
  currentPlayingIndex.value = 0;
  
  playNextSegment();
};

const playNextSegment = () => {
  if (audioQueue.value.length === 0) {
    // 播放完成
    currentPlayingMessageId.value = null;
    currentPlayingIndex.value = 0;
    isPlaying.value = false;
    return;
  }
  
  const segment = audioQueue.value.shift();
  
  if (!segment.audioUrl) {
    // 跳过没有音频的片段
    playNextSegment();
    return;
  }
  
  const audio = new Audio(segment.audioUrl);
  currentAudio.value = audio;
  
  audio.addEventListener('ended', () => {
    currentPlayingIndex.value++;
    playNextSegment();
  });
  
  audio.addEventListener('error', (e) => {
    console.error('音频播放失败:', e);
    ElMessage.warning(`音频片段 ${segment.index + 1} 播放失败，跳过`);
    currentPlayingIndex.value++;
    playNextSegment();
  });
  
  audio.play().catch(e => {
    console.error('播放音频出错:', e);
    currentPlayingIndex.value++;
    playNextSegment();
  });
};

const stopAudioPlayback = () => {
  if (currentAudio.value) {
    currentAudio.value.pause();
    currentAudio.value = null;
  }
  audioQueue.value = [];
  currentPlayingMessageId.value = null;
  currentPlayingIndex.value = 0;
  isPlaying.value = false;
};

// 渲染Markdown
const renderMarkdown = (content) => {
  if (!content) return '';
  try {
    const rawHtml = marked.parse(content);
    return DOMPurify.sanitize(rawHtml);
  } catch (error) {
    console.error('Markdown渲染失败:', error);
    return content;
  }
};

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  return date.toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit', 
    second: '2-digit' 
  });
};

// 滚动控制
const handleScroll = () => {
  if (!messageAreaRef.value) return;
  
  clearTimeout(scrollCheckTimer);
  scrollCheckTimer = setTimeout(() => {
    const { scrollTop, scrollHeight, clientHeight } = messageAreaRef.value;
    const isAtBottom = scrollHeight - scrollTop - clientHeight < 100;
    userScrolled.value = !isAtBottom;
  }, 100);
};

const scrollToBottom = async () => {
  // 如果用户手动滚动了，不自动滚动
  if (userScrolled.value) return;
  
  await nextTick();
  if (messageAreaRef.value) {
    messageAreaRef.value.scrollTop = messageAreaRef.value.scrollHeight;
  }
};

// 清空对话
const clearConversation = () => {
  messages.value = [];
  conversationId.value = null;
  sessionStorage.removeItem('lab-conversation-id');
  stopAudioPlayback();
  ElMessage.success('对话已清空');
};

// 重新连接
const reconnect = () => {
  reconnectAttempts.value = 0;
  initWebSocket();
};

// 返回首页
const goBack = () => {
  router.push('/');
};

// 生命周期
onMounted(() => {
  initWebSocket();
});

onUnmounted(() => {
  stopAudioPlayback();
  if (ws.value) {
    ws.value.close();
    ws.value = null;
  }
  clearTimeout(scrollCheckTimer);
});
</script>

<style scoped>
/* 容器布局 */
.lab-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #1a1a1a;
  color: #e5e7eb;
}

/* 头部样式 */
.lab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background-color: #2d2d2d;
  border-bottom: 1px solid #404040;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

.back-button {
  background: none;
  border: 1px solid #4b5563;
  color: #9ca3af;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-button:hover {
  background-color: #374151;
  color: #e5e7eb;
}

.title {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
}

/* WebSocket状态指示器 */
.ws-status {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 20px;
  background-color: rgba(0, 0, 0, 0.3);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #6b7280;
}

.status-connected .status-dot {
  background-color: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.status-connecting .status-dot {
  background-color: #f59e0b;
  animation: pulse 1.5s infinite;
}

.status-disconnected .status-dot {
  background-color: #ef4444;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.5; }
  100% { opacity: 1; }
}

.status-text {
  font-size: 0.875rem;
}

/* 消息区域 */
.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  scroll-behavior: smooth;
}

.empty-state {
  text-align: center;
  padding: 4rem 1rem;
  color: #6b7280;
}

.empty-state .hint {
  margin-top: 0.5rem;
  font-size: 0.875rem;
  color: #4b5563;
}

/* 消息样式 */
.message-item {
  margin-bottom: 1.5rem;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.message-content {
  padding: 0.75rem 1rem;
  border-radius: 8px;
  max-width: 80%;
}

.user .message-content {
  margin-left: auto;
  background-color: #2563eb;
  color: white;
}

.assistant .message-content {
  margin-right: auto;
  background-color: #374151;
}

.system .message-content {
  margin: 0 auto;
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid #7f1d1d;
  color: #fca5a5;
  text-align: center;
}

/* 流式文本样式 */
.streaming-text {
  position: relative;
}

.cursor-blink {
  animation: blink 1s infinite;
  color: #3b82f6;
  font-weight: bold;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* Markdown渲染样式 */
.rendered-content {
  line-height: 1.6;
}

.rendered-content :deep(pre) {
  background-color: #1f2937;
  padding: 1rem;
  border-radius: 6px;
  overflow-x: auto;
}

.rendered-content :deep(code) {
  background-color: #1f2937;
  padding: 0.2rem 0.4rem;
  border-radius: 3px;
  font-size: 0.875rem;
}

.rendered-content :deep(ul),
.rendered-content :deep(ol) {
  margin-left: 1.5rem;
}

.rendered-content :deep(blockquote) {
  border-left: 3px solid #4b5563;
  padding-left: 1rem;
  color: #9ca3af;
}

.rendered-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 1rem 0;
}

.rendered-content :deep(th),
.rendered-content :deep(td) {
  border: 1px solid #4b5563;
  padding: 0.5rem;
}

.rendered-content :deep(th) {
  background-color: #1f2937;
}

/* 音频进度 */
.audio-progress {
  margin-top: 0.5rem;
  font-size: 0.875rem;
  color: #60a5fa;
}

/* 消息时间 */
.message-time {
  margin-top: 0.25rem;
  font-size: 0.75rem;
  color: #6b7280;
  text-align: right;
}

.user .message-time {
  text-align: right;
}

.assistant .message-time {
  text-align: left;
}

/* 底部控制区域 */
.lab-footer {
  padding: 1rem 1.5rem;
  background-color: #2d2d2d;
  border-top: 1px solid #404040;
}

.controls {
  display: flex;
  gap: 1.5rem;
  align-items: center;
  margin-bottom: 1rem;
}

.switch-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
}

/* 开关样式 */
.switch {
  position: relative;
  display: inline-block;
  width: 40px;
  height: 22px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #4b5563;
  transition: 0.4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.4s;
}

input:checked + .slider {
  background-color: #3b82f6;
}

input:checked + .slider:before {
  transform: translateX(18px);
}

.slider.round {
  border-radius: 22px;
}

.slider.round:before {
  border-radius: 50%;
}

.stop-audio-btn {
  background-color: #dc2626;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.stop-audio-btn:hover {
  background-color: #b91c1c;
}

/* 输入区域 */
.input-area {
  display: flex;
  gap: 1rem;
}

.message-input {
  flex: 1;
  padding: 0.75rem 1rem;
  background-color: #1f2937;
  border: 1px solid #4b5563;
  border-radius: 8px;
  color: #e5e7eb;
  font-size: 1rem;
  outline: none;
  transition: border-color 0.2s;
}

.message-input:focus {
  border-color: #3b82f6;
}

.message-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.send-btn {
  padding: 0.75rem 1.5rem;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}

.send-btn:hover:not(:disabled) {
  background-color: #2563eb;
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 底部信息 */
.footer-info {
  margin-top: 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
}

.error-info {
  color: #f87171;
  display: flex;
  align-items: center;
  gap: 1rem;
}

.reconnect-btn {
  background-color: transparent;
  border: 1px solid #f87171;
  color: #f87171;
  padding: 0.25rem 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.reconnect-btn:hover {
  background-color: rgba(248, 113, 113, 0.1);
}

.clear-btn {
  background-color: transparent;
  border: 1px solid #6b7280;
  color: #9ca3af;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.clear-btn:hover {
  background-color: #374151;
  color: #e5e7eb;
}

/* 滚动条样式 */
.message-area::-webkit-scrollbar {
  width: 8px;
}

.message-area::-webkit-scrollbar-track {
  background: #1f2937;
  border-radius: 4px;
}

.message-area::-webkit-scrollbar-thumb {
  background: #4b5563;
  border-radius: 4px;
}

.message-area::-webkit-scrollbar-thumb:hover {
  background: #6b7280;
}

/* 响应式 */
@media (max-width: 768px) {
  .lab-header {
    flex-direction: column;
    gap: 1rem;
  }
  
  .header-left {
    width: 100%;
    justify-content: space-between;
  }
  
  .title {
    font-size: 1.2rem;
  }
  
  .controls {
    flex-wrap: wrap;
  }
  
  .message-content {
    max-width: 90%;
  }
  
  .input-area {
    flex-direction: column;
  }
  
  .send-btn {
    width: 100%;
  }
}
</style>

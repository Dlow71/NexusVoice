<template>
  <div class="chat-container">
    <!-- 左侧对话历史列表 -->
    <ConversationSidebar
        :history="conversationHistory"
        :active-id="conversationId"
        @switch-conversation="handleSwitchConversation"
        @delete-conversation="handleDeleteConversation"
    />
    
    <!-- 右侧聊天区域 -->
    <div class="chat-view">
      <!-- 顶部角色信息栏 -->
      <header class="chat-header" v-if="currentCharacter">
        <div class="character-info">
          <button @click="goBack" class="back-button">&lt;</button>
          <img
              :src="getAvatarUrl(currentCharacter.avatarUrl)"
              alt="avatar"
              class="avatar"
          />
          <h2 class="name">{{ currentCharacter.name }}</h2>
        </div>
        <div class="header-actions">
          <!-- 角色创建助手按钮 -->
          <button
              @click="openAssistantPanel"
              class="assistant-trigger-btn"
              title="根据当前对话创建新角色"
          >
            <span class="assistant-icon">✨</span>
            <span class="assistant-text">角色生成</span>
          </button>
          <!-- WebSocket状态指示器 -->
          <div class="ws-status" :class="wsStatusClass">
            <span class="status-dot"></span>
            <span class="status-text">{{ wsStatusText }}</span>
          </div>
        </div>
      </header>
      <div v-else class="chat-header-placeholder"></div>
      
      <!-- 消息列表区域 -->
      <main class="message-list" ref="messageListEl" @scroll="handleScroll">
        <div v-if="isLoading" class="loading-messages">正在加载对话内容...</div>
        <div v-else-if="hasError" class="error-messages">
          {{ errorMessage }}
        </div>
        <div v-else-if="messages.length === 0" class="empty-state">
          <p>开始一段新的对话...</p>
          <p class="hint">支持实时流式输出、分段语音合成</p>
        </div>
        
        <!-- 消息列表 -->
        <div v-for="(message, index) in messages" 
             :key="message.id" 
             class="message-item" 
             :class="message.type">
          <div class="message-bubble-wrapper">
            <!-- 角色头像（AI消息） -->
            <div v-if="message.type === 'assistant'" class="message-avatar">
              <img :src="getAvatarUrl(currentCharacter?.avatarUrl)" 
                   :alt="currentCharacter?.name || 'AI'" />
            </div>
            
            <!-- 消息内容 -->
            <div class="message-bubble">
              <!-- 用户消息 -->
              <div v-if="message.type === 'user'" class="message-content">
                {{ message.content }}
              </div>
              
              <!-- AI消息 -->
              <div v-else-if="message.type === 'assistant'" class="message-content">
                <!-- 流式输出时显示原始文本 -->
                <div v-if="message.isStreaming" class="streaming-text">
                  {{ message.content }}
                  <span class="cursor-blink">│</span>
                </div>
                <!-- 完成后显示渲染的Markdown -->
                <div v-else class="rendered-content" 
                     v-html="message.renderedContent || message.content"></div>
                
                <!-- 音频播放控制 -->
                <div v-if="message.audioSegments && message.audioSegments.length > 0" 
                     class="audio-controls">
                  <button v-if="currentPlayingMessageId !== message.id" 
                          @click="startAudioPlayback(message)"
                          class="audio-play-btn">
                    ▶️ 播放音频 ({{ message.audioSegments.length }}段)
                  </button>
                  <div v-else class="audio-playing">
                    <button @click="stopAudioPlayback" class="audio-stop-btn">
                      ⏹️ 停止
                    </button>
                    <span class="audio-progress">
                      播放中 ({{ currentPlayingIndex + 1 }}/{{ message.audioSegments.length }})
                    </span>
                  </div>
                </div>
              </div>
              
              <!-- 系统消息 -->
              <div v-else-if="message.type === 'system'" class="message-content system">
                ⚠️ {{ message.content }}
              </div>
              
              <!-- 时间戳 -->
              <div class="message-time">
                {{ formatTime(message.timestamp) }}
              </div>
            </div>
            
            <!-- 用户头像（用户消息） -->
            <div v-if="message.type === 'user'" class="message-avatar user">
              <img :src="getUserAvatar()" alt="User" />
            </div>
          </div>
        </div>
        
        <!-- AI正在思考指示器 -->
        <div v-if="isAIThinking" class="typing-indicator">
          <div class="typing-dot"></div>
          <div class="typing-dot"></div>
          <div class="typing-dot"></div>
        </div>
      </main>
      
      <!-- 底部输入区域 -->
      <footer class="chat-footer">
        <div class="footer-options">
          <p class="system-message">{{ systemMessage }}</p>
          
          <!-- 模型选择器 -->
          <div class="model-selector" :class="{ 'locked': isModelLocked }">
            <label class="model-label">
              <span class="model-icon">🤖</span>
              <span v-if="isModelLocked" class="lock-icon" title="会话已绑定模型，无法切换">🔒</span>
              模型：
            </label>
            <div class="model-buttons">
              <button 
                v-for="model in availableModels" 
                :key="model.modelKey"
                @click="!isModelLocked && (selectedModel = model.modelKey)"
                :class="{ 
                  'active': selectedModel === model.modelKey,
                  'disabled': isModelLocked
                }"
                :disabled="isModelLocked"
                class="model-btn"
                :title="isModelLocked ? '当前会话已绑定' + model.modelName : model.description"
              >
                <span class="model-name">{{ model.modelName }}</span>
                <span v-if="selectedModel === model.modelKey" class="check-mark">✓</span>
              </button>
            </div>
          </div>
          
          <div class="search-toggle-container">
            <label class="switch">
              <input type="checkbox" v-model="enableWebSearch" />
              <span class="slider round"></span>
            </label>
            <span>联网搜索</span>
          </div>
          <div class="search-toggle-container">
            <label class="switch">
              <input type="checkbox" v-model="enableAudio" />
              <span class="slider round"></span>
            </label>
            <span>音频回复</span>
          </div>
        </div>
        
        <!-- 输入框和发送按钮 -->
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
          <!-- 强制结束按钮 -->
          <button 
            v-if="isSending"
            @click="forceEndStream" 
            class="force-end-btn"
            title="强制结束当前回复"
          >
            停止
          </button>
        </div>
        
        <!-- 连接错误提示 -->
        <div v-if="connectionError" class="footer-info">
          <span class="error-info">
            {{ connectionError }}
            <button @click="reconnect" class="reconnect-btn">重新连接</button>
          </span>
        </div>
      </footer>
    </div>

    <!-- 角色创建助手面板 -->
    <div
        class="assistant-panel-overlay"
        v-if="isAssistantPanelVisible"
        @click.self="isAssistantPanelVisible = false"
    >
      <div class="assistant-panel">
        <header class="panel-header">
          <h3>角色创建助手</h3>
          <button @click="isAssistantPanelVisible = false" class="close-btn">
            ×
          </button>
        </header>
        <main class="panel-content">
          <div v-if="isAssistantLoading" class="panel-loading">处理中...</div>
          <div v-if="assistantStep === 'initial'">
            <p class="panel-description">
              AI将根据当前对话内容，为您生成一个角色定义的草稿。您可以在草稿基础上进行修改和深化。
            </p>
            <button
                @click="handleGenerateBrief"
                :disabled="isAssistantLoading"
                class="panel-btn-primary"
            >
              生成角色草稿
            </button>
          </div>
          <div v-if="assistantStep === 'brief_generated' && roleBrief">
            <h4>
              角色草稿预览
              <button class="panel-btn-link" @click="assistantStep = 'initial'">
                重新生成
              </button>
            </h4>
            <div class="form-group">
              <label>角色描述</label>
              <textarea v-model="roleBrief.description" rows="3"></textarea>
            </div>
            <div class="form-group">
              <label>角色名称</label>
              <input type="text" v-model="roleBrief.name" />
            </div>
            <div class="form-group">
              <label>人设提示词 (Persona)</label>
              <textarea v-model="roleBrief.personaPrompt" rows="6"></textarea>
            </div>
            <div class="form-group">
              <label>开场白</label>
              <textarea v-model="roleBrief.greetingMessage" rows="3"></textarea>
            </div>
            <div class="form-group">
              <label>头像</label>
              <div class="avatar-upload-group">
                <img v-if="roleBrief.avatarUrl" :src="roleBrief.avatarUrl" alt="头像预览" class="avatar-preview">
                <div class="upload-inputs">
                  <input type="text" v-model="roleBrief.avatarUrl" placeholder="可粘贴URL或上传图片">
                  <div class="button-group"> 
                    <input type="file" ref="fileInput" @change="handleImageUpload" accept="image/*" style="display: none;">
                    <button type="button" class="btn-upload" @click="triggerFileInput" :disabled="isUploading">
                      {{ isUploading ? '上传中...' : '上传图片' }}
                    </button>
                    <button type="button" class="btn-generate" @click="handleImageGeneration" :disabled="isGeneratingImage">
                      {{ isGeneratingImage ? '生成中...' : 'AI生成头像' }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>TTS声音类型 *</label>
              <div class="voice-type-group">
                <select v-model="roleBrief.voiceType" required>
                  <option disabled value="">请选择一个声音</option>
                  <option v-for="voice in voiceList" :key="voice.voice_type" :value="voice.voice_type">
                    {{ voice.voice_name }}
                  </option>
                </select>
                <button type="button" class="btn-preview" @click="previewVoice" :disabled="!roleBrief.voiceType" title="试听声音">
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="20" height="20">
                    <path d="M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z"></path>
                  </svg>
                </button>
              </div>
            </div>

            <div class="panel-actions">
              <button
                  @click="handleConfirmCreation(false)"
                  :disabled="isAssistantLoading"
                  class="panel-btn-primary"
              >
                快速创建角色
              </button>
              <button
                  @click="handlePreviewTasks"
                  :disabled="isAssistantLoading"
                  class="panel-btn-secondary"
              >
                (可选) 深化角色...
              </button>
            </div>
          </div>
          <div v-if="assistantStep === 'tasks_previewed'">
            <h4>
              建议的深化研究任务
              <button
                  class="panel-btn-link"
                  @click="assistantStep = 'brief_generated'"
              >
                返回草稿
              </button>
            </h4>
            <p class="panel-description">
              编辑或添加AI需要联网搜索的关键词，以丰富角色的知识和细节。
            </p>
            <div
                v-for="(task, index) in researchTasks"
                :key="task.id"
                class="task-item"
            >
              <input type="checkbox" v-model="task.enabled" :id="'task-' + index" />
              <input type="text" v-model="task.query" class="task-query" />
              <button
                  @click="researchTasks.splice(index, 1)"
                  class="task-delete-btn"
              >
                -
              </button>
            </div>
            <div class="panel-actions">
              <button
                  @click="handleConfirmCreation(true)"
                  :disabled="isAssistantLoading"
                  class="panel-btn-primary"
              >
                执行深研并创建
              </button>
            </div>
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { ElMessage, ElMessageBox } from 'element-plus';
import ConversationSidebar from '../components/ConversationSidebar.vue';
import characterApi from '../services/character';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

// 路由和认证
const route = useRoute();
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
const conversationId = ref(null); // 注意：保持为字符串类型，避免精度丢失
const roleId = computed(() => route.params.roleId); // 从路由获取动态roleId

// 模型选择相关
const availableModels = ref([]); // 可用模型列表
const selectedModel = ref('openai:gpt-oss-20b'); // 默认选中gpt-oss-20b
const isModelLocked = computed(() => !!conversationId.value); // 有对话后锁定模型

// 角色信息
const currentCharacter = ref(null);
const conversationHistory = ref([]);
const isLoading = ref(true);
const hasError = ref(false);
const errorMessage = ref('');
const systemMessage = ref('正在加载...');
const isAIThinking = ref(false);

// 开关选项
const enableWebSearch = ref(false);
const enableAudio = ref(false);  // 默认关闭音频，避免后端TTS问题导致卡死

// 加载可用模型列表
const fetchAvailableModels = async () => {
  try {
    const response = await characterApi.getAvailableModels();
    if (response.data.success && response.data.data) {
      // 只保留我们需要的三个模型
      availableModels.value = response.data.data.filter(
        model => model.modelKey === 'openai:gpt-oss-20b' || 
                model.modelKey === 'grok:grok-4-fast' ||
                model.modelKey === 'deepseek:deepseek-v3.1'
      );
      
      // 如果没有找到指定的模型，手动添加（作为备用）
      if (availableModels.value.length === 0) {
        availableModels.value = [
          {
            modelKey: 'openai:gpt-oss-20b',
            modelName: 'GPT OSS 20B',
            description: 'OpenAI兼容的开源模型',
            contextWindow: 128000
          },
          {
            modelKey: 'grok:grok-4-fast',
            modelName: 'Grok 4 Fast',
            description: 'xAI Grok 4快速版',
            contextWindow: 131072
          },
          {
            modelKey: 'deepseek:deepseek-v3.1',
            modelName: 'DeepSeek V3.1',
            description: 'DeepSeek 深度思考模型',
            contextWindow: 131072
          }
        ];
      }
    }
  } catch (error) {
    console.error('加载模型列表失败:', error);
    // 如果调用失败，使用默认配置
    availableModels.value = [
      {
        modelKey: 'openai:gpt-oss-20b',
        modelName: 'GPT OSS 20B',
        description: 'OpenAI兼容的开源模型',
        contextWindow: 128000
      },
      {
        modelKey: 'grok:grok-4-fast',
        modelName: 'Grok 4 Fast',
        description: 'xAI Grok 4快速版',
        contextWindow: 131072
      },
      {
        modelKey: 'deepseek:deepseek-v3.1',
        modelName: 'DeepSeek V3.1',
        description: 'DeepSeek 深度思考模型',
        contextWindow: 131072
      }
    ];
  }
};

// 音频播放相关
const audioQueue = ref([]);
const currentAudio = ref(null);
const currentPlayingMessageId = ref(null);
const currentPlayingIndex = ref(0);
const isPlaying = ref(false);

// 消息超时处理
let messageTimeoutTimer = null;
const MESSAGE_TIMEOUT = 30000; // 30秒超时

// 消息流状态检测
let lastContentTime = null; // 最后收到CONTENT消息的时间
let streamCheckTimer = null; // 检查消息流是否停止的定时器
const STREAM_IDLE_TIMEOUT = 5000; // 5秒没有新内容认为流结束
const QUICK_END_TIMEOUT = 2000; // 如果有完整句子，2秒就结束
let hasCompleteSentence = false; // 是否检测到完整句子

// 自动滚动控制
const messageListEl = ref(null);
const userScrolled = ref(false);
let scrollCheckTimer = null;

// 角色助手状态
const isAssistantPanelVisible = ref(false);
const isAssistantLoading = ref(false);
const assistantStep = ref("initial");
const roleBrief = ref(null);
const researchTasks = ref([]);

// 表单状态
const fileInput = ref(null);
const isUploading = ref(false);
const isGeneratingImage = ref(false);
const voiceList = ref([]);
let currentPreviewAudio = null;

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
    return;
  }
  
  wsStatus.value = 'connecting';
  connectionError.value = '';
  
  const token = authStore.token;
  if (!token) {
    connectionError.value = '未找到认证Token';
    authStore.logout();
    return;
  }
  
  // 使用子协议（Sec-WebSocket-Protocol）传递token，更安全
  // 格式：Bearer.{token}，后端会从子协议中提取
  const wsUrl = `ws://localhost:8081/ws/chat/stream`;
  const protocol = `Bearer.${token}`;
  
  try {
    ws.value = new WebSocket(wsUrl, protocol);
    
    ws.value.onopen = () => {
      console.log('WebSocket连接已建立');
      wsStatus.value = 'connected';
      connectionError.value = '';
      reconnectAttempts.value = 0;
      systemMessage.value = '连接成功，可以开始对话了';
      
      // 从sessionStorage恢复conversationId（保持字符串类型）
      const savedConversationId = sessionStorage.getItem(`stream-conversation-${roleId.value}`);
      if (savedConversationId) {
        conversationId.value = savedConversationId;
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
  console.log(`[收到消息] 类型: ${data.type}`, data);
  
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
      // 心跳消息，不做处理
      break;
    default:
      console.warn('未知消息类型:', data.type);
  }
};

// 处理START消息
const handleStartMessage = (data) => {
  console.log('[开始流式输出]', data);
  isAIThinking.value = false;
  systemMessage.value = `${currentCharacter.value?.name || 'AI'} 正在回复...`;
  
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
  
  // 记录开始时间
  lastContentTime = Date.now();
  hasCompleteSentence = false;
  
  // 启动超时定时器
  startMessageTimeout();
  
  // 启动流状态检测
  startStreamCheck();
};

// 处理CONTENT消息
const handleContentMessage = (data) => {
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'assistant' && lastMessage.isStreaming) {
    const delta = data.delta || '';
    lastMessage.content += delta;
    
    // 更新最后收到内容的时间
    if (delta.length > 0) {
      lastContentTime = Date.now();
      
      // 检测是否有完整的句子结束标记
      const fullContent = lastMessage.content;
      hasCompleteSentence = /[。！？.!?]\s*$/.test(fullContent) || 
                           /[。！？.!?][\s\n]/.test(fullContent) ||
                           fullContent.includes('\n\n');
      
      if (hasCompleteSentence) {
        console.log('[句子检测] 检测到完整句子结束');
      }
    }
    
    scrollToBottom();
    
    // 重置超时定时器（收到新内容，说明还在正常处理）
    startMessageTimeout();
  }
};

// 处理TTS_SEGMENT消息
const handleTTSSegment = (data) => {
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'assistant') {
    if (!lastMessage.audioSegments) {
      lastMessage.audioSegments = [];
    }
    
    const segment = {
      index: data.index,
      text: data.delta,
      audioUrl: data.audioUrl,
      groupId: data.ttsGroupId
    };
    
    const existingIndex = lastMessage.audioSegments.findIndex(s => s.index === data.index);
    if (existingIndex >= 0) {
      lastMessage.audioSegments[existingIndex] = segment;
    } else {
      lastMessage.audioSegments.push(segment);
      lastMessage.audioSegments.sort((a, b) => a.index - b.index);
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
  console.log('[流式输出结束]', data);
  
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'assistant') {
    lastMessage.isStreaming = false;
    
    // 保存conversationId
    if (data.conversationId) {
      const oldConversationId = conversationId.value;
      conversationId.value = data.conversationId;
      sessionStorage.setItem(`stream-conversation-${roleId.value}`, data.conversationId.toString());
      
      // 如果是新对话，更新左侧列表
      if (!oldConversationId || oldConversationId !== data.conversationId) {
        fetchConversationHistory();
      }
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
    
    // 如果启用了音频且有音频片段，开始自动播放
    if (enableAudio.value && lastMessage.audioSegments && lastMessage.audioSegments.length > 0) {
      const playableSegments = lastMessage.audioSegments.filter(s => s.audioUrl);
      if (playableSegments.length > 0) {
        startAudioPlayback(lastMessage);
      }
    }
  }
  
  isSending.value = false;
  isAIThinking.value = false;  // 关闭思考指示器
  systemMessage.value = '准备就绪';
  
  console.log('[状态重置] isSending=false, isAIThinking=false');
  
  // 清除所有定时器
  clearMessageTimeout();
  clearStreamCheck();
};

// 处理ERROR消息
const handleErrorMessage = (data) => {
  console.error('[错误消息]', data);
  
  // 特殊处理：如果是"上一个请求仍在处理中"的错误，说明后端状态卡住了
  const errorMsg = data.errorMessage || '发生未知错误';
  if (errorMsg.includes('上一个请求仍在处理中')) {
    console.error('[严重错误] 后端会话锁未释放，自动重连WebSocket');
    
    // 强制结束所有流式消息
    messages.value.forEach(msg => {
      if (msg.type === 'assistant' && msg.isStreaming) {
        msg.isStreaming = false;
        if (msg.content) {
          msg.renderedContent = renderMarkdown(msg.content);
        }
      }
    });
    
    // 添加系统提示
    messages.value.push({
      id: `msg-${Date.now()}`,
      type: 'system',
      content: '检测到后端会话锁卡住，正在自动重连...',
      timestamp: new Date()
    });
    
    // 关闭旧连接并重新连接
    if (ws.value) {
      ws.value.close();
      ws.value = null;
    }
    
    // 延迟500ms后重连
    setTimeout(() => {
      initWebSocket();
      ElMessage.success('已重新连接，可以继续对话了');
    }, 500);
  } else {
    messages.value.push({
      id: `msg-${Date.now()}`,
      type: 'system',
      content: errorMsg,
      timestamp: new Date()
    });
  }
  
  // 无论如何都要重置状态
  isSending.value = false;
  isAIThinking.value = false;
  systemMessage.value = '发生错误';
  scrollToBottom();
  
  console.log('[状态重置] isSending=false, isAIThinking=false');
  
  // 清除所有定时器
  clearMessageTimeout();
  clearStreamCheck();
};

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || !isConnected.value) {
    return;
  }
  
  // 如果正在发送，先强制结束上一个流
  if (isSending.value) {
    console.warn('[发送消息] 检测到上一个请求还在处理，强制结束');
    forceEndStream();
    // 等待一小段时间确保状态清理完成
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  
  // 确保有conversationId，如果没有先创建
  if (!conversationId.value) {
    try {
      // 使用选中的模型创建对话
      const response = await characterApi.createConversation({
        roleId: roleId.value, // 保持字符串类型
        modelName: selectedModel.value, // 传递选中的模型
        enableAudio: false // 不需要后端生成音频，我们使用角色本身的音频
      });
      
      if (response.data.success) {
        const data = response.data.data;
        conversationId.value = data.conversationId; // 后端返回的是字符串，直接使用
        sessionStorage.setItem(`stream-conversation-${roleId.value}`, data.conversationId);
        console.log('创建对话成功，使用模型：', selectedModel.value);
      } else {
        throw new Error(response.data.message || '创建对话失败');
      }
    } catch (error) {
      console.error('创建对话失败:', error);
      ElMessage.error('创建对话失败，请重试');
      return;
    }
  }
  
  const messageText = inputMessage.value.trim();
  inputMessage.value = '';
  
  stopAudioPlayback();
  
  // 添加用户消息到列表
  messages.value.push({
    id: `msg-${Date.now()}`,
    type: 'user',
    content: messageText,
    timestamp: new Date()
  });
  
  isAIThinking.value = true;
  systemMessage.value = `${currentCharacter.value?.name || 'AI'} 正在思考...`;
  
  console.log('[发送消息] 设置isSending=true');
  
  // 构建请求消息（注意：WebSocket使用conversationId而不是roleId）
  const requestData = {
    conversationId: conversationId.value,  // 使用已创建的对话ID
    message: messageText,
    enableWebSearch: enableWebSearch.value,
    enableAudio: enableAudio.value  // 建议：如果后端TTS有问题，可以先设为false
    // 不需要roleId，因为对话已经关联了角色
  };
  
  console.log('[发送WebSocket消息]', requestData);
  
  // 发送到WebSocket
  if (ws.value && ws.value.readyState === WebSocket.OPEN) {
    ws.value.send(JSON.stringify(requestData));
    isSending.value = true;
    
    // 记录发送时间，用于调试
    console.log(`[发送成功] 时间: ${new Date().toLocaleTimeString()}`);
  } else {
    ElMessage.error('WebSocket未连接');
    isAIThinking.value = false;
    isSending.value = false;
    console.log('[状态重置] WebSocket未连接，isSending=false');
  }
  
  scrollToBottom();
};

// 音频播放管理
const startAudioPlayback = (message) => {
  if (!message.audioSegments || message.audioSegments.length === 0) {
    return;
  }
  
  stopAudioPlayback();
  
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
    currentPlayingMessageId.value = null;
    currentPlayingIndex.value = 0;
    isPlaying.value = false;
    systemMessage.value = '准备就绪';
    return;
  }
  
  const segment = audioQueue.value.shift();
  
  if (!segment.audioUrl) {
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

// 启动消息超时定时器
const startMessageTimeout = () => {
  clearMessageTimeout();
  messageTimeoutTimer = setTimeout(() => {
    // 超时处理：强制结束当前流式消息
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.type === 'assistant' && lastMessage.isStreaming) {
      console.warn('消息接收超时，强制结束流式输出');
      lastMessage.isStreaming = false;
      
      // 渲染Markdown
      if (lastMessage.content) {
        lastMessage.renderedContent = renderMarkdown(lastMessage.content);
      }
      
      // 添加系统提示消息
      messages.value.push({
        id: `msg-${Date.now()}`,
        type: 'system',
        content: '消息接收超时，可能网络不稳定或服务器繁忙',
        timestamp: new Date()
      });
    }
    
    // 重置状态
    isSending.value = false;
    isAIThinking.value = false;
    systemMessage.value = '准备就绪';
    
    ElMessage.warning('消息接收超时，请重试');
  }, MESSAGE_TIMEOUT);
};

// 清除消息超时定时器
const clearMessageTimeout = () => {
  if (messageTimeoutTimer) {
    clearTimeout(messageTimeoutTimer);
    messageTimeoutTimer = null;
  }
};

// 启动流状态检测
const startStreamCheck = () => {
  clearStreamCheck();
  hasCompleteSentence = false;
  
  let checkCount = 0; // 添加检查计数器
  const MAX_CHECKS = 60; // 最多检查60次（30秒）
  
  streamCheckTimer = setInterval(() => {
    checkCount++;
    
    // 超过最大检查次数，强制结束
    if (checkCount >= MAX_CHECKS) {
      console.error('[流超时] 已检查30秒，强制结束');
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage && lastMessage.type === 'assistant' && lastMessage.isStreaming) {
        lastMessage.isStreaming = false;
        if (lastMessage.content) {
          lastMessage.renderedContent = renderMarkdown(lastMessage.content);
        }
      }
      isSending.value = false;
      isAIThinking.value = false;
      systemMessage.value = '准备就绪';
      clearMessageTimeout();
      clearStreamCheck();
      ElMessage.error('消息接收超时，请刷新页面');
      return;
    }
    
    // 检查是否有正在流式输出的消息
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.type === 'assistant' && lastMessage.isStreaming) {
      const now = Date.now();
      const timeSinceLastContent = now - lastContentTime;
      
      // 决定超时时间：如果有完整句子，使用较短的超时
      const timeout = hasCompleteSentence ? QUICK_END_TIMEOUT : STREAM_IDLE_TIMEOUT;
      
      // 如果超过超时时间没有新内容，认为流已经结束
      if (timeSinceLastContent > timeout) {
        const reason = hasCompleteSentence ? '检测到完整句子' : '长时间无新内容';
        console.warn(`[流结束检测] ${reason}，${timeout}ms没有新内容，强制结束流`);
        
        // 强制结束流式输出
        lastMessage.isStreaming = false;
        
        // 渲染Markdown
        if (lastMessage.content) {
          lastMessage.renderedContent = renderMarkdown(lastMessage.content);
        }
        
        // 重置状态
        isSending.value = false;
        isAIThinking.value = false;
        systemMessage.value = '准备就绪';
        
        console.log('[状态重置] 流结束检测触发，isSending=false');
        
        // 清除定时器
        clearMessageTimeout();
        clearStreamCheck();
      }
    } else {
      // 如果没有流式消息了，停止检查
      clearStreamCheck();
    }
  }, 500); // 每0.5秒检查一次，更快响应
};

// 清除流状态检测定时器
const clearStreamCheck = () => {
  if (streamCheckTimer) {
    clearInterval(streamCheckTimer);
    streamCheckTimer = null;
  }
  hasCompleteSentence = false;
};

// 强制结束流
const forceEndStream = () => {
  console.log('[强制结束] 用户手动停止流式输出');
  
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage && lastMessage.type === 'assistant' && lastMessage.isStreaming) {
    // 强制结束流式输出
    lastMessage.isStreaming = false;
    
    // 渲染Markdown
    if (lastMessage.content) {
      lastMessage.renderedContent = renderMarkdown(lastMessage.content);
    }
  }
  
  // 重置所有状态
  isSending.value = false;
  isAIThinking.value = false;
  systemMessage.value = '准备就绪';
  
  console.log('[状态重置] 强制结束，isSending=false');
  
  // 清除所有定时器
  clearMessageTimeout();
  clearStreamCheck();
  
  ElMessage.warning('已强制停止AI回复');
};

// 加载角色信息
const loadCharacterInfo = async () => {
  if (!roleId.value) return;
  
  try {
    const response = await characterApi.getCharacterDetail(roleId.value);
    if (response.data.success) {
      currentCharacter.value = response.data.data;
      console.log('加载的角色信息:', currentCharacter.value);
      
      // 打印开场白信息用于调试
      const greeting = currentCharacter.value.greetingMessage || currentCharacter.value.greeting_message;
      const greetingAudioUrl = currentCharacter.value.greetingAudioUrl || currentCharacter.value.greeting_audio_url;
      console.log('开场白文本:', greeting);
      console.log('开场白音频:', greetingAudioUrl);
    }
  } catch (error) {
    console.error('加载角色信息失败:', error);
    ElMessage.error('加载角色信息失败');
  }
};

// 仅显示开场白（不创建对话）
const showCharacterGreeting = () => {
  if (!currentCharacter.value) return;
  
  const greeting = currentCharacter.value.greetingMessage || currentCharacter.value.greeting_message;
  const greetingAudioUrl = currentCharacter.value.greetingAudioUrl || currentCharacter.value.greeting_audio_url;
  
  if (greeting) {
    const greetingMessage = {
      id: `msg-greeting-${Date.now()}`,
      type: 'assistant',
      content: greeting,
      isStreaming: false,
      renderedContent: renderMarkdown(greeting),
      audioSegments: [],
      timestamp: new Date()
    };
    
    // 如果有开场白音频，添加到消息中
    if (greetingAudioUrl) {
      greetingMessage.audioSegments = [{
        index: 0,
        text: greeting,
        audioUrl: greetingAudioUrl,
        groupId: 'greeting'
      }];
      
      // 自动播放开场白音频（开场白音频是预先生成的，始终播放）
      console.log('[开场白] 检测到音频URL，准备播放:', greetingAudioUrl);
      setTimeout(() => {
        startAudioPlayback(greetingMessage);
      }, 500);
    } else {
      console.log('[开场白] 没有音频URL');
    }
    
    messages.value.push(greetingMessage);
    scrollToBottom();
  }
};

// 加载对话历史
const fetchConversationHistory = async () => {
  try {
    const response = await characterApi.getConversationHistory();
    if (response.data.success) {
      conversationHistory.value = response.data.data || [];
    }
  } catch (error) {
    console.error('加载对话历史失败:', error);
  }
};

// 切换对话
const handleSwitchConversation = async (convId) => {
  if (!convId) return;
  
  // 停止当前音频播放
  stopAudioPlayback();
  
  // 清空当前消息
  messages.value = [];
  
  // 更新conversationId（保持字符串类型）
  conversationId.value = convId;
  
  // 从会话列表中获取该会话的角色信息
  const targetConversation = conversationHistory.value.find(c => c.id === convId);
  if (targetConversation) {
    // 更新选中的模型（如果会话中有模型信息）
    if (targetConversation.modelName) {
      selectedModel.value = targetConversation.modelName;
    }
    // 更新当前角色信息
    if (targetConversation.conversationRole) {
      currentCharacter.value = targetConversation.conversationRole;
      console.log('切换会话，更新角色信息:', currentCharacter.value);
    } else if (targetConversation.roleId) {
      // 如果只有roleId，则加载角色详情
      try {
        const response = await characterApi.getCharacterDetail(targetConversation.roleId);
        if (response.data.success) {
          currentCharacter.value = response.data.data;
          console.log('切换会话，加载角色详情:', currentCharacter.value);
        }
      } catch (error) {
        console.error('加载角色详情失败:', error);
      }
    }
  }
  
  sessionStorage.setItem(`stream-conversation-${roleId.value}`, convId);
  
  // 更新系统消息
  systemMessage.value = '正在加载历史消息...';
  
  // 加载历史消息
  try {
    const response = await characterApi.getMessagesByConversationId(convId);
    if (response.data.success) {
      const data = response.data.data;
      
      // 如果没有历史消息，显示开场白
      if (!data || data.length === 0) {
        // 显示角色的开场白
        if (currentCharacter.value) {
          const greeting = currentCharacter.value.greetingMessage || currentCharacter.value.greeting_message;
          const greetingAudioUrl = currentCharacter.value.greetingAudioUrl || currentCharacter.value.greeting_audio_url;
          
          if (greeting) {
            const greetingMessage = {
              id: `msg-greeting-${Date.now()}`,
              type: 'assistant',
              content: greeting,
              isStreaming: false,
              renderedContent: renderMarkdown(greeting),
              audioSegments: greetingAudioUrl ? [{
                index: 0,
                text: greeting,
                audioUrl: greetingAudioUrl,
                groupId: 'greeting'
              }] : [],
              timestamp: new Date()
            };
            messages.value.push(greetingMessage);
          }
        }
      } else {
        // 转换历史消息格式
        messages.value = data.map((msg) => ({
          id: msg.id || `msg-${Date.now()}-${Math.random()}`,
          content: msg.content,
          type: msg.role === 'USER' ? 'user' : 'assistant',
          timestamp: new Date(msg.createdAt || msg.created_at || Date.now()),
          audioSegments: msg.audioSegments || [],
          renderedContent: msg.role === 'ASSISTANT' ? renderMarkdown(msg.content) : null
        }));
      }
      
      scrollToBottom();
      systemMessage.value = '准备就绪';
    }
  } catch (error) {
    console.error('加载对话消息失败:', error);
    ElMessage.error('加载对话消息失败');
    systemMessage.value = '加载失败';
  }
};

// 删除对话
const handleDeleteConversation = async (convId) => {
  stopAudioPlayback();
  
  try {
    await ElMessageBox.confirm(
      '此操作将永久删除该对话, 是否继续?',
      '警告',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    await characterApi.deleteConversation(convId);
    ElMessage.success('对话删除成功！');
    
    const index = conversationHistory.value.findIndex(c => c.id === convId);
    if (index !== -1) {
      conversationHistory.value.splice(index, 1);
    }
    
    if (conversationId.value === convId) {
      conversationId.value = null;
      messages.value = [];
      sessionStorage.removeItem(`stream-conversation-${roleId.value}`);
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除对话失败:', error);
      ElMessage.error('删除对话失败');
    }
  }
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
  if (!messageListEl.value) return;
  
  clearTimeout(scrollCheckTimer);
  scrollCheckTimer = setTimeout(() => {
    const { scrollTop, scrollHeight, clientHeight } = messageListEl.value;
    const isAtBottom = scrollHeight - scrollTop - clientHeight < 100;
    userScrolled.value = !isAtBottom;
  }, 100);
};

const scrollToBottom = async () => {
  if (userScrolled.value) return;
  
  await nextTick();
  if (messageListEl.value) {
    messageListEl.value.scrollTop = messageListEl.value.scrollHeight;
  }
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

// 获取头像URL
const getAvatarUrl = (url) => {
  return url || new URL('../assets/placeholder.svg', import.meta.url).href;
};

// 获取用户头像
const getUserAvatar = () => {
  // 优先使用用户设置的头像，否则使用默认占位图
  return authStore.user?.avatar || new URL('../assets/placeholder.svg', import.meta.url).href;
};

// 监听路由变化
watch(() => route.params.roleId, async (newRoleId, oldRoleId) => {
  if (newRoleId && newRoleId !== oldRoleId) {
    stopAudioPlayback();
    messages.value = [];
    conversationId.value = null;
    sessionStorage.removeItem(`stream-conversation-${oldRoleId}`);
    
    // 重新加载角色信息
    await loadCharacterInfo();
    
    // 仅显示开场白（不创建对话）
    showCharacterGreeting();
    
    // 重新建立WebSocket连接
    if (ws.value) {
      ws.value.close();
    }
    initWebSocket();
  }
});

// 角色助手相关方法
const openAssistantPanel = () => {
  assistantStep.value = "initial";
  roleBrief.value = null;
  researchTasks.value = [];
  isAssistantPanelVisible.value = true;
};

const handleGenerateBrief = async () => {
  if (!conversationId.value) {
    ElMessage.warning('请先进行对话后再生成角色草稿');
    return;
  }
  
  isAssistantLoading.value = true;
  try {
    const response = await characterApi.generateRoleBrief(conversationId.value, enableWebSearch.value);
    if (response.data && response.data.success) {
      roleBrief.value = response.data.data;
      assistantStep.value = "brief_generated";
      ElMessage.success("角色草稿已生成！");
    } else {
      throw new Error(response.data.message || "生成草稿失败");
    }
  } catch (error) {
    ElMessage.error(
        error.response?.data?.message || "生成草稿失败，请稍后再试。"
    );
  } finally {
    isAssistantLoading.value = false;
  }
};

const handlePreviewTasks = async () => {
  if (!conversationId.value) {
    ElMessage.warning('无法获取对话ID');
    return;
  }
  
  isAssistantLoading.value = true;
  try {
    const response = await characterApi.getResearchTasks(conversationId.value);
    researchTasks.value = response.data.data.tasks;
    assistantStep.value = "tasks_previewed";
  } catch (error) {
    ElMessage.error(
        error.response?.data?.message || "预览任务失败，请稍后再试。"
    );
  } finally {
    isAssistantLoading.value = false;
  }
};

const handleConfirmCreation = async (isDeep) => {
  if (!conversationId.value) {
    ElMessage.warning('无法获取对话ID');
    return;
  }
  
  isAssistantLoading.value = true;
  const payload = {
    conversationId: conversationId.value,
    deepResearch: isDeep,
    overrideName: roleBrief.value.name,
    description: roleBrief.value.description,
    personaPrompt: roleBrief.value.personaPrompt,
    greetingMessage: roleBrief.value.greetingMessage,
    avatarUrl: roleBrief.value.avatarUrl,
    voiceType: roleBrief.value.voiceType,
  };
  if (isDeep) {
    payload.researchQueries = researchTasks.value
        .filter((t) => t.enabled)
        .map((t) => t.query);
  }
  try {
    const response = await characterApi.confirmRoleCreation(payload);
    ElMessage.success(`角色 "${response.data.data.name}" 创建成功！`);
    isAssistantPanelVisible.value = false;
  } catch (error) {
    ElMessage.error(
        error.response?.data?.message || "创建角色失败，请稍后再试。"
    );
  } finally {
    isAssistantLoading.value = false;
  }
};

const triggerFileInput = () => {
  fileInput.value.click();
};

const handleImageUpload = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  isUploading.value = true;
  try {
    const response = await characterApi.uploadImage(file);
    if (response.data && response.data.success) {
      const imageUrl = response.data.message;
      if (roleBrief.value) {
        roleBrief.value.avatarUrl = imageUrl;
      }
      ElMessage.success("图片上传成功！");
    } else {
      throw new Error(response.data.message || "上传失败");
    }
  } catch (error) {
    ElMessage.error(error.message || "图片上传失败");
  } finally {
    isUploading.value = false;
    event.target.value = "";
  }
};

const handleImageGeneration = async () => {
  if (!roleBrief.value || !roleBrief.value.description) {
    ElMessage.warning("请先填写角色描述，AI需要根据描述来生成头像");
    return;
  }
  isGeneratingImage.value = true;
  try {
    const prompt = `为一位名叫"${roleBrief.value.name}"的角色生成一张头像。角色特征：${roleBrief.value.description}`;
    const response = await characterApi.generateImage(prompt);
    
    if (response.data && response.data.success && response.data.data.imageUrls.length > 0) {
      const imageUrl = response.data.data.imageUrls[0];
      roleBrief.value.avatarUrl = imageUrl;
      ElMessage.success("头像生成成功！");
    } else {
      throw new Error(response.data.message || "未能获取到生成的图片URL");
    }
  } catch (error) {
    console.error("生成头像失败:", error);
    ElMessage.error(error.response?.data?.message || "生成头像失败，请稍后再试。");
  } finally {
    isGeneratingImage.value = false;
  }
};

const fetchVoiceList = async () => {
  try {
    const response = await characterApi.getVoiceList();
    voiceList.value = response.data;
  } catch (error) {
    console.error("获取声音列表失败:", error);
    ElMessage.error("无法加载声音列表");
  }
};

const previewVoice = () => {
  if (currentPreviewAudio) {
    currentPreviewAudio.pause();
  }
  if (!roleBrief.value || !roleBrief.value.voiceType) return;
  const selectedVoice = voiceList.value.find(
      (voice) => voice.voice_type === roleBrief.value.voiceType
  );
  if (selectedVoice && selectedVoice.url) {
    currentPreviewAudio = new Audio(selectedVoice.url);
    currentPreviewAudio.play();
  }
};

// 生命周期
onMounted(async () => {
  isLoading.value = true;
  
  try {
    // 1. 加载可用模型列表
    await fetchAvailableModels();
    
    // 2. 加载角色信息（必须先有角色信息才能显示开场白）
    await loadCharacterInfo();
    
    // 3. 加载历史对话列表
    fetchConversationHistory();
    
    // 4. 检查是否有保存的对话
    const savedConversationId = sessionStorage.getItem(`stream-conversation-${roleId.value}`);
    if (savedConversationId) {
      // 有历史对话，切换到该对话（会锁定模型）
      await handleSwitchConversation(savedConversationId);
    } else {
      // 没有对话，仅显示开场白（不创建对话）
      showCharacterGreeting();
    }
    
    // 5. 初始化WebSocket连接
    initWebSocket();
    
    hasError.value = false;
    systemMessage.value = '准备就绪';
  } catch (error) {
    console.error('初始化失败:', error);
    hasError.value = true;
    errorMessage.value = error.message || '加载失败，请刷新重试';
  } finally {
    isLoading.value = false;
  }
});

onUnmounted(() => {
  stopAudioPlayback();
  clearMessageTimeout();
  clearStreamCheck();
  if (ws.value) {
    ws.value.close();
    ws.value = null;
  }
  clearTimeout(scrollCheckTimer);
});
</script>

<style scoped>
/* 容器布局 */
.chat-container {
  display: flex;
  height: 100vh;
  background-color: #1a1a1a;
}

.chat-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 头部样式 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  background-color: #2d2d2d;
  border-bottom: 1px solid #404040;
}

.character-info {
  display: flex;
  align-items: center;
  gap: 1rem;
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

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.name {
  margin: 0;
  color: #e5e7eb;
  font-size: 1.25rem;
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
  color: #e5e7eb;
}

/* 消息列表区域 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 1.5rem;
  scroll-behavior: smooth;
  background-color: #1a1a1a;
}

.loading-messages,
.error-messages {
  text-align: center;
  padding: 2rem;
  color: #9ca3af;
}

.error-messages {
  color: #ef4444;
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

.message-bubble-wrapper {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-item.user .message-bubble-wrapper {
  justify-content: flex-end;
}

/* 消息头像 */
.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-avatar.user {
  order: 2;
}

/* 消息气泡 */
.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
}

.assistant .message-bubble {
  background: linear-gradient(135deg, #374151, #4b5563);
  color: #e5e7eb;
  border-bottom-left-radius: 4px;
}

.user .message-bubble {
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: white;
  border-bottom-right-radius: 4px;
}

.system .message-bubble {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid #7f1d1d;
  color: #fca5a5;
  max-width: 100%;
  text-align: center;
}

/* 消息内容 */
.message-content {
  line-height: 1.6;
}

/* 流式文本 */
.streaming-text {
  position: relative;
}

.cursor-blink {
  animation: blink 1s infinite;
  color: #60a5fa;
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
  margin: 0.5rem 0;
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

/* 音频控制 */
.audio-controls {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.audio-play-btn,
.audio-stop-btn {
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid #3b82f6;
  color: #60a5fa;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: all 0.2s;
}

.audio-play-btn:hover,
.audio-stop-btn:hover {
  background: rgba(59, 130, 246, 0.2);
}

.audio-playing {
  display: flex;
  align-items: center;
  gap: 12px;
}

.audio-progress {
  color: #60a5fa;
  font-size: 0.875rem;
}

/* 消息时间戳 */
.message-time {
  margin-top: 6px;
  font-size: 0.75rem;
  color: #6b7280;
  opacity: 0.7;
}

/* AI思考指示器 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  margin-left: 48px;
}

.typing-dot {
  width: 8px;
  height: 8px;
  background: #60a5fa;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% { 
    transform: translateY(0); 
    opacity: 0.7;
  }
  30% { 
    transform: translateY(-10px); 
    opacity: 1;
  }
}

/* 模型选择器 */
.model-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background-color: #1e293b;
  border-radius: 8px;
  transition: opacity 0.3s;
}

.model-selector.locked {
  opacity: 0.7;
}

.model-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

.model-icon {
  font-size: 18px;
}

.lock-icon {
  font-size: 12px;
  opacity: 0.7;
}

.model-buttons {
  display: flex;
  gap: 8px;
}

.model-btn {
  position: relative;
  padding: 6px 14px;
  background-color: #334155;
  border: 2px solid transparent;
  border-radius: 6px;
  color: #e2e8f0;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.model-btn:hover:not(.disabled) {
  background-color: #475569;
  border-color: #3b82f6;
}

.model-btn.active {
  background-color: #1e40af;
  border-color: #3b82f6;
  color: white;
}

.model-btn.disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.model-btn .check-mark {
  color: #10b981;
  font-weight: bold;
}

/* 底部输入区域 */
.chat-footer {
  padding: 1rem 1.5rem;
  background-color: #2d2d2d;
  border-top: 1px solid #404040;
}

.footer-options {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  margin-bottom: 1rem;
}

.system-message {
  flex: 1;
  margin: 0;
  color: #9ca3af;
  font-size: 0.875rem;
}

.search-toggle-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #e5e7eb;
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

/* 滚动条样式 */
.message-list::-webkit-scrollbar {
  width: 8px;
}

.message-list::-webkit-scrollbar-track {
  background: #1f2937;
  border-radius: 4px;
}

.message-list::-webkit-scrollbar-thumb {
  background: #4b5563;
  border-radius: 4px;
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: #6b7280;
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-header {
    padding: 1rem;
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .footer-options {
    flex-wrap: wrap;
  }
  
  .input-area {
    flex-direction: column;
  }
  
  .send-btn {
    width: 100%;
  }
}

/* === 角色创建助手样式 === */
.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.assistant-trigger-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background-color: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0.5rem 0.75rem;
  border-radius: 6px;
  transition: all 0.2s;
  font-size: 0.9rem;
}

.assistant-trigger-btn:hover {
  background-color: rgba(59, 130, 246, 0.2);
  border-color: var(--primary-color);
  color: var(--primary-color);
}

.assistant-icon {
  font-size: 1.1rem;
  line-height: 1;
}

.assistant-text {
  font-weight: 500;
  white-space: nowrap;
}

.assistant-panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.assistant-panel {
  width: 100%;
  max-width: 450px;
  height: 100%;
  background-color: var(--bg-secondary);
  box-shadow: -5px 0 15px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.panel-header h3 {
  margin: 0;
  font-size: 1.2rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: var(--text-secondary);
  cursor: pointer;
}

.panel-content {
  padding: 1.5rem;
  overflow-y: auto;
  flex-grow: 1;
}

.panel-loading {
  text-align: center;
  color: var(--text-secondary);
  padding: 2rem;
}

.panel-description {
  color: var(--text-secondary);
  line-height: 1.6;
  font-size: 0.9rem;
  margin-top: 0;
}

.panel-btn-primary,
.panel-btn-secondary {
  width: 100%;
  padding: 0.75rem;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  border: 1px solid var(--primary-color);
}

.panel-btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.panel-btn-primary {
  background-color: var(--primary-color);
  color: white;
}

.panel-btn-secondary {
  background-color: transparent;
  color: var(--primary-color);
  margin-top: 1rem;
}

.panel-btn-link {
  background: none;
  border: none;
  color: var(--primary-color);
  cursor: pointer;
  font-size: 0.875rem;
  padding: 0;
}

.panel-actions {
  margin-top: 1.5rem;
  border-top: 1px solid var(--border-color);
  padding-top: 1.5rem;
}

.form-group {
  margin-bottom: 1.75rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.6rem;
  color: var(--text-secondary);
  font-size: 0.9rem;
  font-weight: 500;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  box-sizing: border-box;
  background-color: var(--bg-main);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: 6px;
  padding: 0.75rem;
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background-color: var(--bg-main);
  border-radius: 6px;
}

.task-query {
  flex-grow: 1;
  border: none;
  background: none;
  color: var(--text-primary);
}

.task-delete-btn {
  background-color: #4a5568;
  border: none;
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  cursor: pointer;
  flex-shrink: 0;
}

/* 头像上传区域 */
.avatar-upload-group {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
}

.avatar-preview {
  width: 88px;
  height: 88px;
  border-radius: 10px;
  object-fit: cover;
  border: 2px solid var(--border-color);
  background-color: var(--bg-main);
  flex-shrink: 0;
  transition: border-color 0.2s;
}

.avatar-upload-group:hover .avatar-preview {
  border-color: var(--primary-color);
}

.upload-inputs {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.upload-inputs input[type="text"] {
  font-size: 0.875rem;
  padding: 0.65rem 0.75rem;
}

.button-group {
  display: flex;
  gap: 0.75rem;
}

.button-group button {
  flex-grow: 1;
}

.btn-upload {
  background-color: #4a5568;
  color: white;
  border: none;
  padding: 0.65rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  text-align: center;
  transition: background-color 0.2s;
}

.btn-upload:hover:not(:disabled) {
  background-color: #6b7280;
}

.btn-generate {
  background-color: #3b82f6;
  color: white;
  border: none;
  padding: 0.65rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 500;
  text-align: center;
  transition: background-color 0.2s;
}

.btn-generate:hover:not(:disabled) {
  background-color: #2563eb;
}

.btn-generate:disabled,
.btn-upload:disabled {
  background-color: #4b5563;
  cursor: not-allowed;
  opacity: 0.5;
}

.force-end-btn {
  padding: 0.75rem 1.5rem;
  background-color: #dc2626;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1rem;
  transition: all 0.2s;
  margin-left: 0.5rem;
}

.force-end-btn:hover {
  background-color: #991b1b;
  transform: translateY(-1px);
}

.footer-options {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

/* TTS声音选择区域 */
.voice-type-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.voice-type-group select {
  flex-grow: 1;
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%239ca3af' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
  background-position: right 0.75rem center;
  background-repeat: no-repeat;
  background-size: 1.25em;
  padding-right: 2.5rem;
}

.btn-preview {
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  width: 44px;
  height: 44px;
  background-color: #4a5568;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  color: white;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}

.btn-preview:hover:not(:disabled) {
  background-color: var(--primary-color-hover);
  border-color: var(--primary-color);
}

.btn-preview:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>

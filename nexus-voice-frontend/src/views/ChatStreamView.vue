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
          <!-- 协议选择器 -->
          <div class="protocol-selector">
            <label class="protocol-label">
              <span class="protocol-icon">🔄</span>
            </label>
            <el-select 
              v-model="streamProtocol" 
              placeholder="选择协议"
              :disabled="isSending"
              class="protocol-select"
              size="small"
              @change="handleProtocolChange"
            >
              <el-option label="WebSocket" value="websocket">
                <span>WebSocket</span>
                <span style="color: #9ca3af; font-size: 12px; margin-left: 8px;">(双向)</span>
              </el-option>
              <el-option label="SSE" value="sse">
                <span>SSE</span>
                <span style="color: #9ca3af; font-size: 12px; margin-left: 8px;">(轻量)</span>
              </el-option>
            </el-select>
          </div>
          
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
                <!-- 文本内容 -->
                <div v-if="message.content" class="message-text">
                  {{ message.content }}
                </div>
                
                <!-- 附件显示（图片/文档等） -->
                <div v-if="message.attachments && message.attachments.length > 0" 
                     class="message-attachments">
                  <div v-for="(attachment, idx) in message.attachments" 
                       :key="idx" 
                       class="attachment-item">
                    <!-- 图片附件 -->
                    <img v-if="attachment.type === 'image'" 
                         :src="attachment.url" 
                         :alt="attachment.name || '图片'"
                         class="attachment-image"
                         @click="openImagePreview(attachment.url)"
                         :title="`${attachment.name} (${formatFileSize(attachment.size)})`" />
                    
                    <!-- 文档附件 -->
                    <a v-else-if="attachment.type === 'document'" 
                       :href="attachment.url"
                       target="_blank"
                       class="attachment-document"
                       :title="`下载 ${attachment.name}`">
                      📄 {{ attachment.name }} ({{ formatFileSize(attachment.size) }})
                    </a>
                    
                    <!-- 其他类型附件 -->
                    <a v-else
                       :href="attachment.url"
                       target="_blank"
                       class="attachment-other"
                       :title="`下载 ${attachment.name}`">
                      📎 {{ attachment.name }} ({{ formatFileSize(attachment.size) }})
                    </a>
                  </div>
                </div>
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
            </label>
            <el-select 
              v-model="selectedModel" 
              placeholder="选择模型"
              :disabled="isModelLocked"
              class="model-select"
              size="small"
              placement="top"
              popper-class="model-select-popper"
            >
              <el-option
                v-for="model in availableModels"
                :key="model.modelKey"
                :label="model.modelName"
                :value="model.modelKey"
              />
            </el-select>
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
        
        <!-- 图片预览区域 -->
        <div v-if="selectedImages.length > 0" class="image-preview-container">
          <div v-for="(img, index) in selectedImages" :key="index" class="image-preview-item">
            <img :src="img.preview" alt="预览图" class="preview-image" />
            <!-- 上传中状态遮罩 -->
            <div v-if="img.uploading" class="uploading-overlay">
              <span class="uploading-text">上传中...</span>
            </div>
            <button v-else @click="removeImage(index)" class="remove-image-btn">×</button>
          </div>
        </div>
        
        <!-- 输入框和发送按钮 -->
        <div class="input-area">
          <!-- 隐藏的图片input -->
          <input 
            ref="imageInput"
            type="file" 
            accept="image/*" 
            multiple 
            @change="handleImageSelect"
            style="display: none;"
          />
          <!-- 图片上传按钮 -->
          <button 
            @click="triggerImageSelect" 
            :disabled="!isConnected || selectedImages.length >= maxImages"
            class="image-upload-btn"
            :title="selectedImages.length >= maxImages ? `最多上传${maxImages}张图片` : '上传图片'"
          >
            📷
          </button>
          <input 
            v-model="inputMessage" 
            @keypress.enter="sendMessage"
            :disabled="!isConnected || isSending"
            placeholder="输入消息，按Enter发送..."
            class="message-input"
          />
          <button 
            @click="sendMessage" 
            :disabled="!isConnected || isSending || (!inputMessage.trim() && selectedImages.length === 0)"
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

    <!-- 图片预览弹窗 -->
    <div 
      v-if="imagePreviewVisible" 
      class="image-preview-overlay"
      @click="closeImagePreview"
    >
      <div class="image-preview-wrapper">
        <img 
          :src="previewImageUrl" 
          alt="图片预览" 
          class="preview-full-image"
          @click.stop
        />
        <button 
          class="preview-close-btn"
          @click="closeImagePreview"
          title="关闭预览"
        >
          ×
        </button>
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
import { createTransport } from '../services/streamTransport';

// 路由和认证
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// 流式传输相关（支持WebSocket和SSE）
const streamProtocol = ref('websocket'); // 'websocket' | 'sse'
const transport = ref(null); // StreamTransport实例
const wsStatus = ref('disconnected'); // disconnected, connecting, connected
const connectionError = ref('');

// 协议统计（用于监控和调试）
const protocolStats = ref({
  websocket: { attempts: 0, success: 0, failures: 0, lastUsed: null },
  sse: { attempts: 0, success: 0, failures: 0, lastUsed: null }
});

// 对话相关
const messages = ref([]);
const inputMessage = ref('');
const isSending = ref(false);
const conversationId = ref(null); // 注意：保持为字符串类型，避免精度丢失
const roleId = computed(() => route.params.roleId); // 从路由获取动态roleId

// 模型选择相关
const availableModels = ref([]); // 可用模型列表
const selectedModel = ref('deepseek:deepseek-v3.1'); // 默认选中DeepSeek V3.1
const isModelLocked = computed(() => !!conversationId.value); // 有对话后锁定模型

// 图片上传相关
const selectedImages = ref([]); // 已选择的图片列表 [{file, url, name, size, mimeType, preview, uploading}]
const imageInput = ref(null); // 图片input引用
const maxImages = 5; // 最大图片数量

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
      // 显示所有可用模型，不再过滤
      availableModels.value = response.data.data;
      console.log('加载到', availableModels.value.length, '个可用模型');
    }
  } catch (error) {
    console.error('加载模型列表失败:', error);
    ElMessage.error('加载模型列表失败');
  }
};

// 图片上传相关方法
const triggerImageSelect = () => {
  imageInput.value?.click();
};

/**
 * 上传单张图片到CDN
 * @param {File} file - 图片文件
 * @returns {Promise<Object>} 上传结果 {url, name, size, mimeType}
 */
const uploadImageToCDN = async (file) => {
  const formData = new FormData();
  formData.append('file', file);
  
  // 从authStore获取token
  const token = authStore.token;
  if (!token) {
    throw new Error('请先登录');
  }
  
  const response = await fetch('http://localhost:8081/api/file/message/upload-image', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  });
  
  if (!response.ok) {
    throw new Error(`上传失败: ${response.statusText}`);
  }
  
  const result = await response.json();
  if (!result.success) {
    throw new Error(result.message || '上传失败');
  }
  
  return result.data;
};

/**
 * 处理图片选择：验证并立即上传到CDN
 */
const handleImageSelect = async (event) => {
  const files = Array.from(event.target.files || []);
  
  // 检查数量限制
  const remainingSlots = maxImages - selectedImages.value.length;
  if (remainingSlots <= 0) {
    ElMessage.warning(`最多只能上传${maxImages}张图片`);
    event.target.value = '';
    return;
  }
  
  const filesToAdd = files.slice(0, remainingSlots);
  
  for (const file of filesToAdd) {
    // 检查文件类型
    if (!file.type.startsWith('image/')) {
      ElMessage.warning(`文件 ${file.name} 不是图片格式`);
      continue;
    }
    
    // 检查文件大小（限制为10MB）
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning(`图片 ${file.name} 大小超过10MB`);
      continue;
    }
    
    // 创建本地预览
    const reader = new FileReader();
    const localPreview = await new Promise((resolve) => {
      reader.onload = (e) => resolve(e.target.result);
      reader.readAsDataURL(file);
    });
    
    // 添加到列表（标记为上传中）
    const imageIndex = selectedImages.value.length;
    selectedImages.value.push({
      file,
      preview: localPreview,
      uploading: true,
      url: null,
      name: file.name,
      size: file.size,
      mimeType: file.type
    });
    
    // 异步上传到CDN
    try {
      ElMessage.info(`正在上传 ${file.name}...`);
      const uploadResult = await uploadImageToCDN(file);
      
      // 更新为CDN URL
      selectedImages.value[imageIndex] = {
        ...selectedImages.value[imageIndex],
        uploading: false,
        url: uploadResult.url,
        preview: uploadResult.url, // 使用CDN URL作为预览
        name: uploadResult.name || file.name,
        size: Number(uploadResult.size) || file.size, // 确保size是数字类型
        mimeType: uploadResult.mimeType || file.type
      };
      
      ElMessage.success(`${file.name} 上传成功`);
    } catch (error) {
      console.error('上传图片失败:', error);
      ElMessage.error(`${file.name} 上传失败：${error.message}`);
      // 移除上传失败的图片
      selectedImages.value.splice(imageIndex, 1);
    }
  }
  
  // 清空input，允许重复选择同一文件
  event.target.value = '';
};

const removeImage = (index) => {
  selectedImages.value.splice(index, 1);
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

// 图片预览弹窗
const imagePreviewVisible = ref(false);
const previewImageUrl = ref('');

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

// 初始化传输层（支持WebSocket和SSE）
const initTransport = () => {
  console.log(`[传输层] 🚀 初始化 ${streamProtocol.value.toUpperCase()} 传输`);
  
  const token = authStore.token;
  if (!token) {
    connectionError.value = '未找到认证Token';
    authStore.logout();
    return;
  }
  
  // 关闭旧连接
  if (transport.value) {
    console.log(`[传输层] 🔌 关闭旧连接`);
    transport.value.close();
    transport.value = null;
  }
  
  try {
    // 创建新的传输实例
    transport.value = createTransport(streamProtocol.value);
    
    // 统计尝试次数
    protocolStats.value[streamProtocol.value].attempts++;
    
    // 绑定状态变更事件
    transport.value.onStatusChange = (status) => {
      console.log(`[${streamProtocol.value.toUpperCase()}] 📡 状态变更: ${status}`);
      wsStatus.value = status;
      
      if (status === 'connected') {
        connectionError.value = '';
        systemMessage.value = '连接成功，可以开始对话了';
        protocolStats.value[streamProtocol.value].success++;
        protocolStats.value[streamProtocol.value].lastUsed = new Date();
        
        // 从sessionStorage恢复conversationId
        const savedConversationId = sessionStorage.getItem(`stream-conversation-${roleId.value}`);
        if (savedConversationId) {
          conversationId.value = savedConversationId;
        }
      } else if (status === 'disconnected') {
        if (!connectionError.value) {
          connectionError.value = '连接已断开';
        }
      }
    };
    
    // 绑定消息事件
    transport.value.onMessage = (data) => {
      console.log(`[${streamProtocol.value.toUpperCase()}] 📨 收到消息: ${data.type}`);
      handleWebSocketMessage(data); // 复用现有消息处理逻辑
    };
    
    // 绑定错误事件
    transport.value.onError = (error) => {
      console.error(`[${streamProtocol.value.toUpperCase()}] ❌ 错误:`, error);
      connectionError.value = error.message || '连接出现错误';
      protocolStats.value[streamProtocol.value].failures++;
      
      // Token认证失败
      if (error.message && error.message.includes('Token认证失败')) {
        setTimeout(() => {
          authStore.logout();
        }, 2000);
      }
    };
    
    // 开始连接
    transport.value.connect(token);
  } catch (error) {
    console.error(`[传输层] ❌ 创建传输实例失败:`, error);
    connectionError.value = '创建连接失败';
    wsStatus.value = 'disconnected';
    protocolStats.value[streamProtocol.value].failures++;
  }
};

// 处理WebSocket消息
const handleWebSocketMessage = (data) => {
  console.log(`[收到消息] 类型: ${data.type}`, data);
  
  // 增强日志：显示消息详情
  if (data.type === 'CONTENT' && data.delta) {
    console.log('[内容增量]', data.delta.substring(0, 100) + (data.delta.length > 100 ? '...' : ''));
  }
  
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
  console.log('[开始流式输出] 🚀', data);
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
  console.log('[START] 创建新消息，isStreaming=true，消息ID:', aiMessage.id);
  console.log('[START] 消息数组长度:', messages.value.length);
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
  let lastMessage = messages.value[messages.value.length - 1];
  console.log('[当前消息数组长度]', messages.value.length);
  console.log('[最后消息]', lastMessage);
  console.log('[isStreaming状态]', lastMessage?.isStreaming);
  
  // 如果最后一条消息不是流式消息，或者不存在，创建一个新的
  if (!lastMessage || lastMessage.type !== 'assistant' || !lastMessage.isStreaming) {
    console.warn('[异常] 收到CONTENT但没有活跃的流式消息，创建新消息');
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
    lastMessage = aiMessage;
  }
  
  const delta = data.delta || '';
  lastMessage.content += delta;
  console.log('[累计内容长度]', lastMessage.content.length);
  
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
    console.log('[最终消息内容]', lastMessage.content);
    console.log('[消息长度]', lastMessage.content.length);
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
  // 修改：允许只发送图片或只发送文本
  if ((!inputMessage.value.trim() && selectedImages.value.length === 0) || !isConnected.value) {
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
  const imagesToSend = [...selectedImages.value]; // 保存当前选中的图片
  
  // 检查是否有图片还在上传中
  const hasUploadingImages = imagesToSend.some(img => img.uploading);
  if (hasUploadingImages) {
    ElMessage.warning('请等待图片上传完成');
    return;
  }
  
  inputMessage.value = '';
  selectedImages.value = []; // 清空图片列表
  
  stopAudioPlayback();
  
  // 构建附件列表（用于显示）
  const attachments = imagesToSend.map(img => ({
    type: 'image',
    url: img.url,
    name: img.name,
    size: img.size,
    mimeType: img.mimeType
  }));
  
  // 添加用户消息到列表（包含附件）
  messages.value.push({
    id: `msg-${Date.now()}`,
    type: 'user',
    content: messageText,
    attachments: attachments, // 使用附件对象列表
    attachmentCount: attachments.length,
    timestamp: new Date()
  });
  
  isAIThinking.value = true;
  systemMessage.value = `${currentCharacter.value?.name || 'AI'} 正在思考...`;
  
  console.log('[发送消息] 设置isSending=true');
  
  // 构建请求消息（使用attachmentUrls而非imageUrls）
  const requestData = {
    conversationId: conversationId.value,
    message: messageText || '请看图片', // 如果没有文本，提供默认文本
    enableWebSearch: enableWebSearch.value,
    enableAudio: enableAudio.value
  };
  
  // 添加附件URL（JSON字符串数组格式）
  if (attachments.length > 0) {
    requestData.attachmentUrls = attachments.map(att => JSON.stringify(att));
  }
  
  // 日志输出
  if (attachments.length > 0) {
    console.log('[发送WebSocket消息] 包含', attachments.length, '个附件');
    attachments.forEach((att, idx) => {
      console.log(`  附件${idx + 1}:`, att.name, `(${formatFileSize(att.size)})`);
    });
  }
  console.log('[发送WebSocket消息]', { 
    ...requestData, 
    attachmentUrls: requestData.attachmentUrls ? `${requestData.attachmentUrls.length}个附件` : undefined 
  });
  
  // 使用传输层发送消息
  if (transport.value && transport.value.isConnected()) {
    try {
      console.log(`[${streamProtocol.value.toUpperCase()}] 📤 准备发送消息`);
      transport.value.send(requestData);
      isSending.value = true;
      
      // 记录发送时间
      console.log(`[发送成功] 时间: ${new Date().toLocaleTimeString()}, 协议: ${streamProtocol.value}`);
    } catch (error) {
      console.error(`[${streamProtocol.value.toUpperCase()}] ❌ 发送失败:`, error);
      ElMessage.error('发送消息失败：' + error.message);
      isAIThinking.value = false;
      isSending.value = false;
    }
  } else {
    ElMessage.error(`${streamProtocol.value.toUpperCase()}未连接`);
    isAIThinking.value = false;
    isSending.value = false;
    console.log('[状态重置] 未连接，isSending=false');
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

// 加载角色信息
const loadCharacterInfo = async () => {
  if (!roleId.value) return;
  
  try {
    const response = await characterApi.getCharacterDetail(roleId.value);
    if (response.data.success) {
      currentCharacter.value = response.data.data;
      console.log('加载的角色信息:', currentCharacter.value);
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
      
      // 自动播放开场白音频
      console.log('[开场白] 检测到音频URL，准备播放:', greetingAudioUrl);
      setTimeout(() => {
        startAudioPlayback(greetingMessage);
      }, 500);
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
  
  // 更新conversationId
  conversationId.value = convId;
  
  // 从会话列表中获取该会话的角色信息
  const targetConversation = conversationHistory.value.find(c => c.id === convId);
  if (targetConversation) {
    // 更新选中的模型
    if (targetConversation.modelName) {
      selectedModel.value = targetConversation.modelName;
    }
    // 更新当前角色信息
    if (targetConversation.conversationRole) {
      currentCharacter.value = targetConversation.conversationRole;
    }
  }
  
  sessionStorage.setItem(`stream-conversation-${roleId.value}`, convId);
  systemMessage.value = '正在加载历史消息...';
  
  // 加载历史消息
  try {
    const response = await characterApi.getMessagesByConversationId(convId);
    console.log('=== API返回的原始数据:', response.data);
    if (response.data.success) {
      const data = response.data.data;
      console.log('=== 历史消息数据:', data);
      
      if (!data || data.length === 0) {
        // 显示开场白
        showCharacterGreeting();
      } else {
        // 转换历史消息格式（包含附件）
        messages.value = data.map((msg) => {
          console.log('=== 处理消息:', {
            id: msg.id,
            role: msg.role,
            hasAttachments: !!msg.attachments,
            attachments: msg.attachments,
            attachmentCount: msg.attachmentCount
          });
          
          return {
            id: msg.id || `msg-${Date.now()}-${Math.random()}`,
            content: msg.content,
            type: msg.role === 'USER' ? 'user' : 'assistant',
            timestamp: new Date(msg.createdAt || msg.created_at || Date.now()),
            audioSegments: msg.audioSegments || [],
            renderedContent: msg.role === 'ASSISTANT' ? renderMarkdown(msg.content) : null,
            // 添加附件字段支持
            attachments: msg.attachments || [],
            attachmentCount: msg.attachmentCount || 0
          };
        });
        
        console.log('=== 转换后的messages:', messages.value);
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

// 重新连接
const reconnect = () => {
  console.log(`[传输层] 🔄 手动重连 ${streamProtocol.value.toUpperCase()}`);
  initTransport();
};

// 协议切换处理
const handleProtocolChange = () => {
  if (isSending.value) {
    ElMessage.warning('请等待当前消息完成后再切换协议');
    // 回退选择
    nextTick(() => {
      streamProtocol.value = streamProtocol.value === 'websocket' ? 'sse' : 'websocket';
    });
    return;
  }
  
  console.log(`[协议切换] 🔄 切换到 ${streamProtocol.value.toUpperCase()}`);
  ElMessage.info(`已切换到 ${streamProtocol.value.toUpperCase()} 协议`);
  
  // 重新初始化传输层
  initTransport();
  
  // 保存到localStorage
  localStorage.setItem('preferred_protocol', streamProtocol.value);
};

/**
 * 格式化文件大小
 * @param {number} bytes - 字节数
 * @returns {string} 格式化后的大小
 */
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B';
  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
};

// 渲染Markdown
const renderMarkdown = (content) => {
  if (!content) return '';
  try {
    // 配置marked选项，禁用部分GFM语法
    marked.setOptions({
      breaks: true,           // 将换行符转换为<br>
      gfm: false,             // 禁用GFM语法（包括删除线）
      headerIds: false,       // 禁用标题ID
      mangle: false,          // 禁用邮箱混淆
      sanitize: false         // 不使用内置sanitizer（我们用DOMPurify）
    });
    
    const html = marked(content);
    return DOMPurify.sanitize(html);
  } catch (error) {
    console.error('Markdown渲染失败:', error);
    return content;
  }
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListEl.value) {
      messageListEl.value.scrollTop = messageListEl.value.scrollHeight;
    }
  });
};

// 加载TTS声音列表
const loadVoiceList = async () => {
  try {
    const response = await characterApi.getVoiceList();
    if (response.data.success) {
      voiceList.value = response.data.data || [];
    }
  } catch (error) {
    console.error('加载声音列表失败:', error);
  }
};

// 获取头像URL
const getAvatarUrl = (url) => {
  if (!url) return '/default-avatar.png';
  if (url.startsWith('http')) return url;
  return url;
};

// 获取用户头像
const getUserAvatar = () => {
  const user = authStore.user;
  if (user && user.avatarUrl) {
    return user.avatarUrl;
  }
  return '/default-user-avatar.png';
};

// 打开图片预览
const openImagePreview = (imageUrl) => {
  previewImageUrl.value = imageUrl;
  imagePreviewVisible.value = true;
};

// 关闭图片预览
const closeImagePreview = () => {
  imagePreviewVisible.value = false;
  previewImageUrl.value = '';
};

// 返回角色列表
const goBack = () => {
  router.push('/characters');
};

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '';
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now - date;
  
  // 小于1分钟
  if (diff < 60000) {
    return '刚刚';
  }
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  }
  // 小于1天
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`;
  }
  // 显示日期时间
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
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
        type: 'warning'
      }
    );
    
    // 执行删除
    const response = await characterApi.deleteConversation(convId);
    if (response.data.success) {
      ElMessage.success('删除成功');
      
      // 如果删除的是当前对话，清空消息列表
      if (conversationId.value === convId) {
        conversationId.value = null;
        messages.value = [];
        sessionStorage.removeItem(`stream-conversation-${roleId.value}`);
        showCharacterGreeting();
      }
      
      // 刷新对话列表
      await fetchConversationHistory();
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除对话失败:', error);
      ElMessage.error('删除失败');
    }
  }
};

// 启动消息超时定时器
const startMessageTimeout = () => {
  clearMessageTimeout();
  messageTimeoutTimer = setTimeout(() => {
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.type === 'assistant' && lastMessage.isStreaming) {
      console.warn('消息接收超时，强制结束流式输出');
      lastMessage.isStreaming = false;
      
      if (lastMessage.content) {
        lastMessage.renderedContent = renderMarkdown(lastMessage.content);
      }
      
      messages.value.push({
        id: `msg-${Date.now()}`,
        type: 'system',
        content: '消息接收超时，可能网络不稳定或服务器繁忙',
        timestamp: new Date()
      });
    }
    
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

// 清除流状态检测定时器
const clearStreamCheck = () => {
  if (streamCheckTimer) {
    clearInterval(streamCheckTimer);
    streamCheckTimer = null;
  }
  hasCompleteSentence = false;
};

// 启动流状态检测
const startStreamCheck = () => {
  clearStreamCheck();
  hasCompleteSentence = false;
  
  let checkCount = 0;
  const MAX_CHECKS = 60;
  
  streamCheckTimer = setInterval(() => {
    checkCount++;
    
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
    
    const lastMessage = messages.value[messages.value.length - 1];
    if (lastMessage && lastMessage.type === 'assistant' && lastMessage.isStreaming) {
      const now = Date.now();
      const timeSinceLastContent = now - lastContentTime;
      
      const timeout = hasCompleteSentence ? QUICK_END_TIMEOUT : STREAM_IDLE_TIMEOUT;
      
      if (timeSinceLastContent > timeout) {
        const reason = hasCompleteSentence ? '检测到完整句子' : '长时间无新内容';
        console.warn(`[流结束检测] ${reason}，${timeout}ms没有新内容，强制结束流`);
        
        lastMessage.isStreaming = false;
        
        if (lastMessage.content) {
          lastMessage.renderedContent = renderMarkdown(lastMessage.content);
        }
        
        isSending.value = false;
        isAIThinking.value = false;
        systemMessage.value = '准备就绪';
        
        clearMessageTimeout();
        clearStreamCheck();
      }
    } else {
      clearStreamCheck();
    }
  }, 500);
};

// 强制结束流
const forceEndStream = () => {
  console.log('[强制结束] 用户手动停止流式输出');
  
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
  
  ElMessage.warning('已强制停止AI回复');
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
    
    // 5. 从localStorage恢复用户上次选择的协议
    const savedProtocol = localStorage.getItem('preferred_protocol');
    if (savedProtocol === 'websocket' || savedProtocol === 'sse') {
      streamProtocol.value = savedProtocol;
      console.log(`[传输层] 📥 恢复用户首选协议: ${savedProtocol.toUpperCase()}`);
    }
    
    // 6. 初始化传输层（支持WebSocket和SSE）
    initTransport();
    
    // 7. 加载TTS声音列表
    loadVoiceList();
    
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
  console.log('[传输层] 🗑️ 组件销毁，清理资源');
  
  // 清理传输层
  if (transport.value) {
    transport.value.close();
    transport.value = null;
  }
  
  // 清理定时器
  clearMessageTimeout();
  clearStreamCheck();
  if (scrollCheckTimer) {
    clearTimeout(scrollCheckTimer);
  }
  
  // 停止音频播放
  stopAudioPlayback();
  
  // 清理试听音频
  if (currentPreviewAudio) {
    currentPreviewAudio.pause();
    currentPreviewAudio = null;
  }
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

/* 协议选择器 */
.protocol-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background-color: rgba(0, 0, 0, 0.3);
  border-radius: 8px;
}

.protocol-label {
  display: flex;
  align-items: center;
  color: #94a3b8;
  font-size: 14px;
  white-space: nowrap;
}

.protocol-icon {
  font-size: 16px;
}

.protocol-select {
  width: 140px;
}

/* 协议选择器下拉框深色主题适配 */
.protocol-select :deep(.el-input__wrapper) {
  background-color: #1e293b;
  border-color: #334155;
  box-shadow: none;
}

.protocol-select :deep(.el-input__inner) {
  color: #e2e8f0;
}

.protocol-select :deep(.el-select__caret) {
  color: #94a3b8;
}

.protocol-select :deep(.el-input__wrapper:hover) {
  border-color: #475569;
}

.protocol-select :deep(.el-input__wrapper.is-focus) {
  border-color: #3b82f6;
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

/* ==================== 模型选择器样式 ==================== */
.model-selector {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.model-label {
  display: flex;
  align-items: center;
  gap: 0.3rem;
  color: #e5e7eb;
  font-size: 0.9rem;
}

.model-icon {
  font-size: 1.2rem;
}

.lock-icon {
  font-size: 1rem;
  opacity: 0.7;
}

/* Element Plus 下拉框样式 */
.model-select {
  min-width: 150px;
}

.model-select :deep(.el-input__wrapper) {
  background-color: #374151 !important;
  border: 1px solid #4b5563 !important;
  box-shadow: none !important;
}

.model-select :deep(.el-input__inner) {
  color: #e5e7eb !important;
}

.model-select :deep(.el-input__wrapper:hover) {
  border-color: #6b7280 !important;
}

.model-select :deep(.el-input__wrapper.is-focus) {
  border-color: #10b981 !important;
}

.model-select :deep(.el-select__caret) {
  color: #9ca3af !important;
}

.model-selector.locked .model-select {
  opacity: 0.6;
}


/* ==================== 图片上传相关样式 ==================== */
.image-upload-btn {
  padding: 0.5rem 0.8rem;
  background-color: #374151;
  border: 1px solid #4b5563;
  border-radius: 8px;
  color: #e5e7eb;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 1.2rem;
}

.image-upload-btn:hover:not(:disabled) {
  background-color: #4b5563;
  border-color: #6b7280;
  transform: scale(1.1);
}

.image-upload-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.image-preview-container {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem;
  background-color: #1f2937;
  border-radius: 8px;
  margin-bottom: 0.5rem;
  overflow-x: auto;
}

.image-preview-item {
  position: relative;
  flex-shrink: 0;
}

.preview-image {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 2px solid #4b5563;
}

.remove-image-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background-color: #ef4444;
  color: white;
  border: 2px solid white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.2rem;
  line-height: 1;
  transition: all 0.2s;
}

.remove-image-btn:hover {
  background-color: #dc2626;
  transform: scale(1.1);
}

.uploading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(2px);
}

.uploading-text {
  color: #fff;
  font-size: 0.875rem;
  font-weight: 500;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* 消息中的附件显示 */
.message-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-top: 0.75rem;
}

.attachment-item {
  position: relative;
}

.attachment-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.attachment-image:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
}

.attachment-document,
.attachment-other {
  display: inline-flex;
  align-items: center;
  padding: 0.5rem 0.75rem;
  background: #374151;
  border-radius: 6px;
  text-decoration: none;
  color: #10b981;
  font-size: 0.875rem;
  transition: all 0.2s ease;
  border: 1px solid #4b5563;
}

.attachment-document:hover,
.attachment-other:hover {
  background: #4b5563;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.message-text {
  word-wrap: break-word;
  line-height: 1.6;
}

/* 图片预览弹窗 */
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.9);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease-in;
}

.image-preview-wrapper {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.preview-full-image {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
  animation: zoomIn 0.3s ease-out;
}

@keyframes zoomIn {
  from {
    transform: scale(0.8);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

.preview-close-btn {
  position: fixed;
  top: 20px;
  right: 20px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  border: 2px solid rgba(255, 255, 255, 0.3);
  font-size: 32px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 2001;
}

.preview-close-btn:hover {
  background-color: rgba(255, 255, 255, 0.2);
  border-color: white;
  transform: scale(1.1);
}
</style>

<!-- 全局样式：覆盖Element Plus下拉菜单（不使用scoped） -->
<style>
/* 下拉菜单深色主题 */
.model-select-popper.el-select-dropdown {
  background-color: #1f2937 !important;
  border: 1px solid #4b5563 !important;
}

.model-select-popper .el-select-dropdown__item {
  color: #e5e7eb !important;
  background-color: transparent !important;
}

.model-select-popper .el-select-dropdown__item:hover {
  background-color: #374151 !important;
}

.model-select-popper .el-select-dropdown__item.selected {
  color: #10b981 !important;
  font-weight: 600 !important;
}

.model-select-popper .el-select-dropdown__item.selected::after {
  content: "✓";
  margin-left: 8px;
  color: #10b981;
}

/* 滚动条样式 */
.model-select-popper .el-select-dropdown__wrap {
  background-color: #1f2937 !important;
}

.model-select-popper .el-scrollbar__thumb {
  background-color: #4b5563 !important;
}
</style>

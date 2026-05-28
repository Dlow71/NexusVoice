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
              @error="handleImageElementError"
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
            <svg class="assistant-button-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M9.94 15.5A2 2 0 0 0 8.5 14.06l-6.14-1.58a.5.5 0 0 1 0-.96L8.5 9.94A2 2 0 0 0 9.94 8.5l1.58-6.14a.5.5 0 0 1 .96 0L14.06 8.5A2 2 0 0 0 15.5 9.94l6.14 1.58a.5.5 0 0 1 0 .96L15.5 14.06a2 2 0 0 0-1.44 1.44l-1.58 6.14a.5.5 0 0 1-.96 0z" />
              <path d="M20 3v4" />
              <path d="M22 5h-4" />
            </svg>
            <span class="assistant-text">角色生成</span>
          </button>
          <button
              @click="goToRagPage"
              class="knowledge-base-btn"
              title="管理知识库"
          >
            <svg class="assistant-button-icon" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4.75 6.75A2.75 2.75 0 0 1 7.5 4h11.75v15.25H7.5a2.75 2.75 0 0 0-2.75 2.75V6.75Z" />
              <path d="M7.5 4v15.25" />
              <path d="M12 8.25h4.75" />
            </svg>
            <span class="assistant-text">知识库</span>
          </button>
          <!-- 协议选择器 -->
          <div class="protocol-selector" :class="wsStatusClass">
            <el-select 
              v-model="streamProtocol" 
              placeholder="选择协议"
              :disabled="isSending"
              class="protocol-select glass-select"
              size="small"
              popper-class="glass-popper"
              @change="handleProtocolChange"
            >
              <el-option label="WebSocket" value="websocket">
                <span>WebSocket</span>
                <span style="color: var(--text-subtle); font-size: 12px; margin-left: 8px;">(双向)</span>
              </el-option>
              <el-option label="SSE" value="sse">
                <span>SSE</span>
                <span style="color: var(--text-subtle); font-size: 12px; margin-left: 8px;">(轻量)</span>
              </el-option>
            </el-select>
            <div class="protocol-connection" :class="wsStatusClass">
              <span class="protocol-status-dot"></span>
              <svg v-if="isConnected" class="protocol-status-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M2.5 8.75A15.7 15.7 0 0 1 12 5.5a15.7 15.7 0 0 1 9.5 3.25" />
                <path d="M5.75 12.5A10.8 10.8 0 0 1 12 10.5a10.8 10.8 0 0 1 6.25 2" />
                <path d="M9.25 16a5.3 5.3 0 0 1 5.5 0" />
              </svg>
              <svg v-else class="protocol-status-icon" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M2.5 8.75A15.7 15.7 0 0 1 12 5.5a15.7 15.7 0 0 1 9.5 3.25" />
                <path d="M5.75 12.5A10.8 10.8 0 0 1 12 10.5a10.8 10.8 0 0 1 6.25 2" />
                <path d="M9.25 16a5.3 5.3 0 0 1 5.5 0" />
                <path d="M4 4 20 20" />
              </svg>
              <span class="protocol-status-text">{{ wsStatusText }}</span>
            </div>
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
                   :alt="currentCharacter?.name || 'AI'"
                   @error="handleImageElementError" />
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
                         @error="handleImageElementError"
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
                <div v-if="message.hasThinking"
                     class="thinking-panel"
                     :class="{ 'thinking-panel-streaming': message.isThinkingStreaming }">
                  <button class="thinking-panel-header"
                          @click="toggleThinkingExpanded(message)">
                    <span>{{ message.isThinkingStreaming ? '深度思考中' : '思考过程' }}</span>
                    <span class="thinking-panel-toggle">{{ message.thinkingExpanded ? '收起' : '展开' }}</span>
                  </button>
                  <div v-if="message.thinkingExpanded"
                       class="thinking-panel-body">
                    <div v-if="message.isStreaming"
                         class="thinking-raw-text">{{ message.reasoningContent }}</div>
                    <div v-else
                         class="thinking-rendered-content"
                         v-html="message.renderedReasoningContent || message.reasoningContent"></div>
                  </div>
                </div>
                <!-- 流式输出时显示原始文本 -->
                <div v-if="message.isStreaming" class="streaming-text">
                  {{ message.content }}
                  <span class="cursor-blink">│</span>
                </div>
                <!-- 完成后显示渲染的Markdown -->
                <div v-else class="rendered-content"
                     @click="handleCitationReferenceClick"
                     v-html="message.renderedContent || message.content"></div>

                <div v-if="!message.isStreaming && message.citations && message.citations.length > 0"
                     class="citation-panel">
                  <div class="citation-panel-title">来源依据</div>
                  <div v-for="citation in message.citations"
                       :key="citation.id || citation.label"
                       :id="getCitationDomId(message, citation)"
                       class="citation-card">
                    <div class="citation-card-header">
                      <span class="citation-label">{{ citation.label }}</span>
                      <span class="citation-file">{{ citation.fileName || '未命名文件' }}</span>
                    </div>
                    <div class="citation-card-meta">
                      <span v-if="shouldShowKnowledgeBaseName(message.citations)">
                        知识库：{{ citation.knowledgeBaseName || '未命名知识库' }}
                      </span>
                      <span v-if="citation.location">
                        片段：{{ citation.location }}
                      </span>
                      <span v-if="citation.matchedQueries && citation.matchedQueries.length > 0">
                        命中：{{ citation.matchedQueries.join(' / ') }}
                      </span>
                    </div>
                    <div v-if="citation.snippetIsHtml"
                         class="citation-snippet citation-snippet-html"
                         :class="{ 'citation-snippet-collapsed': isCitationCollapsible(citation) && !isCitationExpanded(message, citation) }"
                         v-html="citation.renderedSnippet"></div>
                    <div v-else-if="citation.snippet"
                         class="citation-snippet"
                         :class="{ 'citation-snippet-collapsed': isCitationCollapsible(citation) && !isCitationExpanded(message, citation) }">
                      {{ citation.snippet }}
                    </div>
                    <div class="citation-actions">
                      <button v-if="isCitationCollapsible(citation)"
                              class="citation-action-btn"
                              @click="toggleCitationExpanded(message, citation)">
                        {{ isCitationExpanded(message, citation) ? '收起' : '展开更多' }}
                      </button>
                      <button v-if="citation.knowledgeBaseId && citation.fileId && citation.location"
                              class="citation-action-btn secondary"
                              @click="openCitationPreview(citation)">
                        查看原文上下文
                      </button>
                    </div>
                  </div>
                </div>
                
                <!-- 音频播放控制 -->
                <div v-if="message.audioSegments && message.audioSegments.length > 0" 
                     class="audio-controls">
                  <button v-if="currentPlayingMessageId !== message.id" 
                          @click="startAudioPlayback(message)"
                          class="audio-play-btn">
                     <el-icon class="audio-icon"><VideoPlay /></el-icon> <span>播放音频 ({{ message.audioSegments.length }}段)</span>
                  </button>
                  <div v-else class="audio-playing">
                    <button @click="stopAudioPlayback" class="audio-stop-btn">
                      <el-icon class="audio-icon"><VideoPause /></el-icon> <span>停止</span>
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
              <img :src="getUserAvatar()" alt="User" @error="handleImageElementError" />
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
        <div class="footer-options-bar">
          <!-- Row 1: 模型 + 功能开关 chip -->
          <div class="footer-row footer-row-primary">
            <p class="system-message">{{ systemMessage }}</p>
            
            <!-- 模型选择器 -->
            <div class="model-selector" :class="{ 'locked': isModelLocked }">
              <div class="model-selector-star" aria-hidden="true">
                <svg viewBox="0 0 24 24">
                  <path d="M9.94 15.5A2 2 0 0 0 8.5 14.06l-6.14-1.58a.5.5 0 0 1 0-.96L8.5 9.94A2 2 0 0 0 9.94 8.5l1.58-6.14a.5.5 0 0 1 .96 0L14.06 8.5A2 2 0 0 0 15.5 9.94l6.14 1.58a.5.5 0 0 1 0 .96L15.5 14.06a2 2 0 0 0-1.44 1.44l-1.58 6.14a.5.5 0 0 1-.96 0z" />
                  <path d="M20 3v4" />
                  <path d="M22 5h-4" />
                </svg>
              </div>
              <div class="model-selector-body">
              <el-select 
                v-model="selectedModel" 
                placeholder="选择模型"
                :disabled="isModelLocked"
                class="model-select glass-select"
                size="small"
                placement="top"
                popper-class="glass-popper"
              >
                <el-option
                  v-for="model in availableModels"
                  :key="model.modelKey"
                  :label="model.modelName"
                  :value="model.modelKey"
                />
              </el-select>
              </div>
            </div>

            <!-- Chip 开关组 -->
            <div class="chip-group">
              <button 
                class="chip-toggle" 
                :class="{ active: enableWebSearch }" 
                @click="enableWebSearch = !enableWebSearch"
              >🌐 联网搜索</button>
              <button 
                class="chip-toggle" 
                :class="{ active: enableAudio }" 
                @click="enableAudio = !enableAudio"
              >🔊 音频回复</button>
              <button 
                class="chip-toggle" 
                :class="{ active: enableRag, disabled: knowledgeBases.length === 0 }" 
                @click="knowledgeBases.length > 0 && (enableRag = !enableRag)"
              >📚 知识库RAG</button>
              <button 
                class="chip-toggle" 
                :class="{ active: deepThinkingEnabled, disabled: !selectedModelSupportsThinking || isSending }" 
                @click="selectedModelSupportsThinking && !isSending && (deepThinkingEnabled = !deepThinkingEnabled)"
                :title="selectedModelSupportsThinking ? '当前模型支持深度思考' : '当前模型不支持深度思考'"
              >🧠 深度思考</button>
              <button
                class="chip-toggle chip-settings-trigger"
                :class="{ active: advancedPanelVisible }"
                @click.stop="toggleAdvancedPanel"
                :disabled="isSending"
                title="打开高级设置"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M10 3h4" />
                  <path d="M12 3v6" />
                  <path d="M5 9h14" />
                  <path d="M7 9v6" />
                  <path d="M17 9v10" />
                  <path d="M3 15h8" />
                  <path d="M13 19h8" />
                </svg>
                高级设置
              </button>
              <el-tooltip
                placement="top"
                effect="light"
                popper-class="context-tooltip-popper"
              >
                <template #content>
                  <div class="context-tooltip">
                    <div class="context-tooltip-title">上下文占用</div>
                    <div class="context-tooltip-line">已用 {{ contextTokenDetails.used }} tokens / {{ contextTokenDetails.window }} tokens</div>
                    <div class="context-tooltip-line">占比 {{ contextRingPercentLabel }}</div>
                    <div class="context-tooltip-line">策略 {{ contextStrategyLabel }}</div>
                    <div class="context-tooltip-line" v-if="contextSnapshot.compactedMessages > 0">压缩 {{ contextSnapshot.compactedMessages }} 条历史</div>
                    <div class="context-tooltip-line" v-if="contextSnapshot.usedCompactSummary">摘要 {{ contextSnapshot.compactSummaryUpdated ? '本轮已刷新' : '已启用' }}</div>
                    <div class="context-tooltip-line" v-if="contextSnapshot.compactSummaryTokens > 0">摘要占用 {{ contextSnapshot.compactSummaryTokens }} tokens</div>
                    <div class="context-tooltip-line" v-if="contextSnapshot.historyTokens > 0">历史占用 {{ contextSnapshot.historyTokens }} tokens</div>
                    <div class="context-tooltip-line" v-if="contextSnapshot.remainingTokens > 0">剩余预算约 {{ contextSnapshot.remainingTokens }} tokens</div>
                  </div>
                </template>
                <button
                  class="context-ring-button"
                  :style="{ '--ring-progress': `${contextRingPercent}%` }"
                  type="button"
                  :aria-label="`上下文占用 ${contextRingPercentLabel}`"
                >
                  <span class="context-ring-core"></span>
                </button>
              </el-tooltip>
            </div>
          </div>

          <!-- Row 2: RAG 下拉选项（仅 RAG 开启时显示） -->
          <div class="footer-row footer-row-rag" v-show="enableRag">
            <div class="rag-selector">
              <el-select
                v-model="selectedKnowledgeBaseIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="选择知识库"
                class="rag-select glass-select"
                size="small"
                popper-class="glass-popper"
                :disabled="knowledgeBases.length === 0 || isSending"
              >
                <el-option
                  v-for="kb in knowledgeBases"
                  :key="kb.id"
                  :label="`${kb.name} (${kb.documentCount || 0})`"
                  :value="String(kb.id)"
                />
              </el-select>
            </div>
            <div class="rag-selector">
              <el-select
                v-model="ragGroundingMode"
                placeholder="RAG回答模式"
                class="rag-select glass-select"
                size="small"
                popper-class="glass-popper"
                :disabled="!enableRag || isSending"
              >
                <el-option label="精确回答" value="STRICT" />
                <el-option label="扩展回答" value="FLEXIBLE" />
              </el-select>
            </div>
          </div>

        </div>

        <!-- 图片预览区域 -->
        <div v-if="selectedImages.length > 0" class="image-preview-container">
          <div v-for="(img, index) in selectedImages" :key="index" class="image-preview-item">
            <img :src="img.preview" alt="预览图" class="preview-image" @error="handleImageElementError" />
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
          
          <!-- ASR语音录音按钮 -->
          <!-- 空闲状态：显示麦克风按钮 -->
          <button 
            v-if="recordingState === RecordingState.IDLE"
            @click="startRecording"
            :disabled="!isConnected || isSending"
            class="voice-record-btn"
            title="点击开始录音"
          >
            🎤
          </button>
          
          <!-- 录音中：显示停止和取消按钮 -->
          <div v-else-if="recordingState === RecordingState.RECORDING" class="recording-controls">
            <button @click="stopRecording" class="stop-recording-btn" title="停止录音">
              ⏹️ {{ formatDuration(recordingDuration) }}
            </button>
            <button @click="cancelRecording" class="cancel-recording-btn" title="取消录音">
              ❌
            </button>
            <div class="recording-indicator">
              <span class="recording-dot"></span>
              <span>录音中...</span>
            </div>
          </div>
          
          <!-- 识别中：显示处理状态 -->
          <div v-else-if="recordingState === RecordingState.PROCESSING" class="processing-indicator">
            <span class="processing-spinner">⏳</span>
            <span>识别中...</span>
          </div>
          
          <el-input 
            v-model="inputMessage" 
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 6 }"
            @keydown.enter="handleEnter"
            :disabled="!isConnected || isSending || recordingState !== RecordingState.IDLE"
            :placeholder="recordingState === RecordingState.IDLE ? '输入消息，按 Enter 发送，Shift+Enter 换行...' : '等待语音识别...'"
            class="message-input"
            resize="none"
          />
          <button 
            @click="sendMessage" 
            :disabled="!isConnected || isSending || (!inputMessage.trim() && selectedImages.length === 0)"
            class="send-btn"
          >
            <el-icon><Promotion /></el-icon>
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
                <img v-if="roleBrief.avatarUrl" :src="roleBrief.avatarUrl" alt="头像预览" class="avatar-preview" @error="handleImageElementError">
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
              <div class="task-content">
                <input type="text" v-model="task.query" class="task-query" placeholder="搜索关键词" />
                <span v-if="task.rationale" class="task-rationale">{{ task.rationale }}</span>
              </div>
              <button
                  @click="researchTasks.splice(index, 1)"
                  class="task-delete-btn"
                  title="删除此任务"
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
          @error="handleImageElementError"
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

    <div v-if="citationPreviewVisible"
         class="image-preview-overlay citation-preview-overlay"
         @click="closeCitationPreview">
      <div class="citation-preview-modal" @click.stop>
        <div class="citation-preview-header">
          <div>
            <h3 class="citation-preview-title">{{ citationPreviewData?.fileName || '原文上下文' }}</h3>
            <p class="citation-preview-meta">
              <span v-if="citationPreviewData?.knowledgeBaseName">知识库：{{ citationPreviewData.knowledgeBaseName }}</span>
              <span v-if="citationPreviewData?.resolvedLocation">命中片段：{{ citationPreviewData.resolvedLocation }}</span>
            </p>
          </div>
          <button class="preview-close-btn" @click="closeCitationPreview" title="关闭预览">×</button>
        </div>

        <div v-if="citationPreviewLoading" class="citation-preview-loading">正在加载原文上下文...</div>
        <div v-else-if="citationPreviewError" class="citation-preview-error">{{ citationPreviewError }}</div>
        <div v-else class="citation-preview-body">
          <div v-for="segment in citationPreviewData?.segments || []"
               :key="segment.page"
               class="citation-context-segment"
               :class="{ 'citation-context-hit': segment.hit }">
            <div class="citation-context-label">
              <span>片段 {{ segment.page }}</span>
              <span v-if="segment.hit" class="citation-context-hit-tag">命中</span>
            </div>
            <div v-if="segment.renderedContent"
                 class="citation-context-content citation-snippet-html"
                 v-html="segment.renderedContent"></div>
            <div v-else class="citation-context-content">{{ segment.content }}</div>
          </div>
        </div>
      </div>
    </div>

    <div
      v-if="advancedPanelVisible"
      class="advanced-drawer-backdrop"
      @click.self="advancedPanelVisible = false"
    >
      <aside class="advanced-drawer" ref="advancedPanelRef">
        <div class="advanced-drawer-header">
          <div>
            <p class="advanced-panel-kicker">Chat Runtime Policy</p>
            <h3>高级设置</h3>
          </div>
          <button class="advanced-panel-close" @click="advancedPanelVisible = false">×</button>
        </div>
        <p class="advanced-panel-description">
          调整本会话的生成参数、思考预算和上下文 compact 策略。已有会话会立即保存；新会话会在首次发送后落库。
        </p>
        <div class="advanced-drawer-body">
          <section class="advanced-drawer-section">
            <div class="advanced-section-title">生成参数</div>
            <div class="advanced-slider-card">
              <div class="advanced-slider-header">
                <span>温度</span>
                <strong>{{ runtimePolicyDraft.temperature.toFixed(2) }}</strong>
              </div>
              <el-slider
                v-model="runtimePolicyDraft.temperature"
                :min="0"
                :max="2"
                :step="0.05"
                class="advanced-slider"
              />
            </div>

            <div class="advanced-slider-card">
              <div class="advanced-slider-header">
                <span>Top P</span>
                <strong>{{ runtimePolicyDraft.topP.toFixed(2) }}</strong>
              </div>
              <el-slider
                v-model="runtimePolicyDraft.topP"
                :min="0"
                :max="1"
                :step="0.05"
                class="advanced-slider"
              />
            </div>

            <div class="advanced-slider-card">
              <div class="advanced-slider-header">
                <span>频率惩罚</span>
                <strong>{{ runtimePolicyDraft.frequencyPenalty.toFixed(1) }}</strong>
              </div>
              <el-slider
                v-model="runtimePolicyDraft.frequencyPenalty"
                :min="-2"
                :max="2"
                :step="0.1"
                class="advanced-slider"
              />
            </div>

            <div class="advanced-slider-card">
              <div class="advanced-slider-header">
                <span>存在惩罚</span>
                <strong>{{ runtimePolicyDraft.presencePenalty.toFixed(1) }}</strong>
              </div>
              <el-slider
                v-model="runtimePolicyDraft.presencePenalty"
                :min="-2"
                :max="2"
                :step="0.1"
                class="advanced-slider"
              />
            </div>

            <div class="advanced-inline-grid">
              <div class="advanced-mini-card">
                <span class="advanced-mini-label">最大输出</span>
                <el-input-number
                  v-model="runtimePolicyDraft.maxTokens"
                  :min="128"
                  :max="32000"
                  :step="128"
                  controls-position="right"
                  class="advanced-number"
                />
              </div>
            </div>

            <div class="advanced-pill-group">
              <button
                v-for="effort in reasoningEffortOptions"
                :key="effort.value"
                class="advanced-pill-btn"
                :class="{ active: runtimePolicyDraft.reasoningEffort === effort.value }"
                @click="runtimePolicyDraft.reasoningEffort = effort.value"
              >
                {{ effort.label }}
              </button>
            </div>
          </section>

          <section class="advanced-drawer-section">
            <div class="advanced-section-title">上下文管理</div>
            <div class="advanced-pill-group">
              <button
                v-for="option in contextStrategyOptions"
                :key="option.value"
                class="advanced-pill-btn"
                :class="{ active: runtimePolicyDraft.contextStrategy === option.value }"
                @click="runtimePolicyDraft.contextStrategy = option.value"
              >
                {{ option.label }}
              </button>
            </div>

            <div class="advanced-slider-card">
              <div class="advanced-slider-header">
                <span>保留最近轮数</span>
                <strong>{{ runtimePolicyDraft.recentTurnsToKeep }}</strong>
              </div>
              <el-slider
                v-model="runtimePolicyDraft.recentTurnsToKeep"
                :min="2"
                :max="20"
                :step="1"
                class="advanced-slider"
              />
            </div>

            <div class="advanced-slider-card">
              <div class="advanced-slider-header">
                <span>compact触发阈值</span>
                <strong>{{ (runtimePolicyDraft.compactTriggerRatio * 100).toFixed(0) }}%</strong>
              </div>
              <el-slider
                v-model="runtimePolicyDraft.compactTriggerRatio"
                :min="0.35"
                :max="0.95"
                :step="0.01"
                class="advanced-slider"
              />
            </div>

            <div class="advanced-inline-grid">
              <div class="advanced-mini-card">
                <span class="advanced-mini-label">预留输出</span>
                <el-input-number
                  v-model="runtimePolicyDraft.reservedOutputTokens"
                  :min="256"
                  :max="32000"
                  :step="128"
                  controls-position="right"
                  class="advanced-number"
                />
              </div>
            </div>
          </section>

          <section class="advanced-drawer-section">
            <div class="advanced-section-title">思考模式</div>
            <div class="advanced-pill-group">
              <button
                v-for="option in thinkingModeOptions"
                :key="option.value"
                class="advanced-pill-btn"
                :class="{ active: runtimePolicyDraft.thinkingMode === option.value, disabled: !selectedModelSupportsThinking }"
                :disabled="!selectedModelSupportsThinking"
                @click="selectedModelSupportsThinking && (runtimePolicyDraft.thinkingMode = option.value)"
              >
                {{ option.label }}
              </button>
            </div>

            <div class="advanced-slider-card" :class="{ muted: runtimePolicyDraft.thinkingMode === 'disabled' || !selectedModelSupportsThinking }">
              <div class="advanced-slider-header">
                <span>思考预算</span>
                <strong>{{ runtimePolicyDraft.thinkingBudgetTokens }}</strong>
              </div>
              <el-slider
                v-model="runtimePolicyDraft.thinkingBudgetTokens"
                :min="256"
                :max="8192"
                :step="256"
                :disabled="runtimePolicyDraft.thinkingMode === 'disabled' || !selectedModelSupportsThinking"
                class="advanced-slider"
              />
            </div>
          </section>
        </div>

        <div class="advanced-panel-footer">
          <div class="advanced-panel-hint">
            {{ conversationId ? '当前会话保存后立即生效。' : '当前是新会话，首次发送后会自动保存。' }}
          </div>
          <div class="advanced-panel-actions">
            <button class="advanced-secondary-btn" @click="resetRuntimePolicyDraft">恢复默认</button>
            <button class="advanced-primary-btn" @click="applyAdvancedSettings">保存设置</button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, shallowRef, onMounted, onUnmounted, computed, nextTick, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Promotion, VideoPlay, Microphone, VideoPause } from '@element-plus/icons-vue';
import ConversationSidebar from '../components/ConversationSidebar.vue';
import characterApi from '../services/character';
import ragApi from '../services/rag';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import { createTransport } from '../services/streamTransport';
import asrService from '../services/asr';
import { useAudioRecorder, RecordingState } from '../composables/useAudioRecorder';
import { replaceImageWithStarFallback, withStarFallback } from '../utils/starFallback';

// 路由和认证
const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

// 流式传输相关（支持WebSocket和SSE）
const streamProtocol = ref('websocket'); // 'websocket' | 'sse'
const transport = shallowRef(null); // StreamTransport实例，避免被Vue代理后丢失实例身份
const wsStatus = ref('disconnected'); // disconnected, connecting, connected
const connectionError = ref('');
let transportVersion = 0;

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
const preferredThinkingModel = 'qiniu:deepseek/deepseek-v3.2-251201';
const selectedModelInfo = computed(() => availableModels.value.find(model => model.modelKey === selectedModel.value) || null);
const selectedModelSupportsThinking = computed(() => Boolean(selectedModelInfo.value?.supportsThinking));

const createDefaultRuntimePolicy = () => ({
  temperature: 0.7,
  maxTokens: 2000,
  topP: 1,
  frequencyPenalty: 0,
  presencePenalty: 0,
  thinkingMode: 'disabled',
  showThinking: false,
  thinkingBudgetTokens: 1024,
  reasoningEffort: 'medium',
  contextStrategy: 'AUTO',
  recentTurnsToKeep: 8,
  reservedOutputTokens: 2000,
  compactTriggerRatio: 0.72
});

const createEmptyContextSnapshot = () => ({
  modelKey: '',
  modelContextWindow: 0,
  estimatedInputTokens: 0,
  reservedOutputTokens: 0,
  reservedThinkingTokens: 0,
  reservedRagTokens: 0,
  reservedSearchTokens: 0,
  remainingTokens: 0,
  systemPromptTokens: 0,
  compactSummaryTokens: 0,
  historyTokens: 0,
  totalHistoryMessages: 0,
  includedHistoryMessages: 0,
  compactedMessages: 0,
  usedCompactSummary: false,
  compactSummaryUpdated: false,
  needsCompaction: false,
  appliedContextStrategy: 'WINDOW_ONLY'
});

const runtimePolicy = ref(createDefaultRuntimePolicy());
const runtimePolicyDraft = ref(createDefaultRuntimePolicy());
const contextSnapshot = ref(createEmptyContextSnapshot());
const advancedPanelVisible = ref(false);
const advancedPanelRef = ref(null);
const contextStrategyOptions = [
  { value: 'AUTO', label: '自动' },
  { value: 'WINDOW_ONLY', label: '滑动窗口' },
  { value: 'COMPACT', label: 'Compact' }
];
const thinkingModeOptions = [
  { value: 'disabled', label: '关闭' },
  { value: 'auto', label: '自动' },
  { value: 'enabled', label: '开启' }
];
const reasoningEffortOptions = [
  { value: 'minimal', label: 'Minimal' },
  { value: 'low', label: 'Low' },
  { value: 'medium', label: 'Medium' },
  { value: 'high', label: 'High' },
  { value: 'none', label: 'None' }
];

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
const enableRag = ref(false);
const ragGroundingMode = ref('STRICT');
const deepThinkingEnabled = ref(false);
const defaultThinkingBudgetTokens = 1024;
const knowledgeBases = ref([]);
const selectedKnowledgeBaseIds = ref([]);
const citationExpandedState = ref({});
const citationPreviewVisible = ref(false);
const citationPreviewLoading = ref(false);
const citationPreviewError = ref('');
const citationPreviewData = ref(null);
const RAG_STORAGE_KEY = 'selected-rag-knowledge-bases';
const RAG_GROUNDING_MODE_KEY = 'rag-grounding-mode';

const extractConversationCharacter = (payload) => {
  if (!payload) {
    return null;
  }

  if (payload.conversationRole) {
    return payload.conversationRole;
  }

  if (Array.isArray(payload)) {
    const roleMessage = payload.find(item => item?.conversationRole);
    return roleMessage?.conversationRole || null;
  }

  return null;
};

// 加载可用模型列表
const fetchAvailableModels = async () => {
  try {
    const response = await characterApi.getAvailableModels();
    if (response.data.success && response.data.data) {
      // 显示所有可用模型，不再过滤
      availableModels.value = response.data.data;
      const availableKeys = new Set(availableModels.value.map(model => model.modelKey));
      if (availableKeys.has(preferredThinkingModel) && (!availableKeys.has(selectedModel.value) || selectedModel.value === 'deepseek:deepseek-v3.1')) {
        selectedModel.value = preferredThinkingModel;
      } else if (!availableKeys.has(selectedModel.value) && availableModels.value.length > 0) {
        selectedModel.value = availableModels.value[0].modelKey;
      }
      console.log('加载到', availableModels.value.length, '个可用模型');
    }
  } catch (error) {
    console.error('加载模型列表失败:', error);
    ElMessage.error('加载模型列表失败');
  }
};

const fetchKnowledgeBases = async () => {
  try {
    const response = await ragApi.listKnowledgeBases();
    if (response.data.success && response.data.data) {
      knowledgeBases.value = response.data.data;
      const availableIdSet = new Set(knowledgeBases.value.map(item => String(item.id)));
      const savedIds = JSON.parse(localStorage.getItem(RAG_STORAGE_KEY) || '[]');
      if (Array.isArray(savedIds)) {
        selectedKnowledgeBaseIds.value = savedIds
          .map(item => String(item))
          .filter(item => availableIdSet.has(item));
      }
      const savedGroundingMode = localStorage.getItem(RAG_GROUNDING_MODE_KEY);
      if (savedGroundingMode === 'STRICT' || savedGroundingMode === 'FLEXIBLE') {
        ragGroundingMode.value = savedGroundingMode;
      }
      if (selectedKnowledgeBaseIds.value.length === 0 && knowledgeBases.value.length > 0) {
        selectedKnowledgeBaseIds.value = knowledgeBases.value.map(item => String(item.id));
      }
    }
  } catch (error) {
    console.error('加载知识库失败:', error);
  }
};

const goToRagPage = () => {
  router.push('/rag');
};

watch(selectedKnowledgeBaseIds, (value) => {
  localStorage.setItem(RAG_STORAGE_KEY, JSON.stringify(value));
}, { deep: true });

watch(ragGroundingMode, (value) => {
  const normalizedMode = value === 'FLEXIBLE' ? 'FLEXIBLE' : 'STRICT';
  if (normalizedMode !== value) {
    ragGroundingMode.value = normalizedMode;
    return;
  }
  localStorage.setItem(RAG_GROUNDING_MODE_KEY, normalizedMode);
});

watch(selectedModelSupportsThinking, (supported) => {
  if (!supported) {
    deepThinkingEnabled.value = false;
    runtimePolicy.value = {
      ...runtimePolicy.value,
      thinkingMode: 'disabled',
      showThinking: false
    };
    runtimePolicyDraft.value = {
      ...runtimePolicyDraft.value,
      thinkingMode: 'disabled',
      showThinking: false
    };
  } else {
    applyRuntimePolicyToThinkingState();
  }
}, { immediate: true });

watch(enableRag, (enabled) => {
  if (enabled && selectedKnowledgeBaseIds.value.length === 0 && knowledgeBases.value.length > 0) {
    selectedKnowledgeBaseIds.value = knowledgeBases.value.map(item => String(item.id));
  }
});

watch(deepThinkingEnabled, (enabled) => {
  if (!selectedModelSupportsThinking.value) {
    return;
  }
  const thinkingMode = enabled
    ? (runtimePolicy.value.thinkingMode === 'auto' ? 'auto' : 'enabled')
    : 'disabled';
  runtimePolicy.value = {
    ...runtimePolicy.value,
    thinkingMode,
    showThinking: enabled
  };
});

watch(selectedModel, () => {
  syncRuntimePolicy(runtimePolicy.value, { keepDraft: false });
  if (!conversationId.value) {
    syncContextSnapshot({
      ...createEmptyContextSnapshot(),
      modelKey: selectedModel.value,
      modelContextWindow: Number(selectedModelInfo.value?.contextWindow) || 0,
      reservedOutputTokens: runtimePolicy.value.reservedOutputTokens
    });
  }
});

const sanitizeRuntimePolicy = (source = {}) => {
  const defaults = createDefaultRuntimePolicy();
  const contextWindow = Number(selectedModelInfo.value?.contextWindow) || 0;
  const maxOutputCap = contextWindow > 0 ? Math.max(1024, contextWindow - 512) : 32000;
  const thinkingCap = contextWindow > 0 ? Math.max(1024, Math.floor(contextWindow / 2)) : 8192;
  const normalizeNumber = (value, min, max, fallback) => {
    const num = Number(value);
    if (!Number.isFinite(num)) {
      return fallback;
    }
    return Math.min(Math.max(num, min), max);
  };
  const normalizeRatio = (value, fallback) => {
    const num = Number(value);
    if (!Number.isFinite(num)) {
      return fallback;
    }
    return Math.min(Math.max(num, 0.35), 0.95);
  };

  const thinkingMode = ['disabled', 'auto', 'enabled'].includes(String(source.thinkingMode || defaults.thinkingMode))
    ? String(source.thinkingMode || defaults.thinkingMode)
    : defaults.thinkingMode;
  const contextStrategy = ['AUTO', 'WINDOW_ONLY', 'COMPACT'].includes(String(source.contextStrategy || defaults.contextStrategy))
    ? String(source.contextStrategy || defaults.contextStrategy)
    : defaults.contextStrategy;
  const reasoningEffort = ['minimal', 'low', 'medium', 'high', 'none'].includes(String(source.reasoningEffort || defaults.reasoningEffort))
    ? String(source.reasoningEffort || defaults.reasoningEffort)
    : defaults.reasoningEffort;

  return {
    temperature: normalizeNumber(source.temperature, 0, 2, defaults.temperature),
    maxTokens: Math.round(normalizeNumber(source.maxTokens, 128, maxOutputCap, defaults.maxTokens)),
    topP: normalizeNumber(source.topP, 0, 1, defaults.topP),
    frequencyPenalty: normalizeNumber(source.frequencyPenalty, -2, 2, defaults.frequencyPenalty),
    presencePenalty: normalizeNumber(source.presencePenalty, -2, 2, defaults.presencePenalty),
    thinkingMode,
    showThinking: Boolean(source.showThinking),
    thinkingBudgetTokens: Math.round(normalizeNumber(source.thinkingBudgetTokens, 256, thinkingCap, defaults.thinkingBudgetTokens)),
    reasoningEffort,
    contextStrategy,
    recentTurnsToKeep: Math.round(normalizeNumber(source.recentTurnsToKeep, 2, 20, defaults.recentTurnsToKeep)),
    reservedOutputTokens: Math.round(normalizeNumber(source.reservedOutputTokens, 256, maxOutputCap, defaults.reservedOutputTokens)),
    compactTriggerRatio: normalizeRatio(source.compactTriggerRatio, defaults.compactTriggerRatio)
  };
};

const syncRuntimePolicy = (policy, options = {}) => {
  const normalized = sanitizeRuntimePolicy(policy || {});
  runtimePolicy.value = normalized;
  if (!options.keepDraft) {
    runtimePolicyDraft.value = { ...normalized };
  }
};

const syncContextSnapshot = (snapshot) => {
  contextSnapshot.value = {
    ...createEmptyContextSnapshot(),
    ...(snapshot || {})
  };
};

const formatTokenNumber = (value) => {
  const numeric = Number(value) || 0;
  if (numeric >= 1000) {
    return `${(numeric / 1000).toFixed(numeric >= 10000 ? 0 : 1)}k`;
  }
  return `${numeric}`;
};

const contextTokenDetails = computed(() => {
  const used = Number(contextSnapshot.value.estimatedInputTokens) || 0;
  const windowSize = Number(contextSnapshot.value.modelContextWindow || selectedModelInfo.value?.contextWindow) || 0;
  return {
    used,
    window: windowSize
  };
});

const contextRingPercent = computed(() => {
  const { used, window } = contextTokenDetails.value;
  if (!window) {
    return 0;
  }
  return Math.min(100, Math.max(0, (used / window) * 100));
});

const contextRingPercentLabel = computed(() => `${contextRingPercent.value.toFixed(contextRingPercent.value >= 10 ? 0 : 1)}%`);

const contextRingInnerLabel = computed(() => {
  const { window } = contextTokenDetails.value;
  if (!window) {
    return '--';
  }
  const percent = contextRingPercent.value;
  if (percent < 1) {
    return `${percent.toFixed(1)}%`;
  }
  return `${percent.toFixed(0)}%`;
});

const contextStrategyLabel = computed(() => {
  const current = contextSnapshot.value.appliedContextStrategy || runtimePolicy.value.contextStrategy;
  switch (current) {
    case 'COMPACT':
      return 'Compact';
    case 'WINDOW_ONLY':
      return '滑动窗口';
    default:
      return '自动';
  }
});

const resetRuntimePolicyDraft = () => {
  runtimePolicyDraft.value = sanitizeRuntimePolicy(createDefaultRuntimePolicy());
};

const toggleAdvancedPanel = () => {
  if (isSending.value) {
    return;
  }
  if (!advancedPanelVisible.value) {
    runtimePolicyDraft.value = { ...runtimePolicy.value };
  }
  advancedPanelVisible.value = !advancedPanelVisible.value;
};

const applyRuntimePolicyToThinkingState = () => {
  const supportsThinking = selectedModelSupportsThinking.value;
  const policyThinkingMode = runtimePolicy.value.thinkingMode;
  deepThinkingEnabled.value = supportsThinking && policyThinkingMode !== 'disabled';
};

const loadConversationRuntimeConfig = async (convId) => {
  if (!convId) {
    syncRuntimePolicy(createDefaultRuntimePolicy());
    syncContextSnapshot(createEmptyContextSnapshot());
    return;
  }
  try {
    const response = await characterApi.getConversationRuntimeConfig(convId);
    if (response.data?.success && response.data?.data) {
      syncRuntimePolicy(response.data.data.policy || createDefaultRuntimePolicy());
      syncContextSnapshot(response.data.data.contextSnapshot || createEmptyContextSnapshot());
      applyRuntimePolicyToThinkingState();
    }
  } catch (error) {
    console.error('加载会话运行配置失败:', error);
  }
};

const saveConversationRuntimeConfig = async () => {
  if (!conversationId.value) {
    syncRuntimePolicy(runtimePolicyDraft.value);
    syncContextSnapshot({
      ...contextSnapshot.value,
      modelKey: selectedModel.value,
      modelContextWindow: Number(selectedModelInfo.value?.contextWindow) || 0,
      reservedOutputTokens: runtimePolicy.value.reservedOutputTokens
    });
    applyRuntimePolicyToThinkingState();
    ElMessage.success('设置已保存，会在首次发送后写入会话');
    advancedPanelVisible.value = false;
    return;
  }
  const payload = sanitizeRuntimePolicy(runtimePolicyDraft.value);
  const response = await characterApi.updateConversationRuntimeConfig(conversationId.value, payload);
  if (!response.data?.success || !response.data?.data) {
    throw new Error(response.data?.message || '保存高级设置失败');
  }
  syncRuntimePolicy(response.data.data.policy || payload);
  syncContextSnapshot(response.data.data.contextSnapshot || createEmptyContextSnapshot());
  applyRuntimePolicyToThinkingState();
  advancedPanelVisible.value = false;
};

const applyAdvancedSettings = async () => {
  try {
    await saveConversationRuntimeConfig();
    ElMessage.success('高级设置已更新');
  } catch (error) {
    console.error('保存高级设置失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '保存高级设置失败');
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

const readFileAsDataUrl = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.onload = (e) => resolve(e.target.result);
  reader.onerror = () => reject(reader.error || new Error('读取图片失败'));
  reader.readAsDataURL(file);
});

const createModelImageDataUrl = (sourceDataUrl) => new Promise((resolve, reject) => {
  const image = new Image();
  image.onload = () => {
    const maxDimension = 1600;
    const scale = Math.min(1, maxDimension / Math.max(image.width, image.height));
    const width = Math.max(1, Math.round(image.width * scale));
    const height = Math.max(1, Math.round(image.height * scale));

    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    context.drawImage(image, 0, 0, width, height);
    resolve(canvas.toDataURL('image/jpeg', 0.9));
  };
  image.onerror = () => reject(new Error('处理图片失败'));
  image.src = sourceDataUrl;
});

const prepareModelImageData = async (file, localPreview) => {
  const maxInlineBytes = 2.5 * 1024 * 1024;
  if (file.size <= maxInlineBytes && localPreview.length <= maxInlineBytes * 1.4) {
    return localPreview;
  }

  try {
    return await createModelImageDataUrl(localPreview);
  } catch (error) {
    console.warn('压缩模型图片失败，使用原图data URL:', error);
    return localPreview;
  }
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
    const localPreview = await readFileAsDataUrl(file);
    const modelImageData = await prepareModelImageData(file, localPreview);
    
    // 添加到列表（标记为上传中）
    const imageIndex = selectedImages.value.length;
    selectedImages.value.push({
      file,
      preview: localPreview,
      modelImageData,
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
        modelImageData,
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

const getMessageTimeoutMs = () => (
  enableRag.value && ragGroundingMode.value === 'STRICT'
    ? STRICT_RAG_MESSAGE_TIMEOUT
    : MESSAGE_TIMEOUT
);

const getStreamIdleTimeoutMs = () => (
  enableRag.value && ragGroundingMode.value === 'STRICT'
    ? STRICT_RAG_STREAM_IDLE_TIMEOUT
    : STREAM_IDLE_TIMEOUT
);

// 音频播放相关
const audioQueue = ref([]);
const currentAudio = ref(null);
const currentPlayingMessageId = ref(null);
const currentPlayingIndex = ref(0);
const isPlaying = ref(false);

// 消息超时处理
let messageTimeoutTimer = null;
const MESSAGE_TIMEOUT = 30000; // 30秒超时
const STRICT_RAG_MESSAGE_TIMEOUT = 45000;

// 消息流状态检测
let lastContentTime = null; // 最后收到CONTENT消息的时间
let streamCheckTimer = null; // 检查消息流是否停止的定时器
const STREAM_IDLE_TIMEOUT = 5000; // 5秒没有新内容认为流结束
const QUICK_END_TIMEOUT = 2000; // 如果有完整句子，2秒就结束
const STRICT_RAG_STREAM_IDLE_TIMEOUT = 15000;
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

// ==================== ASR语音识别相关 ====================

/**
 * 初始化录音功能
 * 传入录音完成和错误处理回调
 */
const {
  recordingState,
  recordingDuration,
  startRecording,
  stopRecording,
  cancelRecording,
  isRecordingSupported,
  formatDuration
} = useAudioRecorder({
  maxDuration: 60, // 最大60秒
  onRecordComplete: handleAudioRecorded,
  onError: handleRecordingError
});

/**
 * 处理录音完成后的音频识别
 * @param {Blob} audioBlob - 录制的音频数据
 */
async function handleAudioRecorded(audioBlob) {
  console.log('[ASR] 开始识别音频，大小:', (audioBlob.size / 1024).toFixed(2), 'KB');
  
  try {
    // 调用ASR服务识别语音
    const result = await asrService.transcribe(audioBlob, {
      modelKey: 'siliconflow:telespeech-asr', // 使用默认ASR模型
      enablePunctuation: true,
      enableItn: true
    });
    
    // 识别成功，填充到输入框
    if (result && result.text) {
      const recognizedText = result.text.trim();
      inputMessage.value = recognizedText;
      
      console.log('[ASR] 识别成功:', recognizedText);
      console.log('[ASR] 耗时:', result.transcriptionTimeMs, 'ms');
      console.log('[ASR] 音频时长:', result.audioDurationMs, 'ms');
      
      ElMessage.success({
        message: `识别成功：${recognizedText.substring(0, 30)}${recognizedText.length > 30 ? '...' : ''}`,
        duration: 3000
      });
      
      // 可选：自动发送消息
      // await nextTick();
      // await sendMessage();
    } else {
      throw new Error('识别结果为空');
    }
  } catch (error) {
    console.error('[ASR] 识别失败:', error);
    ElMessage.error(`语音识别失败: ${error.message}`);
  }
}

/**
 * 处理录音错误
 * @param {Error} error - 错误对象
 */
function handleRecordingError(error) {
  console.error('[录音] 发生错误:', error);
  ElMessage.error(error.message);
}

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
  const protocol = streamProtocol.value;
  const currentTransportVersion = ++transportVersion;
  console.log(`[传输层] 🚀 初始化 ${protocol.toUpperCase()} 传输`);
  
  const token = authStore.token;
  if (!token) {
    connectionError.value = '未找到认证Token';
    authStore.logout();
    return;
  }
  
  // 关闭旧连接
  if (transport.value) {
    console.log(`[传输层] 🔌 关闭旧连接`);
    transport.value.onStatusChange = null;
    transport.value.onMessage = null;
    transport.value.onError = null;
    transport.value.close();
    transport.value = null;
  }
  
  try {
    // 创建新的传输实例
    const nextTransport = createTransport(protocol);
    transport.value = nextTransport;
    connectionError.value = '';
    
    // 统计尝试次数
    protocolStats.value[protocol].attempts++;
    
    // 绑定状态变更事件
    nextTransport.onStatusChange = (status) => {
      if (transportVersion !== currentTransportVersion || transport.value !== nextTransport) {
        return;
      }

      console.log(`[${protocol.toUpperCase()}] 📡 状态变更: ${status}`);
      wsStatus.value = status;
      
      if (status === 'connected') {
        connectionError.value = '';
        systemMessage.value = '连接成功，可以开始对话了';
        protocolStats.value[protocol].success++;
        protocolStats.value[protocol].lastUsed = new Date();
        
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
    nextTransport.onMessage = (data) => {
      if (transportVersion !== currentTransportVersion || transport.value !== nextTransport) {
        return;
      }

      console.log(`[${protocol.toUpperCase()}] 📨 收到消息: ${data.type}`);
      handleWebSocketMessage(data); // 复用现有消息处理逻辑
    };
    
    // 绑定错误事件
    nextTransport.onError = (error) => {
      if (transportVersion !== currentTransportVersion || transport.value !== nextTransport) {
        return;
      }

      console.error(`[${protocol.toUpperCase()}] ❌ 错误:`, error);
      connectionError.value = error.message || '连接出现错误';
      protocolStats.value[protocol].failures++;
      
      // Token认证失败
      if (error.message && error.message.includes('Token认证失败')) {
        setTimeout(() => {
          authStore.logout();
        }, 2000);
      }
    };
    
    // 开始连接
    nextTransport.connect(token);
  } catch (error) {
    console.error(`[传输层] ❌ 创建传输实例失败:`, error);
    connectionError.value = '创建连接失败';
    wsStatus.value = 'disconnected';
    protocolStats.value[protocol].failures++;
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
    case 'THINKING_START':
      handleThinkingStartMessage(data);
      break;
    case 'THINKING_DELTA':
      handleThinkingDeltaMessage(data);
      break;
    case 'THINKING_END':
      handleThinkingEndMessage(data);
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
  if (!data?.id && !data?.model) {
    console.log('[忽略伪START] 该消息缺少流标识，视为连接态通知', data);
    if (data?.delta) {
      systemMessage.value = '连接成功，可以开始对话了';
    }
    return;
  }

  console.log('[开始流式输出] 🚀', data);
  isAIThinking.value = false;
  systemMessage.value = `${currentCharacter.value?.name || 'AI'} 正在回复...`;
  
  const aiMessage = {
    id: `msg-${Date.now()}`,
    type: 'assistant',
    content: '',
    isStreaming: true,
    citations: [],
    reasoningContent: '',
    renderedReasoningContent: '',
    hasThinking: false,
    thinkingExpanded: false,
    isThinkingStreaming: false,
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

const ensureStreamingAssistantMessage = (data) => {
  let lastMessage = messages.value[messages.value.length - 1];
  if (!lastMessage || lastMessage.type !== 'assistant' || !lastMessage.isStreaming) {
    console.warn('[异常] 收到流式消息但没有活跃的流式AI消息，创建新消息');
    const aiMessage = {
      id: `msg-${Date.now()}`,
      type: 'assistant',
      content: '',
      isStreaming: true,
      citations: [],
      reasoningContent: '',
      renderedReasoningContent: '',
      hasThinking: false,
      thinkingExpanded: false,
      isThinkingStreaming: false,
      audioSegments: [],
      timestamp: new Date(),
      model: data.model
    };
    messages.value.push(aiMessage);
    lastMessage = aiMessage;
  }
  return lastMessage;
};

// 处理CONTENT消息
const handleContentMessage = (data) => {
  let lastMessage = messages.value[messages.value.length - 1];
  console.log('[当前消息数组长度]', messages.value.length);
  console.log('[最后消息]', lastMessage);
  console.log('[isStreaming状态]', lastMessage?.isStreaming);
  
  // 如果最后一条消息不是流式消息，或者不存在，创建一个新的
  if (!lastMessage || lastMessage.type !== 'assistant' || !lastMessage.isStreaming) {
    lastMessage = ensureStreamingAssistantMessage(data);
  }

  if (lastMessage.isThinkingStreaming) {
    lastMessage.isThinkingStreaming = false;
    lastMessage.thinkingExpanded = false;
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

const handleThinkingStartMessage = (data) => {
  const lastMessage = ensureStreamingAssistantMessage(data);
  lastMessage.hasThinking = true;
  lastMessage.isThinkingStreaming = true;
  lastMessage.thinkingExpanded = true;
  lastContentTime = Date.now();
  startMessageTimeout();
  scrollToBottom();
};

const handleThinkingDeltaMessage = (data) => {
  const lastMessage = ensureStreamingAssistantMessage(data);
  const delta = data.delta || '';
  if (!delta) {
    return;
  }
  lastMessage.hasThinking = true;
  lastMessage.isThinkingStreaming = true;
  lastMessage.thinkingExpanded = true;
  lastMessage.reasoningContent = (lastMessage.reasoningContent || '') + delta;
  lastContentTime = Date.now();
  startMessageTimeout();
  scrollToBottom();
};

const handleThinkingEndMessage = (data) => {
  const lastMessage = ensureStreamingAssistantMessage(data);
  lastMessage.hasThinking = Boolean(lastMessage.reasoningContent);
  lastMessage.isThinkingStreaming = false;
  lastMessage.thinkingExpanded = false;
  lastContentTime = Date.now();
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
    if (data.content) {
      lastMessage.content = data.content;
    }
    console.log('[最终消息内容]', lastMessage.content);
    console.log('[消息长度]', lastMessage.content.length);
    lastMessage.isStreaming = false;
    lastMessage.citations = normalizeCitations(data.citations);
    if (typeof data.reasoningContent === 'string') {
      lastMessage.reasoningContent = data.reasoningContent;
    }
    if (data.contextSnapshot) {
      syncContextSnapshot(data.contextSnapshot);
    }
    lastMessage.hasThinking = Boolean(lastMessage.reasoningContent);
    lastMessage.isThinkingStreaming = false;
    
    // 保存conversationId
    if (data.conversationId) {
      const oldConversationId = conversationId.value;
      conversationId.value = data.conversationId;
      sessionStorage.setItem(`stream-conversation-${roleId.value}`, data.conversationId.toString());
      
      // 如果是新对话，更新左侧列表
      if (!oldConversationId || oldConversationId !== data.conversationId) {
        fetchConversationHistory();
      }
      loadConversationRuntimeConfig(data.conversationId);
    }
    
    // 渲染Markdown
    applyAssistantPresentation(lastMessage);
    
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
  
  // 如果是用户发送的第一条消息收到回复，生成标题
  // 判断条件：有且仅有1条用户消息（过滤掉开场白）
  const userMessageCount = messages.value.filter(m => m.type === 'user').length;
  if (userMessageCount === 1 && conversationId.value) {
    console.log('[检测到首次对话完成，准备生成标题] conversationId:', conversationId.value, 'userMessageCount:', userMessageCount);
    generateConversationTitle();
  }
  
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
        applyAssistantPresentation(msg);
      }
    });
    
    // 添加系统提示
    messages.value.push({
      id: `msg-${Date.now()}`,
      type: 'system',
      content: '检测到后端会话锁卡住，正在自动重连...',
      timestamp: new Date()
    });
    
      // 当前实现已统一收敛到 transport 层，不再保留旧 ws/initWebSocket 分支。
      // 这里仅提示并让用户重新发送，避免混用两套连接状态机。
      connectionError.value = '连接状态已恢复，请重新发送上一条消息';
      ElMessage.warning(connectionError.value);
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
// 处理回车按键（支持 Shift+Enter 换行）
const handleEnter = (e) => {
  if (e.shiftKey) {
    return; // 允许原生换行
  }
  e.preventDefault(); // 阻止默认的回车行为（这会产生换行）
  
  if (!isConnected.value || isSending.value) return;
  if (!inputMessage.value.trim() && selectedImages.value.length === 0) return;
  
  sendMessage();
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

  if (enableRag.value && selectedKnowledgeBaseIds.value.length === 0) {
    ElMessage.warning('已开启知识库RAG，但还没有选择知识库');
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

  const effectiveThinkingMode = deepThinkingEnabled.value && selectedModelSupportsThinking.value
    ? (runtimePolicy.value.thinkingMode === 'auto' ? 'auto' : 'enabled')
    : 'disabled';
  const effectiveRuntimePolicy = sanitizeRuntimePolicy({
    ...runtimePolicy.value,
    thinkingMode: effectiveThinkingMode,
    showThinking: effectiveThinkingMode !== 'disabled'
  });
  syncRuntimePolicy(effectiveRuntimePolicy);
  
  // 构建请求消息（使用attachmentUrls而非imageUrls）
  const requestData = {
    conversationId: conversationId.value,
    message: messageText || '请看图片', // 如果没有文本，提供默认文本
    enableWebSearch: enableWebSearch.value,
    enableAudio: enableAudio.value,
    enableRag: enableRag.value && selectedKnowledgeBaseIds.value.length > 0,
    knowledgeBaseIds: selectedKnowledgeBaseIds.value,
    ragGroundingMode: ragGroundingMode.value,
    temperature: effectiveRuntimePolicy.temperature,
    maxTokens: effectiveRuntimePolicy.maxTokens,
    topP: effectiveRuntimePolicy.topP,
    frequencyPenalty: effectiveRuntimePolicy.frequencyPenalty,
    presencePenalty: effectiveRuntimePolicy.presencePenalty,
    thinkingMode: effectiveRuntimePolicy.thinkingMode,
    showThinking: effectiveRuntimePolicy.showThinking,
    thinkingBudgetTokens: effectiveRuntimePolicy.thinkingMode !== 'disabled'
      ? effectiveRuntimePolicy.thinkingBudgetTokens
      : null,
    reasoningEffort: effectiveRuntimePolicy.reasoningEffort,
    contextStrategy: effectiveRuntimePolicy.contextStrategy,
    recentTurnsToKeep: effectiveRuntimePolicy.recentTurnsToKeep,
    reservedOutputTokens: effectiveRuntimePolicy.reservedOutputTokens,
    compactTriggerRatio: effectiveRuntimePolicy.compactTriggerRatio
  };
  
  // 添加附件URL（JSON字符串数组格式）
  if (attachments.length > 0) {
    requestData.attachmentUrls = attachments.map(att => JSON.stringify(att));

    const modelImageUrls = imagesToSend
      .map(img => img.modelImageData)
      .filter(url => typeof url === 'string' && url.startsWith('data:image/'));

    if (modelImageUrls.length > 0) {
      requestData.imageUrls = modelImageUrls;
    }
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

const diagnoseAudioUrl = async (audioUrl) => {
  if (!audioUrl) {
    return null;
  }

  try {
    const response = await fetch(audioUrl, {
      method: 'HEAD',
      mode: 'cors'
    });
    const contentType = response.headers.get('content-type') || '';

    if (!response.ok) {
      return `音频资源不可用（HTTP ${response.status}）`;
    }

    if (contentType && !contentType.startsWith('audio/')) {
      return `音频资源格式异常（${contentType}）`;
    }

    return null;
  } catch (error) {
    console.warn('音频资源探测失败:', error);
    return null;
  }
};

const buildAudioPlaybackFailureMessage = async (segment) => {
  const diagnosis = await diagnoseAudioUrl(segment.audioUrl);
  if (diagnosis) {
    return diagnosis;
  }

  return `音频片段 ${segment.index + 1} 播放失败，已跳过`;
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
  let playbackFailed = false;

  const handlePlaybackFailure = async (error) => {
    if (playbackFailed) {
      return;
    }

    playbackFailed = true;
    console.warn('音频播放失败:', {
      error,
      audioUrl: segment.audioUrl,
      segment
    });

    const message = await buildAudioPlaybackFailureMessage(segment);
    if (segment.groupId !== 'greeting') {
      ElMessage.warning(message);
    } else {
      systemMessage.value = message;
    }

    currentPlayingIndex.value++;
    playNextSegment();
  };
  
  audio.addEventListener('ended', () => {
    currentPlayingIndex.value++;
    playNextSegment();
  });
  
  audio.addEventListener('error', (e) => {
    void handlePlaybackFailure(e);
  });
  
  audio.play().catch(e => {
    void handlePlaybackFailure(e);
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
      citations: [],
      reasoningContent: '',
      renderedReasoningContent: '',
      hasThinking: false,
      thinkingExpanded: false,
      isThinkingStreaming: false,
      audioSegments: [],
      timestamp: new Date()
    };
    applyAssistantPresentation(greetingMessage);
    
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
      return conversationHistory.value;
    }
  } catch (error) {
    console.error('加载对话历史失败:', error);
  }

  return conversationHistory.value;
};

// 生成对话标题
const generateConversationTitle = async () => {
  if (!conversationId.value) {
    console.warn('[标题生成跳过] 没有conversationId');
    return;
  }
  
  try {
    console.log('[开始生成标题] conversationId:', conversationId.value);
    
    // 使用封装好的API
    const response = await characterApi.generateConversationTitle(conversationId.value);
    console.log('[标题生成响应]', response.data);
    
    // 后端Result类使用code判断成功，200表示成功
    if (response.data.code === 200 && response.data.data) {
      const newTitle = response.data.data;
      console.log('[标题生成成功]', newTitle);
      
      // 更新左侧对话列表中的标题
      const targetConversation = conversationHistory.value.find(
        c => c.id === conversationId.value
      );
      
      if (targetConversation) {
        targetConversation.title = newTitle;
        console.log('[对话列表标题已更新]', newTitle);
      } else {
        // 如果列表中没有这个对话，刷新整个列表
        console.log('[对话不在列表中，刷新整个列表]');
        await fetchConversationHistory();
      }
      
      // 可选：显示成功提示
      // ElMessage.success('标题生成成功');
    } else {
      console.warn('[标题生成失败]', response.data.message);
    }
  } catch (error) {
    // 静默处理错误，不影响聊天功能
    console.error('[标题生成异常]', error);
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
      const data = response.data.data || [];
      console.log('=== 历史消息数据:', data);

      if (!currentCharacter.value) {
        const messageCharacter = extractConversationCharacter(data);
        if (messageCharacter) {
          currentCharacter.value = messageCharacter;
        }
      }
      
      if (!Array.isArray(data) || data.length === 0) {
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
            type: msg.role === 'USER' ? 'user' : (msg.role === 'SYSTEM' ? 'system' : 'assistant'),
            timestamp: new Date(msg.createdAt || msg.created_at || Date.now()),
            audioSegments: msg.audioSegments || [],
            citations: normalizeCitations(msg.citations),
            reasoningContent: msg.reasoningContent || '',
            renderedReasoningContent: '',
            hasThinking: Boolean(msg.reasoningContent),
            thinkingExpanded: false,
            isThinkingStreaming: false,
            // 添加附件字段支持
            attachments: msg.attachments || [],
            attachmentCount: msg.attachmentCount || 0
          };
        }).map((message) => {
          applyAssistantPresentation(message);
          return message;
        });
        
        console.log('=== 转换后的messages:', messages.value);
      }
      
      scrollToBottom();
      await loadConversationRuntimeConfig(convId);
      systemMessage.value = '准备就绪';
    }
  } catch (error) {
    console.error('加载对话消息失败:', error);
    const message = error?.response?.data?.message || error?.message || '加载对话消息失败';
    ElMessage.error(message);
    systemMessage.value = '加载失败';
    // 降级：不要阻断整条自动创建角色流程，至少允许继续在当前会话上操作
    if (messages.value.length === 0) {
      showCharacterGreeting();
    }
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

const normalizeCitations = (citations) => (
  Array.isArray(citations)
    ? citations
      .filter(item => item && (item.label || item.fileName || item.snippet))
      .map((citation) => {
        const snippet = normalizeCitationSnippetForDisplay(citation.snippet || '');
        const snippetIsHtml = shouldRenderCitationAsMarkdown(citation);
        return {
          ...citation,
          snippet,
          snippetIsHtml,
          renderedSnippet: snippetIsHtml ? renderMarkdown(snippet, { preserveEmphasis: true }) : ''
        };
      })
    : []
);

const normalizeCitationSnippetForDisplay = (snippet) => {
  if (!snippet) {
    return '';
  }
  return snippet
    .replace(/^"+|"+$/g, '')
    .replace(/^(#+)([^\s#])/gm, '$1 $2')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
};

const shouldRenderFileAsMarkdown = (fileName) => {
  const normalizedName = String(fileName || '').toLowerCase();
  return ['.md', '.markdown', '.mdx'].some(ext => normalizedName.endsWith(ext));
};

const getCitationStateKey = (message, citation) => `${sanitizeDomId(message?.id || message?.messageId)}:${sanitizeDomId(citation?.id || citation?.label)}`;

const isCitationCollapsible = (citation) => {
  const snippet = citation?.snippet || '';
  return snippet.length > 180 || snippet.split('\n').length > 4;
};

const isCitationExpanded = (message, citation) => Boolean(citationExpandedState.value[getCitationStateKey(message, citation)]);

const toggleCitationExpanded = (message, citation) => {
  const key = getCitationStateKey(message, citation);
  citationExpandedState.value = {
    ...citationExpandedState.value,
    [key]: !citationExpandedState.value[key]
  };
};

const sanitizeDomId = (value) => String(value ?? 'unknown').replace(/[^a-zA-Z0-9_-]/g, '-');

const getCitationDomId = (message, citation) => {
  const messageId = sanitizeDomId(message?.id || message?.messageId || 'message');
  const citationId = sanitizeDomId(citation?.id || citation?.label || 'source');
  return `citation-${messageId}-${citationId}`;
};

const shouldShowKnowledgeBaseName = (citations) => {
  const keys = normalizeCitations(citations)
    .map(citation => citation.knowledgeBaseName || citation.knowledgeBaseId)
    .filter(Boolean);
  return new Set(keys).size > 1;
};

const decorateCitationReferences = (content, citations, messageId) => {
  if (!content) {
    return '';
  }
  const citationMap = new Map(
    normalizeCitations(citations)
      .filter(citation => citation.label)
      .map(citation => [citation.label, getCitationDomId({ id: messageId }, citation)])
  );

  return content.replace(/\[(来源\d+)]/g, (match, label) => {
    const targetId = citationMap.get(label);
    if (!targetId) {
      return match;
    }
    return `<a href="#${targetId}" class="citation-ref" data-citation-target="${targetId}">${label}</a>`;
  });
};

const normalizeMarkdownForDisplay = (content, options = {}) => {
  if (!content) {
    return '';
  }
  const preserveEmphasis = options.preserveEmphasis !== false;
  let normalized = content.replace(/\r\n/g, '\n');

  normalized = trimUnbalancedMarker(normalized, '```', '\n```');
  if (preserveEmphasis) {
    normalized = trimUnbalancedMarker(normalized, '**', '');
    normalized = trimUnbalancedMarker(normalized, '__', '');
    normalized = trimUnbalancedMarker(normalized, '`', '');

    normalized = normalized
      .replace(/\*\*\s+(?=\S)/g, '')
      .replace(/(?<=\S)\s+\*\*/g, '')
      .replace(/__(?=\s+\S)/g, '')
      .replace(/(?<=\S)\s+__/g, '');
  } else {
    normalized = normalized
      .replace(/\*\*/g, '')
      .replace(/__/g, '')
      .replace(/`/g, '');
  }

  return normalized;
};

const trimUnbalancedMarker = (content, marker, completion) => {
  const count = countOccurrences(content, marker);
  if (count % 2 === 0) {
    return content;
  }
  if (completion) {
    return `${content}${completion}`;
  }
  const lastIndex = content.lastIndexOf(marker);
  if (lastIndex < 0) {
    return content;
  }
  return `${content.slice(0, lastIndex)}${content.slice(lastIndex + marker.length)}`;
};

const countOccurrences = (content, marker) => {
  let count = 0;
  let index = 0;
  while (index !== -1) {
    index = content.indexOf(marker, index);
    if (index !== -1) {
      count += 1;
      index += marker.length;
    }
  }
  return count;
};

// 渲染Markdown
const renderMarkdown = (content, options = {}) => {
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
    
    const normalizedContent = normalizeMarkdownForDisplay(content, options);
    const html = marked(normalizedContent);
    return DOMPurify.sanitize(html);
  } catch (error) {
    console.error('Markdown渲染失败:', error);
    return content;
  }
};

const renderAssistantMessage = (message) => {
  if (!message) {
    return '';
  }
  const decoratedContent = decorateCitationReferences(
    message.content,
    message.citations,
    message.id || message.messageId
  );
  return renderMarkdown(decoratedContent, { preserveEmphasis: false });
};

const shouldRenderCitationAsMarkdown = (citation) => shouldRenderFileAsMarkdown(citation?.fileName);

const normalizeCitationPreview = (data) => {
  if (!data) {
    return null;
  }
  const markdownFile = shouldRenderFileAsMarkdown(data.fileName);
  return {
    ...data,
    segments: Array.isArray(data.segments)
      ? data.segments.map(segment => ({
        ...segment,
        renderedContent: markdownFile ? renderMarkdown(normalizeCitationSnippetForDisplay(segment.content || ''), { preserveEmphasis: true }) : ''
      }))
      : []
  };
};

const openCitationPreview = async (citation) => {
  citationPreviewVisible.value = true;
  citationPreviewLoading.value = true;
  citationPreviewError.value = '';
  citationPreviewData.value = null;

  try {
    const response = await ragApi.getCitationContext(
      citation.knowledgeBaseId,
      citation.fileId,
      citation.location,
      1
    );
    if (!response.data?.success) {
      throw new Error(response.data?.message || '加载原文上下文失败');
    }
    citationPreviewData.value = normalizeCitationPreview(response.data.data);
  } catch (error) {
    console.error('加载引用上下文失败:', error);
    citationPreviewError.value = error.response?.data?.message || error.message || '加载原文上下文失败';
  } finally {
    citationPreviewLoading.value = false;
  }
};

const closeCitationPreview = () => {
  citationPreviewVisible.value = false;
  citationPreviewLoading.value = false;
  citationPreviewError.value = '';
  citationPreviewData.value = null;
};

const applyAssistantPresentation = (message) => {
  if (!message || message.type !== 'assistant') {
    return;
  }
  message.citations = normalizeCitations(message.citations);
  message.reasoningContent = message.reasoningContent || '';
  message.hasThinking = Boolean(message.reasoningContent);
  message.renderedReasoningContent = message.hasThinking
    ? renderMarkdown(message.reasoningContent, { preserveEmphasis: true })
    : '';
  if (typeof message.thinkingExpanded !== 'boolean') {
    message.thinkingExpanded = false;
  }
  if (typeof message.isThinkingStreaming !== 'boolean') {
    message.isThinkingStreaming = false;
  }
  if (!message.isStreaming) {
    message.renderedContent = renderAssistantMessage(message);
  }
};

const toggleThinkingExpanded = (message) => {
  if (!message || !message.hasThinking) {
    return;
  }
  message.thinkingExpanded = !message.thinkingExpanded;
};

const handleCitationReferenceClick = (event) => {
  const sourceElement = event.target instanceof Element
    ? event.target
    : event.target?.parentElement;
  const target = sourceElement?.closest?.('[data-citation-target]');
  if (!target) {
    return;
  }
  event.preventDefault();
  const citationElement = document.getElementById(target.dataset.citationTarget);
  if (!citationElement) {
    return;
  }
  window.location.hash = target.dataset.citationTarget;
  citationElement.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  citationElement.classList.remove('citation-card-active');
  void citationElement.offsetWidth;
  citationElement.classList.add('citation-card-active');
  setTimeout(() => {
    citationElement.classList.remove('citation-card-active');
  }, 1600);
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
      const voiceTypes = response.data.data || [];
      voiceList.value = voiceTypes.map((voiceType) => ({
        voice_type: voiceType,
        voice_name: voiceType
      }));

      if (roleBrief.value && !roleBrief.value.voiceType && voiceList.value.length > 0) {
        roleBrief.value.voiceType = voiceList.value[0].voice_type;
      }
    }
  } catch (error) {
    console.error('加载声音列表失败:', error);
  }
};

// 获取头像URL
const getAvatarUrl = (url) => {
  return withStarFallback(url);
};

// 获取用户头像
const getUserAvatar = () => {
  const user = authStore.user;
  return withStarFallback(user?.avatarUrl);
};

const handleImageElementError = (event) => {
  replaceImageWithStarFallback(event.target);
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
        syncRuntimePolicy(createDefaultRuntimePolicy());
        syncContextSnapshot(createEmptyContextSnapshot());
        applyRuntimePolicyToThinkingState();
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
      if (lastMessage.isThinkingStreaming || (lastMessage.hasThinking && !lastMessage.content)) {
        console.warn('思考流仍在进行或刚结束，延长超时等待');
        startMessageTimeout();
        return;
      }
      console.warn('消息接收超时，强制结束流式输出');
      lastMessage.isStreaming = false;
      
      applyAssistantPresentation(lastMessage);
      
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
  }, getMessageTimeoutMs());
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
        applyAssistantPresentation(lastMessage);
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

      const inThinkingPhase = lastMessage.isThinkingStreaming || (lastMessage.hasThinking && !lastMessage.content);
      const timeout = inThinkingPhase
        ? Math.max(getStreamIdleTimeoutMs(), 20000)
        : (hasCompleteSentence ? QUICK_END_TIMEOUT : getStreamIdleTimeoutMs());
      
      if (timeSinceLastContent > timeout) {
        const reason = inThinkingPhase
          ? '思考流长时间无新内容'
          : (hasCompleteSentence ? '检测到完整句子' : '长时间无新内容');
        console.warn(`[流结束检测] ${reason}，${timeout}ms没有新内容，强制结束流`);
        
        lastMessage.isStreaming = false;
        applyAssistantPresentation(lastMessage);
        
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
    
    applyAssistantPresentation(lastMessage);
  }
  
  isSending.value = false;
  isAIThinking.value = false;
  systemMessage.value = '准备就绪';
  
  clearMessageTimeout();
  clearStreamCheck();
  
  ElMessage.warning('已强制停止AI回复');
};

// ==================== 角色创建助手方法 ====================

// 打开助手面板
const openAssistantPanel = () => {
  if (!conversationId.value) {
    ElMessage.warning('请先开始对话再使用角色生成功能');
    return;
  }
  isAssistantPanelVisible.value = true;
  assistantStep.value = 'initial';
};

const extractRoleBriefFromMessageContent = (content) => {
  if (!content || (!content.includes('角色草稿Brief') && !content.includes('角色草稿 (Brief)'))) {
    return null;
  }

  const pickField = (fieldName) => {
    const match = content.match(
      new RegExp(`(?:-|\\*)\\s*\\*\\*${fieldName}\\*\\*:?([\\s\\S]*?)(?=\\n(?:-|\\*)\\s*\\*\\*|\\n\\s*\\*\\*请确认|\\n\\s*###|$)`, 'i')
    );
    return match ? match[1].trim() : '';
  };

  const name = pickField('name');
  const description = pickField('description');
  const personaPrompt = pickField('personaPrompt');
  const greetingMessage = pickField('greetingMessage');
  const avatarUrl = pickField('avatarUrl');
  const voiceType = pickField('voiceType');
  const disclaimers = pickField('disclaimers');

  if (!name || !description || !personaPrompt || !greetingMessage) {
    return null;
  }

  const normalizedAvatarUrl = avatarUrl && !avatarUrl.includes('暂空') ? avatarUrl : '';
  const normalizedVoiceType = voiceType && !voiceType.includes('建议使用') ? voiceType : '';

  return {
    name,
    description,
    personaPrompt,
    greetingMessage,
    avatarUrl: normalizedAvatarUrl,
    voiceType: normalizedVoiceType,
    sources: [],
    disclaimers: disclaimers ? [disclaimers] : []
  };
};

const getLatestGeneratedRoleBriefFromMessages = () => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const message = messages.value[i];
    if (message?.type !== 'assistant' || message?.isStreaming) {
      continue;
    }

    const brief = extractRoleBriefFromMessageContent(message.content);
    if (brief) {
      return brief;
    }
  }

  return null;
};

// 生成角色草稿
const handleGenerateBrief = async () => {
  if (!conversationId.value) {
    ElMessage.error('没有找到对话ID');
    return;
  }

  const existingBrief = getLatestGeneratedRoleBriefFromMessages();
  if (existingBrief) {
    roleBrief.value = existingBrief;
    assistantStep.value = 'brief_generated';
    ElMessage.success('已复用当前对话中的角色草稿');
    console.log('[角色助手] 复用聊天中已有草稿:', roleBrief.value);
    return;
  }
  
  isAssistantLoading.value = true;
  try {
    console.log('[角色助手] 开始生成草稿，conversationId:', conversationId.value);
    const response = await characterApi.generateRoleBrief(conversationId.value, false);
    
    if (response.data.code === 200 && response.data.data) {
      roleBrief.value = response.data.data;
      if (!roleBrief.value.voiceType && voiceList.value.length > 0) {
        roleBrief.value.voiceType = voiceList.value[0].voice_type;
      }
      assistantStep.value = 'brief_generated';
      ElMessage.success('草稿生成成功');
      console.log('[角色助手] 草稿生成成功:', roleBrief.value);
    } else {
      throw new Error(response.data.message || '生成失败');
    }
  } catch (error) {
    console.error('[角色助手] 生成草稿失败:', error);
    ElMessage.error('生成草稿失败：' + (error.response?.data?.message || error.message));
  } finally {
    isAssistantLoading.value = false;
  }
};

// 预览研究任务
const handlePreviewTasks = async () => {
  if (!conversationId.value) {
    ElMessage.error('没有找到对话ID');
    return;
  }
  
  isAssistantLoading.value = true;
  try {
    const response = await characterApi.getResearchTasks(conversationId.value);
    
    if (response.data.code === 200 && response.data.data) {
      const taskData = response.data.data;
      // 后端返回的是 { tasks: [...], defaultLimit, maxLimit }
      const tasks = taskData.tasks || [];
      researchTasks.value = tasks.map((task) => ({
        id: task.id || `task-${Date.now()}-${Math.random()}`,
        query: task.query,
        rationale: task.rationale,
        enabled: task.enabled !== false // 默认启用
      }));
      assistantStep.value = 'tasks_previewed';
      ElMessage.success('任务列表加载成功');
      console.log('[角色助手] 加载到研究任务:', researchTasks.value);
    } else {
      throw new Error(response.data.message || '加载失败');
    }
  } catch (error) {
    console.error('[角色助手] 加载任务失败:', error);
    ElMessage.error('加载任务失败：' + (error.response?.data?.message || error.message));
  } finally {
    isAssistantLoading.value = false;
  }
};

// 确认创建角色
const handleConfirmCreation = async (withResearch) => {
  if (!roleBrief.value) {
    ElMessage.error('没有找到角色草稿');
    return;
  }
  
  // 验证必填字段
  if (!roleBrief.value.name || !roleBrief.value.voiceType) {
    ElMessage.warning('请填写角色名称并选择声音类型');
    return;
  }
  
  isAssistantLoading.value = true;
  try {
    const payload = {
      conversationId: conversationId.value,
      voiceType: roleBrief.value.voiceType,
      avatarUrl: roleBrief.value.avatarUrl,
      overrideName: roleBrief.value.name,
      description: roleBrief.value.description,
      personaPrompt: roleBrief.value.personaPrompt,
      greetingMessage: roleBrief.value.greetingMessage,
      deepResearch: withResearch,  // 修正字段名：executeResearch -> deepResearch
      researchLimit: 12,
      researchQueries: withResearch ? researchTasks.value.filter(t => t.enabled).map(t => t.query) : []
    };
    
    console.log('[角色助手] 提交创建请求:', payload);
    const response = await characterApi.confirmRoleCreation(payload);
    
    if (response.data.code === 200) {
      ElMessage.success('角色创建成功！');
      isAssistantPanelVisible.value = false;
      
      // 重置状态
      assistantStep.value = 'initial';
      roleBrief.value = null;
      researchTasks.value = [];
      
      // 可选：跳转到角色列表
      setTimeout(() => {
        router.push('/characters');
      }, 1500);
    } else {
      throw new Error(response.data.message || '创建失败');
    }
  } catch (error) {
    console.error('[角色助手] 创建角色失败:', error);
    ElMessage.error('创建角色失败：' + (error.response?.data?.message || error.message));
  } finally {
    isAssistantLoading.value = false;
  }
};

// 触发文件选择
const triggerFileInput = () => {
  fileInput.value?.click();
};

// 处理图片上传
const handleImageUpload = async (event) => {
  const file = event.target.files?.[0];
  if (!file) return;
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    return;
  }
  
  // 检查文件大小（限制10MB）
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过10MB');
    return;
  }
  
  isUploading.value = true;
  try {
    const response = await characterApi.uploadImage(file);
    
    if (response.data.success && response.data.data) {
      roleBrief.value.avatarUrl = response.data.data;
      ElMessage.success('图片上传成功');
    } else {
      throw new Error(response.data.message || '上传失败');
    }
  } catch (error) {
    console.error('上传图片失败:', error);
    ElMessage.error('上传图片失败：' + (error.response?.data?.message || error.message));
  } finally {
    isUploading.value = false;
    // 清空input，允许重复选择同一文件
    event.target.value = '';
  }
};

// AI生成头像
const handleImageGeneration = async () => {
  if (!roleBrief.value || !roleBrief.value.name) {
    ElMessage.warning('请先填写角色名称');
    return;
  }
  
  isGeneratingImage.value = true;
  try {
    // 构建prompt
    const prompt = `A professional avatar for a character named "${roleBrief.value.name}". ${roleBrief.value.description || ''}. High quality, detailed, portrait style.`;
    
    console.log('[AI生成头像] prompt:', prompt);
    const response = await characterApi.generateImage(prompt);
    
    if (response.data.code === 200 && response.data.data) {
      // 后端返回的是ImageGenerationResponseDTO，包含imageUrls数组
      const imageUrl = response.data.data.imageUrls?.[0] || response.data.data.url;
      if (imageUrl) {
        roleBrief.value.avatarUrl = imageUrl;
        ElMessage.success('头像生成成功');
        console.log('[AI生成头像] 成功，URL:', imageUrl);
      } else {
        throw new Error('未获取到图片URL');
      }
    } else {
      throw new Error(response.data.message || '生成失败');
    }
  } catch (error) {
    console.error('生成头像失败:', error);
    ElMessage.error('生成头像失败：' + (error.response?.data?.message || error.message));
  } finally {
    isGeneratingImage.value = false;
  }
};

// 试听声音
const previewVoice = async () => {
  if (!roleBrief.value || !roleBrief.value.voiceType) {
    return;
  }
  
  // 停止当前正在播放的试听
  if (currentPreviewAudio) {
    currentPreviewAudio.pause();
    currentPreviewAudio = null;
  }
  
  try {
    // 使用角色名称或默认文本作为试听内容
    const testText = roleBrief.value.greetingMessage || `你好，我是${roleBrief.value.name || '角色'}`;
    
    const response = await characterApi.textToSpeech({
      text: testText,
      voiceType: roleBrief.value.voiceType
    });
    
    if (response.data.success && response.data.data?.audioData) {
      const audioUrl = response.data.data.audioData;
      currentPreviewAudio = new Audio(audioUrl);
      
      currentPreviewAudio.addEventListener('ended', () => {
        currentPreviewAudio = null;
      });
      
      currentPreviewAudio.addEventListener('error', (e) => {
        console.error('音频播放失败:', e);
        ElMessage.error('音频播放失败');
        currentPreviewAudio = null;
      });
      
      await currentPreviewAudio.play();
      ElMessage.success('开始播放试听');
    } else {
      throw new Error(response.data.message || '生成失败');
    }
  } catch (error) {
    console.error('试听声音失败:', error);
    ElMessage.error('试听失败：' + (error.response?.data?.message || error.message));
  }
};

// 生命周期
onMounted(async () => {
  isLoading.value = true;
  
  try {
    // 1. 加载可用模型列表
    await fetchAvailableModels();
    syncRuntimePolicy(createDefaultRuntimePolicy());
    syncContextSnapshot({
      ...createEmptyContextSnapshot(),
      modelKey: selectedModel.value,
      modelContextWindow: Number(selectedModelInfo.value?.contextWindow) || 0,
      reservedOutputTokens: runtimePolicy.value.reservedOutputTokens
    });
    await fetchKnowledgeBases();
    
    // 2. 加载角色信息（必须先有角色信息才能显示开场白）
    await loadCharacterInfo();
    
    // 3. 加载历史对话列表
    await fetchConversationHistory();
    
    // 4. 检查是否有保存的对话
    const savedConversationId = sessionStorage.getItem(`stream-conversation-${roleId.value}`);
    if (savedConversationId) {
      // 有历史对话，切换到该对话（会锁定模型）
      await handleSwitchConversation(savedConversationId);
    } else {
      // 没有对话，仅显示开场白（不创建对话）
      applyRuntimePolicyToThinkingState();
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
/* ===== Glass Premium 琉璃拟态主题 ===== */

/* 容器布局 */
.chat-container {
  display: flex;
  width: 100%;
  min-width: 100%;
  height: 100dvh;
  max-height: 100dvh;
  min-height: 100dvh;
  background: linear-gradient(145deg, var(--surface-base) 0%, var(--surface-base-alt) 50%, var(--surface-base) 100%);
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.chat-view {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  min-height: 0;
  min-width: 0;
}

/* 头部样式 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.85rem 1.5rem;
  background: var(--surface-elevated);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border-subtle);
  position: relative;
  z-index: 10;
}

.character-info {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

.back-button {
  background: var(--surface-glass);
  border: 1px solid var(--border-default);
  color: var(--text-subtle);
  padding: 0.45rem 0.85rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 1rem;
}

.back-button:hover {
  background: var(--surface-glass-active);
  border-color: var(--border-strong);
  color: var(--text-default);
}

.avatar {
  width: 38px;
  height: 38px;
  display: block;
  border-radius: 10px;
  object-fit: cover;
  border: 1px solid var(--border-glass);
  background: #e0f2fe;
}

.name {
  margin: 0;
  color: var(--text-primary);
  font-size: 1.15rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}

/* 协议选择器 - 改进版 */
.protocol-selector {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  min-height: 3.15rem;
  padding: 0.5rem 0.82rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 252, 255, 0.98) 100%);
  border: 1px solid rgba(191, 219, 254, 0.9);
  border-radius: 20px;
  box-shadow: 0 12px 24px rgba(148, 163, 184, 0.12);
  backdrop-filter: blur(14px);
  transition: border-color 0.25s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.25s cubic-bezier(0.4, 0, 0.2, 1), transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.protocol-selector:hover {
  border-color: rgba(96, 165, 250, 0.96);
  box-shadow: 0 16px 30px rgba(96, 165, 250, 0.14);
  transform: translateY(-1px);
}

.protocol-connection {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
  color: #10b981;
  font-size: 0.92rem;
  font-weight: 700;
}

.protocol-status-icon {
  width: 1rem;
  height: 1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.protocol-status-dot {
  width: 0.48rem;
  height: 0.48rem;
  flex-shrink: 0;
  border-radius: 999px;
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.12);
}

.status-disconnected .protocol-connection {
  color: #ef4444;
}

.status-disconnected .protocol-status-dot {
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}

.status-connecting .protocol-connection {
  color: #f59e0b;
}

.status-connecting .protocol-status-dot {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.12);
}

.protocol-select {
  min-width: 138px;
  width: auto;
}

/* 协议选择器下拉框浅色主题 */
.protocol-select :deep(.el-select__wrapper) {
  min-height: 2.2rem !important;
  padding: 0 0.2rem 0 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

.protocol-select :deep(.el-select__selection) {
  min-height: auto !important;
}

.protocol-select :deep(.el-select__selected-item),
.protocol-select :deep(.el-select__placeholder),
.protocol-select :deep(.el-select__input-text) {
  color: #334155 !important;
  font-weight: 600 !important;
  font-size: 0.92rem !important;
}

.protocol-select :deep(.el-select__placeholder) {
  color: rgba(100, 116, 139, 0.68) !important;
}

.protocol-select :deep(.el-select__caret) {
  color: #64748b !important;
  font-size: 0.95rem !important;
}

.protocol-select :deep(.el-select__wrapper.is-focused),
.protocol-select :deep(.el-tooltip__trigger.is-focus),
.protocol-select :deep(.el-tooltip__trigger:focus-visible) {
  box-shadow: none !important;
}

@keyframes pulse {
  0% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(1.2); }
  100% { opacity: 1; transform: scale(1); }
}

.protocol-status-text {
  white-space: nowrap;
}

/* 消息列表区域 */
.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 1.5rem 2rem;
  scroll-behavior: smooth;
  background: transparent;
  display: flex;
  flex-direction: column;
}

/* 空状态、加载、错误提示 - 改进版 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 3rem 2rem;
  animation: fadeIn 0.5s ease-in;
}

.empty-state p {
  margin: 0.5rem 0;
  color: var(--text-primary);
  font-size: 1.1rem;
  font-weight: 500;
}

.empty-state .hint {
  color: var(--text-secondary);
  font-size: 0.95rem;
  font-weight: 400;
  opacity: 0.9;
  margin-top: 0.75rem;
}

.loading-messages,
.error-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  padding: 3rem 2rem;
  color: var(--text-secondary);
  font-size: 1rem;
  font-weight: 500;
}

.error-messages {
  color: var(--error-text);
}

/* 增加一个包裹层用于中心对齐或者让消息直接居中且定宽 */
.message-item {
  width: 100%;
  max-width: 100%; /* Unconstrained to hit edges */
  margin-bottom: 2rem;
  animation: slide-up-fade 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards;
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
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  border: 1px solid var(--border-glass);
}

.message-avatar img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  background: #e0f2fe;
}

.message-avatar.user {
  order: 2;
}

/* 消息气泡 — Glass Premium */
.message-bubble {
  max-width: 75%;
  padding: 16px 20px;
  border-radius: 20px;
  position: relative;
  transition: box-shadow 0.3s ease, transform 0.2s ease;
  line-height: 1.6;
}

.assistant .message-bubble {
  background: var(--ai-bubble-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--ai-bubble-border);
  color: var(--text-primary);
  border-radius: 16px;
  border-bottom-left-radius: 4px;
  box-shadow: var(--ai-bubble-shadow);
}

.assistant .message-bubble:hover {
  box-shadow: var(--ai-bubble-shadow-hover);
  border-color: var(--ai-bubble-border-hover);
}

.user .message-bubble {
  background: var(--user-bubble-bg);
  color: var(--user-bubble-text, #ffffff);
  border-radius: 16px;
  border-bottom-right-radius: 4px;
  border: 1px solid var(--border-glass, rgba(255, 255, 255, 0.1));
  box-shadow: var(--user-bubble-shadow);
}

.user .message-bubble:hover {
  box-shadow: var(--user-bubble-shadow-hover);
  transform: translateY(-1px);
}

.system .message-bubble {
  background: var(--error-soft-bg);
  border: 1px solid var(--error-soft-border);
  color: var(--error-text);
  max-width: 100%;
  text-align: center;
  backdrop-filter: blur(8px);
}

/* 消息内容 */
.message-content {
  line-height: 1.6;
}

/* 流式文本 */
.streaming-text {
  position: relative;
}

.thinking-panel {
  margin-bottom: 12px;
  border: 1px solid var(--border-default);
  border-radius: 12px;
  background: var(--surface-base-alt);
  overflow: hidden;
}

.thinking-panel-streaming {
  border-color: var(--primary-soft-border);
  box-shadow: 0 0 0 1px var(--border-focus-ring), 0 0 16px rgba(99, 102, 241, 0.06);
}

.thinking-panel-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border: 0;
  background: var(--surface-glass-subtle);
  color: var(--text-subtle);
  cursor: pointer;
  font-size: 0.83rem;
  font-weight: 600;
  transition: background 0.2s;
}

.thinking-panel-header:hover {
  background: var(--surface-glass-hover);
}

.thinking-panel-toggle {
  color: var(--text-accent-strong);
  font-size: 0.78rem;
  font-weight: 500;
}

.thinking-panel-body {
  padding: 10px 14px 14px;
}

.thinking-raw-text {
  white-space: pre-wrap;
  color: var(--text-subtle);
  font-size: 0.86rem;
  line-height: 1.7;
}

.thinking-rendered-content {
  line-height: 1.7;
  color: var(--text-dim);
}

.thinking-rendered-content :deep(pre) {
  background: var(--surface-base-alt);
  border: 1px solid var(--border-subtle);
  padding: 0.85rem;
  border-radius: 8px;
  overflow-x: auto;
  margin: 0.5rem 0;
}

.thinking-rendered-content :deep(code) {
  background: var(--surface-base-alt);
  padding: 0.2rem 0.4rem;
  border-radius: 4px;
  font-size: 0.85rem;
}

.cursor-blink {
  animation: blink 1s infinite;
  color: var(--text-accent-strong);
  font-weight: bold;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* Markdown渲染样式 */
.rendered-content {
  line-height: 1.65;
}

.rendered-content :deep(.citation-ref) {
  display: inline-flex;
  align-items: center;
  margin: 0 0.15rem;
  padding: 0.05rem 0.45rem;
  border-radius: 999px;
  border: 1px solid var(--accent-soft-border);
  background: var(--primary-soft-bg);
  color: var(--text-accent-purple);
  font-size: 0.82rem;
  text-decoration: none;
  transition: all 0.25s ease;
}

.rendered-content :deep(.citation-ref:hover) {
  background: var(--primary-soft-bg-hover);
  border-color: var(--accent-soft-border-hover);
}

.rendered-content :deep(pre) {
  background: var(--surface-elevated);
  border: 1px solid var(--border-subtle);
  padding: 1rem;
  border-radius: 10px;
  overflow-x: auto;
  margin: 0.5rem 0;
}

.rendered-content :deep(code) {
  background: rgba(15, 23, 42, 0.5);
  padding: 0.2rem 0.45rem;
  border-radius: 4px;
  font-size: 0.875rem;
  color: var(--text-accent-purple);
}

.rendered-content :deep(ul),
.rendered-content :deep(ol) {
  margin-left: 1.5rem;
}

.rendered-content :deep(blockquote) {
  border-left: 3px solid var(--primary-soft-border);
  padding-left: 1rem;
  color: var(--text-subtle);
}

.citation-panel {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
}

.citation-panel-title {
  margin-bottom: 8px;
  color: var(--text-subtle);
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.citation-card {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--surface-glass-subtle);
  border: 1px solid var(--border-subtle);
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(6px);
}

.citation-card-active {
  border-color: var(--primary-soft-border-hover);
  box-shadow: 0 0 0 2px var(--border-focus-ring);
  transform: translateY(-1px);
}

.citation-card:target {
  border-color: var(--primary-soft-border-hover);
  box-shadow: 0 0 0 2px var(--border-focus-ring);
  transform: translateY(-1px);
}

.citation-card-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.citation-label {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.55rem;
  border-radius: 999px;
  background: var(--primary-soft-bg);
  color: var(--text-accent);
  font-size: 0.78rem;
  font-weight: 700;
}

.citation-file {
  color: var(--text-default);
  font-size: 0.9rem;
  font-weight: 600;
  word-break: break-all;
}

.citation-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 6px;
  color: var(--text-subtle);
  font-size: 0.78rem;
}

.citation-snippet {
  margin-top: 8px;
  color: var(--text-default);
  font-size: 0.86rem;
  line-height: 1.6;
  white-space: pre-wrap;
}

.citation-snippet-collapsed {
  display: -webkit-box;
  -webkit-line-clamp: 5;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.citation-snippet-html {
  white-space: normal;
}

.citation-snippet-html :deep(p),
.citation-snippet-html :deep(h1),
.citation-snippet-html :deep(h2),
.citation-snippet-html :deep(h3),
.citation-snippet-html :deep(h4),
.citation-snippet-html :deep(ul),
.citation-snippet-html :deep(ol),
.citation-snippet-html :deep(blockquote) {
  margin: 0.35rem 0;
}

.citation-snippet-html :deep(h1),
.citation-snippet-html :deep(h2),
.citation-snippet-html :deep(h3),
.citation-snippet-html :deep(h4) {
  color: var(--text-bright);
  font-size: 0.92rem;
  line-height: 1.45;
}

.citation-snippet-html :deep(strong) {
  color: var(--text-bright);
  font-weight: 700;
}

.citation-snippet-html :deep(code) {
  padding: 0.08rem 0.35rem;
  border-radius: 6px;
  background: var(--surface-elevated);
  color: var(--text-secondary);
}

.citation-snippet-html :deep(blockquote) {
  padding-left: 0.7rem;
  border-left: 2px solid rgba(125, 211, 252, 0.35);
  color: var(--text-secondary);
}

.citation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.citation-action-btn {
  border: 1px solid var(--primary-soft-border);
  background: var(--primary-soft-bg);
  color: var(--text-accent);
  padding: 0.32rem 0.68rem;
  border-radius: 999px;
  font-size: 0.78rem;
  cursor: pointer;
  transition: all 0.25s ease;
}

.citation-action-btn:hover {
  background: var(--primary-soft-bg-hover);
  border-color: var(--primary-soft-border-hover);
}

.citation-action-btn.secondary {
  color: var(--text-secondary);
  border-color: rgba(148, 163, 184, 0.15);
  background: var(--surface-glass);
}

.citation-preview-overlay {
  background: rgba(2, 6, 23, 0.8);
  backdrop-filter: blur(8px);
  z-index: 1200;
}

.citation-preview-modal {
  width: min(880px, calc(100vw - 32px));
  max-height: calc(100vh - 48px);
  overflow: hidden;
  border-radius: 20px;
  border: 1px solid var(--border-default);
  background: var(--surface-overlay);
  backdrop-filter: blur(24px);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.5), 0 0 40px rgba(99, 102, 241, 0.06);
}

.citation-preview-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  padding: 20px 22px 14px;
  border-bottom: 1px solid var(--border-strong);
}

.citation-preview-title {
  margin: 0;
  color: var(--text-bright);
  font-size: 1.05rem;
}

.citation-preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin: 8px 0 0;
  color: var(--text-subtle);
  font-size: 0.82rem;
}

.citation-preview-loading,
.citation-preview-error {
  padding: 28px 22px;
  color: var(--text-secondary);
}

.citation-preview-error {
  color: #fda4af;
}

.citation-preview-body {
  max-height: calc(100vh - 180px);
  overflow-y: auto;
  padding: 18px 22px 22px;
}

.citation-context-segment {
  padding: 14px 16px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(15, 23, 42, 0.42);
}

.citation-context-segment + .citation-context-segment {
  margin-top: 12px;
}

.citation-context-hit {
  border-color: var(--primary-soft-border);
  background: var(--primary-soft-bg);
}

.citation-context-label {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
}

.citation-context-hit-tag {
  display: inline-flex;
  align-items: center;
  padding: 0.1rem 0.48rem;
  border-radius: 999px;
  background: rgba(99, 102, 241, 0.15);
  color: var(--text-accent);
  font-size: 0.74rem;
}

.citation-context-content {
  color: var(--text-default);
  line-height: 1.7;
  white-space: pre-wrap;
}

/* 音频控制 */
.audio-controls {
  margin-top: 12px;
  display: flex;
}

.audio-play-btn, .audio-stop-btn {
  background: var(--surface-base-alt);
  color: var(--text-secondary);
  border: none;
  border-radius: 99px;
  padding: 6px 14px;
  font-size: 0.8rem;
  font-weight: 500;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  transition: all 0.2s ease;
}

.audio-play-btn:hover, .audio-stop-btn:hover {
  background: var(--border-default); /* slightly darker than alt */
  color: var(--text-primary);
}

.audio-icon {
  font-size: 0.85rem;
  opacity: 0.9;
}

.audio-playing {
  display: flex;
  align-items: center;
  gap: 12px;
}

.audio-progress {
  color: var(--text-accent-strong);
  font-size: 0.85rem;
}

/* 消息时间戳 - 改进版，提高可读性 */
.message-time {
  margin-top: 8px;
  font-size: 0.8rem;
  color: var(--text-secondary);
  font-weight: 500;
  opacity: 0.85;
  letter-spacing: 0.02em;
}

/* AI思考指示器 */
.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 12px 16px;
  margin-left: 48px;
}

.typing-dot {
  width: 7px;
  height: 7px;
  background: var(--text-accent-strong);
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

/* 模型选择器 - 改进版 */
.model-selector {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  min-width: 242px;
  padding: 0.42rem 0.72rem 0.42rem 0.6rem;
  background: linear-gradient(180deg, rgba(233, 247, 255, 0.98) 0%, rgba(218, 241, 252, 0.96) 100%);
  border: 1px solid rgba(103, 232, 249, 0.56);
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(56, 189, 248, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
  backdrop-filter: blur(16px);
}

.model-selector:not(.locked):hover {
  border-color: rgba(56, 189, 248, 0.76);
  box-shadow: 0 18px 34px rgba(56, 189, 248, 0.18), 0 0 0 1px rgba(103, 232, 249, 0.16);
  transform: translateY(-1px);
}

.model-selector.locked {
  opacity: 1;
  cursor: not-allowed;
}

.model-selector-star {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1.82rem;
  height: 1.82rem;
  flex-shrink: 0;
  color: #38bdf8;
}

.model-selector-star svg {
  width: 1.1rem;
  height: 1.1rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.1;
  stroke-linecap: round;
  stroke-linejoin: round;
  filter: drop-shadow(0 0 10px rgba(56, 189, 248, 0.22));
}

.model-selector-body {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
}

.model-buttons {
  display: flex;
  gap: 8px;
}

.model-btn {
  position: relative;
  padding: 6px 14px;
  background: var(--surface-glass);
  border: 1px solid var(--border-default);
  border-radius: 8px;
  color: var(--text-subtle);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  align-items: center;
  gap: 6px;
}

.model-btn:hover:not(.disabled) {
  background: var(--primary-soft-bg);
  border-color: var(--primary-soft-border);
  color: var(--text-default);
}

.model-btn.active {
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.18), rgba(139, 92, 246, 0.12));
  border-color: var(--primary-soft-border-hover);
  color: #e0e7ff;
  box-shadow: var(--shadow-glow);
}

.model-btn.disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

.model-btn .check-mark {
  color: var(--success-color);
  font-weight: bold;
}

/* 底部输入区域 */
.chat-footer {
  flex-shrink: 0;
  width: 100%;
  box-sizing: border-box;
  padding: 0.78rem 1.4rem calc(0.92rem + env(safe-area-inset-bottom, 0px));
  background: var(--surface-elevated);
  border-top: 1px solid var(--border-subtle);
  position: relative;
  z-index: 10;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 创建一个内部浮动的包裹层 */
.footer-wrapper {
  width: 100%;
  max-width: 100%;
  background: transparent;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.footer-options-bar {
  display: flex;
  flex-direction: column;
  align-items: center;  /* Center the chips */
  gap: 0.58rem;
  margin-bottom: 0.62rem;
  width: min(100%, 920px);
}

.footer-row {
  display: flex;
  align-items: center;
  justify-content: center; /* Center the chips */
  gap: 0.75rem;
  flex-wrap: wrap;
}

.footer-row-primary {
  gap: 0.5rem;
}

.footer-row-rag {
  gap: 0.5rem;
  padding-left: 0.25rem;
}

.system-message {
  flex: 0 1 164px;
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.84rem;
  font-weight: 500;
  opacity: 0.9;
}

/* Chip Toggle 样式 - 改进版 */
.chip-group {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.chip-toggle {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 7px 14px;
  border-radius: 999px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--surface-glass-strong);
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  white-space: nowrap;
  line-height: 1.4;
  user-select: none;
  backdrop-filter: blur(8px);
}

.chip-toggle:hover:not(.disabled) {
  background: var(--surface-glass-active);
  border-color: var(--border-strong);
  color: var(--text-primary);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.chip-toggle.active {
  background: var(--primary-soft-bg);
  border-color: var(--primary-soft-border);
  color: var(--text-accent);
  box-shadow: 0 0 0 1px var(--border-focus-ring), 0 2px 8px rgba(99, 102, 241, 0.15);
}

.chip-toggle.active:hover {
  background: var(--primary-soft-bg-hover);
  border-color: var(--primary-soft-border-hover);
  color: var(--text-accent-hover);
  transform: translateY(-1px);
  box-shadow: 0 0 0 1px var(--border-focus-ring), 0 4px 12px rgba(99, 102, 241, 0.25);
}

.chip-toggle.disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
}

.chip-settings-trigger svg {
  width: 0.9rem;
  height: 0.9rem;
  stroke: currentColor;
  fill: none;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.footer-row-context {
  width: 100%;
  justify-content: flex-end;
}

.context-ring-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: transparent;
  box-shadow: none;
  cursor: default;
}

.context-ring-button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 4px rgba(125, 211, 252, 0.18);
}

.context-ring-core {
  position: relative;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: conic-gradient(from -90deg, rgba(56, 189, 248, 0.95) calc(var(--ring-progress) * 1%), rgba(186, 230, 253, 0.28) 0);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-shadow:
    0 0 0 1px rgba(186, 230, 253, 0.34),
    0 4px 10px rgba(56, 189, 248, 0.12);
}

.context-ring-core::after {
  content: "";
  position: absolute;
  inset: 2.5px;
  border-radius: 50%;
  background: rgba(247, 251, 255, 0.98);
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.92);
}

.context-tooltip {
  min-width: 210px;
}

.context-tooltip-title {
  margin-bottom: 0.38rem;
  font-size: 0.82rem;
  font-weight: 700;
  color: #0f172a;
}

.context-tooltip-line {
  font-size: 0.76rem;
  line-height: 1.6;
  color: #475569;
}

.advanced-drawer-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(240, 249, 255, 0.34);
  backdrop-filter: blur(8px);
  z-index: 60;
  display: flex;
  justify-content: flex-end;
}

.advanced-drawer {
  width: min(430px, 100vw);
  height: 100%;
  min-height: 100dvh;
  max-height: 100dvh;
  box-sizing: border-box;
  padding: 1.35rem 1.1rem 1.25rem;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(248, 252, 255, 0.96)),
    radial-gradient(circle at top right, rgba(125, 211, 252, 0.2), transparent 58%);
  border-left: 1px solid rgba(186, 230, 253, 0.96);
  box-shadow: -18px 0 48px rgba(14, 165, 233, 0.12);
  backdrop-filter: blur(18px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.advanced-drawer-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.6rem;
}

.advanced-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.5rem;
}

.advanced-panel-kicker {
  margin: 0 0 0.18rem;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #0891b2;
}

.advanced-panel-header h3 {
  margin: 0;
  font-size: 1.1rem;
  color: var(--text-primary);
}

.advanced-panel-close {
  width: 2rem;
  height: 2rem;
  border: 1px solid rgba(186, 230, 253, 0.92);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s ease;
}

.advanced-panel-close:hover {
  color: var(--text-primary);
  border-color: rgba(56, 189, 248, 0.65);
}

.advanced-panel-description {
  margin: 0 0 0.92rem;
  font-size: 0.85rem;
  line-height: 1.65;
  color: var(--text-secondary);
}

.advanced-drawer-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 0.15rem;
  padding-bottom: 1.3rem;
}

.advanced-drawer-section {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid rgba(203, 213, 225, 0.74);
}

.advanced-section-title {
  margin-bottom: 0.8rem;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--text-primary);
}

.advanced-slider-card,
.advanced-mini-card {
  padding: 0.82rem 0.9rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(186, 230, 253, 0.88);
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.06);
}

.advanced-slider-card + .advanced-slider-card,
.advanced-pill-group + .advanced-slider-card,
.advanced-slider-card + .advanced-inline-grid,
.advanced-inline-grid + .advanced-pill-group {
  margin-top: 0.8rem;
}

.advanced-slider-card.muted {
  opacity: 0.55;
}

.advanced-slider-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  margin-bottom: 0.7rem;
  font-size: 0.82rem;
  color: var(--text-secondary);
}

.advanced-slider-header strong {
  font-size: 0.88rem;
  color: var(--text-primary);
}

.advanced-pill-group {
  display: flex;
  flex-wrap: wrap;
  gap: 0.55rem;
}

.advanced-pill-btn {
  min-height: 40px;
  padding: 0.62rem 0.92rem;
  border-radius: 999px;
  border: 1px solid rgba(203, 213, 225, 0.92);
  background: rgba(255, 255, 255, 0.9);
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.18s ease;
}

.advanced-pill-btn:hover:not(.disabled) {
  transform: translateY(-1px);
  border-color: rgba(14, 165, 233, 0.44);
  color: var(--text-primary);
}

.advanced-pill-btn.active {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.18), rgba(6, 182, 212, 0.12));
  border-color: rgba(14, 165, 233, 0.42);
  color: #0f172a;
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.12);
}

.advanced-pill-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.advanced-inline-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.75rem;
  margin-top: 0.8rem;
}

.advanced-mini-label {
  display: block;
  margin-bottom: 0.6rem;
  font-size: 0.76rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.advanced-number {
  width: 100%;
}

.advanced-panel-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  margin-top: 0.75rem;
  padding: 1rem 0 calc(1rem + env(safe-area-inset-bottom, 0px));
  border-top: 1px solid rgba(203, 213, 225, 0.74);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.98));
}

.advanced-panel-hint {
  flex: 1;
  font-size: 0.78rem;
  line-height: 1.5;
  color: var(--text-subtle);
}

.advanced-panel-actions {
  display: flex;
  align-items: center;
  gap: 0.56rem;
}

.advanced-primary-btn,
.advanced-secondary-btn {
  min-height: 40px;
  padding: 0.65rem 1rem;
  border-radius: 999px;
  font-size: 0.84rem;
  font-weight: 700;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.advanced-primary-btn {
  border: 1px solid rgba(14, 165, 233, 0.42);
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.18), rgba(6, 182, 212, 0.14));
  color: #0f172a;
  box-shadow: 0 12px 24px rgba(14, 165, 233, 0.14);
}

.advanced-secondary-btn {
  border: 1px solid rgba(203, 213, 225, 0.96);
  background: rgba(255, 255, 255, 0.88);
  color: var(--text-secondary);
}

.advanced-primary-btn:hover,
.advanced-secondary-btn:hover {
  transform: translateY(-1px);
}

:deep(.advanced-slider .el-slider__runway) {
  height: 6px;
  background: rgba(186, 230, 253, 0.5);
}

:deep(.advanced-slider .el-slider__bar) {
  height: 6px;
  background: linear-gradient(90deg, #38bdf8, #0ea5e9);
}

:deep(.advanced-slider .el-slider__button) {
  width: 16px;
  height: 16px;
  border: 2px solid #fff;
  background: #0ea5e9;
  box-shadow: 0 8px 18px rgba(14, 165, 233, 0.28);
}

:deep(.advanced-number .el-input-number__decrease),
:deep(.advanced-number .el-input-number__increase) {
  background: rgba(224, 242, 254, 0.82);
  color: var(--text-secondary);
  border-color: rgba(186, 230, 253, 0.88);
}

:deep(.advanced-number .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.94);
  border-radius: 14px;
  box-shadow: 0 0 0 1px rgba(186, 230, 253, 0.88) inset;
}

:deep(.context-tooltip-popper.el-popper) {
  border-radius: 16px;
  border: 1px solid rgba(186, 230, 253, 0.92);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 16px 36px rgba(14, 165, 233, 0.14);
}

/* 保留旧开关样式以兼容其他地方 */
.search-toggle-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.85rem;
  color: var(--text-subtle);
}

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
  background: var(--switch-track-bg);
  border: 1px solid var(--switch-track-border);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 2px;
  bottom: 2px;
  background-color: var(--switch-knob);
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

input:checked + .slider {
  background: var(--switch-track-active);
  border-color: var(--switch-track-active-border);
}

input:checked + .slider:before {
  transform: translateX(18px);
  background-color: var(--switch-knob-active);
  box-shadow: 0 0 6px var(--switch-knob-glow);
}

.slider.round {
  border-radius: 22px;
}

.slider.round:before {
  border-radius: 50%;
}

/* 输入区域：变成一个完整的大胶囊/圆角框 */
.input-area {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  background: var(--surface-base-alt);
  border: 1px solid var(--border-default);
  border-radius: 20px;
  padding: 0.5rem 0.55rem 0.5rem 0.9rem;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.03);
  transition: all 0.3s ease;
  width: min(100%, 920px);
}

.input-area:focus-within {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px var(--border-focus-ring);
  background: var(--surface-overlay);
}

/* ASR语音录音按钮 & 图片上传按钮公用基础 */
.voice-record-btn,
.image-upload-btn {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(186, 230, 253, 0.9);
  color: #475569;
  font-size: 1.05rem;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0;
  align-self: center;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.92);
}

.voice-record-btn:hover:not(:disabled),
.image-upload-btn:hover:not(:disabled) {
  background: rgba(224, 242, 254, 0.96);
  border-color: rgba(125, 211, 252, 0.96);
  color: #0284c7;
}

.voice-record-btn:hover:not(:disabled) {
  transform: scale(1.08);
  box-shadow: var(--user-bubble-shadow-hover);
}

.voice-record-btn:active:not(:disabled) {
  transform: scale(0.95);
}

.voice-record-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: var(--surface-glass-strong);
  box-shadow: none;
}

/* 录音控制区域 */
.recording-controls {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: rgba(239, 68, 68, 0.1);
  border-radius: 16px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  animation: recording-border-pulse 2s ease-in-out infinite;
  flex-shrink: 0;
  backdrop-filter: blur(8px);
}

@keyframes recording-border-pulse {
  0%, 100% {
    border-color: rgba(239, 68, 68, 0.3);
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.2);
  }
  50% {
    border-color: rgba(239, 68, 68, 0.5);
    box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.06);
  }
}

.stop-recording-btn {
  padding: 0.5rem 1rem;
  background: linear-gradient(135deg, #dc2626, #ef4444);
  color: white;
  border: none;
  border-radius: 12px;
  font-weight: 600;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  white-space: nowrap;
}

.stop-recording-btn:hover {
  box-shadow: 0 2px 10px rgba(239, 68, 68, 0.3);
  transform: scale(1.03);
}

.stop-recording-btn:active {
  transform: scale(0.98);
}

.cancel-recording-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--surface-glass-strong);
  border: 1px solid var(--border-default);
  font-size: 0.9rem;
  color: var(--text-subtle);
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cancel-recording-btn:hover {
  background: var(--surface-glass-active);
  color: var(--text-primary);
}

.cancel-recording-btn:active {
  transform: scale(0.9);
}

/* 录音指示器 */
.recording-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #ef4444;
  font-weight: 600;
  font-size: 0.875rem;
  white-space: nowrap;
}

.recording-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  animation: recording-pulse 1.5s ease-in-out infinite;
  flex-shrink: 0;
}

@keyframes recording-pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(1.3);
  }
}

/* 识别中指示器 */
.processing-indicator {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  background: var(--primary-soft-bg);
  border-radius: 16px;
  border: 1px solid var(--primary-soft-border);
  color: var(--text-accent-strong);
  font-weight: 600;
  font-size: 0.85rem;
  white-space: nowrap;
  flex-shrink: 0;
  backdrop-filter: blur(8px);
}

.processing-spinner {
  font-size: 1.1rem;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 覆盖 Element Plus Textarea 默认原生样式 */
.message-input {
  flex: 1;
  align-self: stretch;
  display: flex;
  align-items: center;
}

.message-input :deep(.el-textarea__inner) {
  background: var(--surface-glass);
  border: 1px solid var(--border-default);
  border-radius: 16px;
  color: var(--text-primary);
  font-size: 0.95rem;
  padding: 0.85rem 1.15rem;
  outline: none;
  box-shadow: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  min-height: 46px !important;
  line-height: 1.5;
  scrollbar-width: none; /* Firefox */
  resize: none;
}

.message-input :deep(.el-textarea__inner)::-webkit-scrollbar {
  display: none;
}

.message-input :deep(.el-textarea__inner):focus {
  border-color: var(--border-focus);
  box-shadow: 0 0 0 3px var(--border-focus-ring), 0 0 16px rgba(99, 102, 241, 0.08);
  background: var(--surface-glass-strong);
}

.message-input :deep(.el-textarea__inner)::placeholder {
  color: var(--text-subtle);
  opacity: 0.65;
  font-weight: 400;
}

.message-input :deep(.el-textarea__inner):disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: rgba(15, 23, 42, 0.4);
}

.send-btn {
  min-width: 96px;
  min-height: 42px;
  padding: 0.65rem 1.12rem;
  background: var(--primary-color);
  color: #ffffff;
  border: none;
  border-radius: 99px; /* Very rounded pill like image */
  cursor: pointer;
  font-weight: 500;
  font-size: 0.95rem;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--shadow-sm);
  letter-spacing: 0.02em;
  margin: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  align-self: center;
}

.send-btn:hover:not(:disabled) {
  background: var(--primary-color-hover);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.send-btn:active:not(:disabled) {
  transform: translateY(0);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  box-shadow: none;
  background: var(--surface-glass-strong);
  color: var(--text-subtle);
}

/* 底部信息 */
.footer-info {
  margin-top: 0.75rem;
  font-size: 0.85rem;
}

.error-info {
  color: var(--error-color);
  display: flex;
  align-items: center;
  gap: 1rem;
}

.reconnect-btn {
  background: var(--error-soft-bg);
  border: 1px solid rgba(248, 113, 113, 0.3);
  color: var(--error-color);
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.25s;
}

.reconnect-btn:hover {
  background: rgba(248, 113, 113, 0.15);
  border-color: rgba(248, 113, 113, 0.5);
}

/* 滚动条样式 — Glass Premium */
.message-list::-webkit-scrollbar {
  width: 6px;
}

.message-list::-webkit-scrollbar-track {
  background: transparent;
}

.message-list::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb-subtle);
  border-radius: 3px;
}

.message-list::-webkit-scrollbar-thumb:hover {
  background: var(--scrollbar-thumb-subtle-hover);
}

/* 响应式 */
@media (max-width: 768px) {
  .chat-header {
    padding: 1rem;
  }
  
  .message-bubble {
    max-width: 85%;
  }
  
  .footer-options-bar {
    gap: 0.25rem;
  }
  
  .input-area {
    flex-direction: column;
  }
  
  .send-btn {
    width: 100%;
  }

  .advanced-drawer {
    width: 100vw;
    max-width: 100vw;
    min-height: 100dvh;
    max-height: 100dvh;
    padding: 1rem 0.9rem 0.95rem;
  }

  .advanced-inline-grid {
    grid-template-columns: 1fr;
  }

  .advanced-panel-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .advanced-panel-actions {
    width: 100%;
  }

  .advanced-primary-btn,
  .advanced-secondary-btn {
    flex: 1;
  }
}

/* === 角色创建助手样式 === */
.header-actions {
  display: flex;
  align-items: center;
  gap: 0.72rem;
}

.assistant-trigger-btn {
  display: flex;
  align-items: center;
  gap: 0.48rem;
  min-height: 3.15rem;
  padding: 0.68rem 1.08rem;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(243, 249, 255, 0.98) 100%);
  border: 1px solid rgba(191, 219, 254, 0.86);
  color: #0f172a;
  cursor: pointer;
  border-radius: 20px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 0.92rem;
  font-weight: 700;
  box-shadow: 0 10px 22px rgba(148, 163, 184, 0.1);
}

.assistant-trigger-btn:hover {
  border-color: rgba(96, 165, 250, 0.9);
  box-shadow: 0 14px 28px rgba(96, 165, 250, 0.14);
  transform: translateY(-1px);
}

.assistant-button-icon {
  width: 1.2rem;
  height: 1.2rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.9;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.assistant-text {
  white-space: nowrap;
}

.assistant-panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(2, 6, 23, 0.6);
  backdrop-filter: blur(6px);
  z-index: 1000;
  display: flex;
  justify-content: flex-end;
}

.assistant-panel {
  width: 100%;
  max-width: 450px;
  height: 100%;
  background: var(--surface-overlay);
  backdrop-filter: blur(24px);
  box-shadow: -8px 0 32px rgba(0, 0, 0, 0.3);
  border-left: 1px solid var(--border-subtle);
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
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 1rem;
  padding: 0.75rem;
  background-color: var(--bg-main);
  border-radius: 6px;
  border: 1px solid var(--border-color);
  transition: all 0.2s;
}

.task-item:hover {
  border-color: var(--primary-color);
  background-color: rgba(59, 130, 246, 0.05);
}

.task-item input[type="checkbox"] {
  margin-top: 0.5rem;
  width: 18px;
  height: 18px;
  cursor: pointer;
  flex-shrink: 0;
}

.task-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.task-query {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--text-primary);
  font-size: 0.95rem;
  font-weight: 500;
  padding: 0.25rem 0;
  outline: none;
}

.task-query:focus {
  border-bottom: 1px solid var(--primary-color);
}

.task-rationale {
  font-size: 0.85rem;
  color: var(--text-secondary);
  line-height: 1.4;
  font-style: italic;
}

.task-delete-btn {
  background: var(--surface-glass-strong);
  border: 1px solid var(--border-subtle);
  color: var(--text-subtle);
  border-radius: 8px;
  width: 28px;
  height: 28px;
  cursor: pointer;
  flex-shrink: 0;
  font-size: 1.2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
}

.task-delete-btn:hover {
  background: var(--error-soft-bg);
  border-color: var(--error-soft-border);
  color: var(--error-color);
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
  background: var(--surface-glass-strong);
  color: var(--text-secondary);
  border: 1px solid var(--border-default);
  padding: 0.65rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 500;
  text-align: center;
  transition: all 0.25s;
}

.btn-upload:hover:not(:disabled) {
  background: var(--surface-glass-active);
  border-color: var(--border-strong);
}

.btn-generate {
  background: var(--primary-gradient);
  color: white;
  border: none;
  padding: 0.65rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.85rem;
  font-weight: 500;
  text-align: center;
  transition: all 0.25s;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.2);
}

.btn-generate:hover:not(:disabled) {
  box-shadow: 0 4px 14px rgba(99, 102, 241, 0.35);
}

.btn-generate:disabled,
.btn-upload:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.force-end-btn {
  padding: 0.7rem 1.4rem;
  background: linear-gradient(135deg, #dc2626, #ef4444);
  color: white;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.9rem;
  font-weight: 600;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  margin-left: 0.5rem;
  box-shadow: 0 2px 10px rgba(239, 68, 68, 0.2);
}

.force-end-btn:hover {
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.35);
  transform: translateY(-1px);
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
  background: var(--surface-glass);
  border: 1px solid var(--border-default);
  border-radius: 8px;
  color: var(--text-subtle);
  cursor: pointer;
  transition: all 0.25s;
}

.btn-preview:hover:not(:disabled) {
  background: var(--primary-soft-bg);
  border-color: var(--primary-soft-border);
  color: var(--text-accent);
}

.btn-preview:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* ==================== 模型选择器样式 ==================== */
.model-selector {
  display: flex;
  align-items: center;
  gap: 0.85rem;
}

/* Element Plus 下拉框样式 — Glass Premium 改进版 */
.model-select {
  min-width: 0;
  width: 100%;
}

.model-select :deep(.el-select__wrapper) {
  min-height: auto !important;
  padding: 0 !important;
  background: transparent !important;
  box-shadow: none !important;
}

.model-select :deep(.el-select__selection) {
  min-height: auto !important;
}

.model-select :deep(.el-select__placeholder),
.model-select :deep(.el-select__selected-item),
.model-select :deep(.el-select__input-text) {
  color: #0f172a !important;
  font-weight: 600 !important;
  font-size: 0.98rem !important;
  letter-spacing: 0.01em;
}

.model-select :deep(.el-select__placeholder) {
  color: rgba(51, 65, 85, 0.52) !important;
}

.model-select :deep(.el-select__caret) {
  color: #0891b2 !important;
  font-size: 1rem !important;
}

.model-select :deep(.el-select__wrapper.is-focused),
.model-select :deep(.el-tooltip__trigger.is-focus),
.model-select :deep(.el-tooltip__trigger:focus-visible) {
  box-shadow: none !important;
}

.model-select :deep(.el-input__wrapper),
.model-select :deep(.el-input__inner) {
  background-color: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

.model-selector.locked .model-select {
  opacity: 0.92;
  pointer-events: none;
}


/* ==================== 图片上传相关样式 ==================== */
.image-upload-btn {
  padding: 0.5rem 0.8rem;
  background: var(--surface-glass);
  border: 1px solid var(--border-default);
  border-radius: 10px;
  color: var(--text-subtle);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 1.2rem;
}

.image-upload-btn:hover:not(:disabled) {
  background: var(--primary-soft-bg);
  border-color: var(--primary-soft-border);
  color: var(--text-accent);
  transform: scale(1.05);
}

.image-upload-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.image-preview-container {
  display: flex;
  gap: 0.5rem;
  padding: 0.5rem;
  background: var(--surface-glass-subtle);
  border: 1px solid var(--border-subtle);
  border-radius: 10px;
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
  border: 1px solid var(--border-glass);
}

.remove-image-btn {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(239, 68, 68, 0.85);
  color: white;
  border: 2px solid rgba(15, 23, 42, 0.9);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.1rem;
  line-height: 1;
  transition: all 0.2s;
}

.remove-image-btn:hover {
  background: #ef4444;
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
  background: var(--surface-glass);
  border-radius: 8px;
  text-decoration: none;
  color: var(--success-color);
  font-size: 0.85rem;
  transition: all 0.25s ease;
  border: 1px solid var(--border-subtle);
}

.attachment-document:hover,
.attachment-other:hover {
  background: var(--surface-glass-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
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
  background: rgba(2, 6, 23, 0.92);
  backdrop-filter: blur(12px);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease-in;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
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
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--surface-glass-strong);
  color: var(--text-subtle);
  border: 1px solid var(--border-glass);
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s;
  z-index: 2001;
  backdrop-filter: blur(8px);
}

.preview-close-btn:hover {
  background: var(--surface-glass-active);
  border-color: var(--border-glass-hover);
  color: var(--text-primary);
}

.knowledge-base-btn {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  background: var(--accent-soft-bg);
  border: 1px solid var(--accent-soft-border);
  color: var(--text-accent-purple);
  cursor: pointer;
  padding: 0.45rem 0.75rem;
  border-radius: 8px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 0.85rem;
}

.knowledge-base-btn:hover {
  background: var(--accent-soft-bg-hover);
  border-color: var(--accent-soft-border-hover);
  box-shadow: var(--shadow-glow-accent);
}

/* 顶部导航最终尺寸收敛，避免被前面重复样式放大 */
.chat-header .header-actions {
  display: flex;
  align-items: center;
  gap: 0.58rem;
  flex-wrap: wrap;
}

.chat-header .assistant-trigger-btn,
.chat-header .knowledge-base-btn {
  min-height: 40px;
  padding: 0 0.92rem;
  border-radius: 14px;
  font-size: 0.92rem;
  font-weight: 700;
  gap: 0.42rem;
  box-shadow: 0 8px 20px rgba(148, 163, 184, 0.1);
}

.chat-header .assistant-trigger-btn {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98) 0%, rgba(242, 249, 255, 0.98) 100%);
  border: 1px solid rgba(191, 219, 254, 0.9);
  color: #0f172a;
}

.chat-header .knowledge-base-btn {
  background: linear-gradient(180deg, rgba(233, 252, 245, 0.96) 0%, rgba(221, 247, 239, 0.96) 100%);
  border: 1px solid rgba(134, 239, 172, 0.82);
  color: #0f766e;
}

.chat-header .assistant-trigger-btn:hover,
.chat-header .knowledge-base-btn:hover {
  transform: translateY(-1px);
}

.chat-header .assistant-button-icon {
  width: 0.96rem;
  height: 0.96rem;
}

.chat-header .protocol-selector {
  min-height: 42px;
  gap: 0.54rem;
  padding: 0 0.78rem;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(148, 163, 184, 0.12);
}

.chat-header .protocol-select {
  min-width: 138px;
}

.chat-header .protocol-select :deep(.el-select__wrapper) {
  min-height: 38px !important;
}

.chat-header .protocol-select :deep(.el-select__selected-item),
.chat-header .protocol-select :deep(.el-select__placeholder),
.chat-header .protocol-select :deep(.el-select__input-text) {
  font-size: 0.9rem !important;
}

.chat-header .protocol-connection {
  gap: 0.36rem;
  font-size: 0.88rem;
}

.chat-header .protocol-status-icon {
  width: 0.88rem;
  height: 0.88rem;
}

.chat-header .protocol-status-dot {
  width: 0.42rem;
  height: 0.42rem;
}

.rag-selector {
  min-width: 240px;
}

.rag-select {
  width: 100%;
}

/* RAG选择器样式改进 */
.rag-select :deep(.el-input__wrapper) {
  background-color: var(--surface-base-alt) !important;
  border: 1px solid var(--border-default) !important;
  box-shadow: none !important;
  border-radius: 10px !important;
  transition: all 0.25s ease !important;
}

.rag-select :deep(.el-input__inner) {
  color: var(--text-primary) !important;
  font-weight: 500 !important;
  font-size: 14px !important;
}

.rag-select :deep(.el-input__wrapper:hover) {
  border-color: var(--border-strong) !important;
  background-color: var(--surface-elevated) !important;
}

.rag-select :deep(.el-input__wrapper.is-focus) {
  border-color: var(--border-focus) !important;
  box-shadow: 0 0 0 3px var(--border-focus-ring) !important;
  background-color: var(--surface-elevated) !important;
}

.rag-select :deep(.el-select__caret) {
  color: var(--text-secondary) !important;
}

.rag-select :deep(.el-tag) {
  background-color: var(--primary-soft-bg) !important;
  border-color: var(--primary-soft-border) !important;
  color: var(--text-accent) !important;
}

@media (max-width: 900px) {
  .rag-selector {
    min-width: 100%;
  }
}
</style>

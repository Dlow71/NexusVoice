<template>
  <div class="chat-container">
    <ConversationSidebar
        :history="conversationHistory"
        :active-id="route.params.id"
        @switch-conversation="handleSwitchConversation"
        @delete-conversation="handleDeleteConversation"
    />
    <div class="chat-view">
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
        <button
            @click="openAssistantPanel"
            class="assistant-trigger-btn"
            title="根据当前对话创建新角色"
        >
          ✨ 角色助手
        </button>
      </header>
      <div v-else class="chat-header-placeholder"></div>
      <main class="message-list" ref="messageListEl">
        <div v-if="isLoading" class="loading-messages">正在加载对话内容...</div>
        <div v-else-if="hasError" class="error-messages">
          {{ errorMessage }}
        </div>
        <ChatMessage
            v-else
            v-for="message in messages"
            :key="message.id"
            :message="message"
            :playing-index="
            message.sender === 'ai' ? currentPlayingIndex : null
          "
        />
      </main>
      <footer class="chat-footer">
        <div class="footer-options">
          <p class="system-message">{{ systemMessage }}</p>
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
        <button
            @click="toggleRecording"
            :class="['mic-button', { recording: isRecording }]"
            :disabled="isAIThinking || isLoading || hasError"
        >
          <i
              class="mic-icon"
              :class="isRecording ? 'stop-icon' : 'mic-icon-active'"
          ></i>
        </button>
      </footer>
    </div>

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
                  <div class="button-group"> <input type="file" ref="fileInput" @change="handleImageUpload" accept="image/*" style="display: none;">
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
import {ref, onMounted, onUnmounted, nextTick, watch} from "vue";
import {useRoute, useRouter} from "vue-router";
import ConversationSidebar from "../components/ConversationSidebar.vue";
import ChatMessage from "../components/ChatMessage.vue";
import characterApi from "../services/character";
import {ElMessage, ElMessageBox} from "element-plus";

const route = useRoute();
const router = useRouter();

const isLoading = ref(true);
const hasError = ref(false);
const errorMessage = ref("");
const currentCharacter = ref(null);
const messages = ref([]);
const conversationHistory = ref([]);
const messageListEl = ref(null);
const enableWebSearch = ref(false);
const enableAudio = ref(true);
const isRecording = ref(false);
const isAIThinking = ref(false);
const systemMessage = ref("正在加载...");
let recognition;

// --- 音频播放状态 ---
const currentPlayingIndex = ref(null);
const audioQueue = ref([]);
const currentAudio = ref(null);

// --- 角色助手状态 ---
const isAssistantPanelVisible = ref(false);
const isAssistantLoading = ref(false);
const assistantStep = ref("initial");
const roleBrief = ref(null);
const researchTasks = ref([]);

// --- 表单状态 ---
const fileInput = ref(null);
const isUploading = ref(false);
const isGeneratingImage = ref(false) //ai生成头像
const voiceList = ref([]);
let currentPreviewAudio = null;

const stopAudioPlayback = () => {
  if (currentAudio.value) {
    currentAudio.value.pause();
    currentAudio.value.src = ""; // 释放资源
    currentAudio.value = null;
  }
  audioQueue.value = [];
  currentPlayingIndex.value = null;
  if (!isAIThinking.value && !isRecording.value) {
    systemMessage.value = "点击麦克风开始对话";
  }
};

const loadConversation = async (convId) => {
  isLoading.value = true;
  hasError.value = false;
  errorMessage.value = "";
  systemMessage.value = "正在加载会话...";
  messages.value = [];
  currentCharacter.value = null;
  stopAudioPlayback(); // 切换会话时停止播放

  try {
    const response = await characterApi.getMessagesByConversationId(convId);
    if (!response || !response.data) throw new Error("无效的API响应");
    if (response.data.success) {
      const data = response.data.data;
      if (!data) throw new Error("响应数据体为空");
      if (Array.isArray(data) && data.length > 0 && data[0].conversationRole) {
        currentCharacter.value = data[0].conversationRole;
      } else {
        console.warn("会话数据中缺少角色信息");
        currentCharacter.value = {name: "未知角色", avatarUrl: ""};
      }
      messages.value = (Array.isArray(data) ? data : []).map((msg) => ({
        id: msg.id,
        text: msg.content,
        sender: msg.role === "USER" ? "user" : "ai",
        // 注意：历史消息可能没有ttsSegments，需要兼容
        ttsSegments: msg.ttsSegments || [],
      }));

      if (
          messages.value.length === 1 &&
          messages.value[0].sender === "ai" &&
          currentCharacter.value &&
          currentCharacter.value.greetingAudioUrl &&
          enableAudio.value
      ) {
        setTimeout(() => {
          // 开场白通常是单个文件，直接播放
          const audio = new Audio(currentCharacter.value.greetingAudioUrl);
          currentAudio.value = audio;
          systemMessage.value = "正在播放开场白...";
          audio.play();
          audio.onended = () => {
            systemMessage.value = "点击麦克风开始对话";
            currentAudio.value = null;
          };
        }, 500);
      }
      systemMessage.value = "请点击麦克风开始对话";
      await scrollToBottom();
    } else {
      throw new Error(response.data.message || "加载会话失败");
    }
  } catch (error) {
    console.error("加载会话失败:", error);
    hasError.value = true;
    errorMessage.value = error.message || "加载会话时发生未知错误，请刷新重试。";
    systemMessage.value = "加载失败";
    ElMessage.error(errorMessage.value);
  } finally {
    isLoading.value = false;
  }
};

const fetchConversationHistory = async () => {
  try {
    const response = await characterApi.getConversationHistory();
    if (response.data.success) {
      conversationHistory.value = response.data.data;
    }
  } catch (error) {
    console.error("获取会话历史失败:", error);
    ElMessage.error("获取会话历史失败");
  }
};

const playNextSegment = () => {
  if (audioQueue.value.length === 0) {
    currentPlayingIndex.value = null;
    currentAudio.value = null;
    systemMessage.value = "播放完毕";
    // 延迟一会再恢复默认提示
    setTimeout(() => {
      if (!isRecording.value && !isAIThinking.value) {
        systemMessage.value = "点击麦克风开始对话";
      }
    }, 1500);
    return;
  }

  const segment = audioQueue.value.shift();
  currentPlayingIndex.value = segment.index;
  systemMessage.value = `正在播放片段 ${segment.index + 1}...`;

  const audio = new Audio(segment.url);
  currentAudio.value = audio;

  audio.addEventListener("ended", playNextSegment);
  audio.addEventListener("error", () => {
    console.error(`播放音频失败: ${segment.url}`);
    ElMessage.error(`片段 ${segment.index + 1} 播放失败`);
    playNextSegment(); // 尝试播放下一个
  });

  audio.play().catch(e => {
    console.error("播放时发生错误:", e);
    // 如果播放被中断或失败，也尝试继续队列
    playNextSegment();
  });
};

const playSegmentedAudio = (segments) => {
  stopAudioPlayback(); // 开始新播放前，先停止旧的
  if (segments && segments.length > 0) {
    // 确保按index排序
    audioQueue.value = [...segments].sort((a, b) => a.index - b.index);
    playNextSegment();
  }
};

const processUserMessage = async (text) => {
  const conversationId = route.params.id;
  if (!currentCharacter.value || !conversationId) {
    ElMessage.error("会话或角色信息丢失，无法发送消息");
    return;
  }
  // 在发送用户消息时停止任何正在播放的音频
  stopAudioPlayback();
  addMessage(text, "user");
  const isNewConversation = !messages.value.some((m) => m.sender === "user");

  try {
    isAIThinking.value = true;
    systemMessage.value = `${currentCharacter.value.name} 正在思考...`;
    const response = await characterApi.getLLMReply({
      conversationId: conversationId,
      message: text,
      roleId: currentCharacter.value.id,
      enableWebSearch: enableWebSearch.value,
      enableAudio: enableAudio.value,
    });
    if (response.data.success) {
      const responseData = response.data.data;
      if (responseData.content) {
        // 创建消息对象时，附带上 ttsSegments
        addMessage(responseData.content, "ai", responseData.ttsSegments);
        if (enableAudio.value) {
          playSegmentedAudio(responseData.ttsSegments);
        }
      }
      if (isNewConversation) fetchConversationHistory();
    } else {
      throw new Error(response.data.message || "AI响应错误");
    }
  } catch (error) {
    console.error("处理用户消息时出错:", error);
    ElMessage.error(error.message || "发送消息失败");
    addMessage("抱歉，我暂时无法回复。", "ai");
  } finally {
    isAIThinking.value = false;
    // 如果没有开启音频，思考结束后立即恢复提示
    if (!enableAudio.value) {
      systemMessage.value = "点击麦克风开始对话";
    }
  }
};

const handleSwitchConversation = (conversationId) => {
  if (route.params.id !== conversationId) {
    stopAudioPlayback(); // 切换前停止音频
    router.push(`/chat/${conversationId}`);
  }
};

const handleDeleteConversation = async (conversationId) => {
  stopAudioPlayback(); // 删除前也停止音频
  try {
    await ElMessageBox.confirm(
        "此操作将永久删除该对话, 是否继续?",
        "警告",
        {
          confirmButtonText: "确认删除",
          cancelButtonText: "取消",
          type: "warning",
        }
    );
    try {
      await characterApi.deleteConversation(conversationId);
      ElMessage.success("对话删除成功！");
      const index = conversationHistory.value.findIndex(
          (c) => c.id === conversationId
      );
      if (index !== -1) {
        conversationHistory.value.splice(index, 1);
      }
      if (route.params.id === conversationId) {
        router.push("/");
      }
    } catch (apiError) {
      console.error("删除对话API调用失败:", apiError);
      ElMessage.error(
          apiError.response?.data?.message || "删除对话失败，请稍后再试。"
      );
    }
  } catch (cancelAction) {
    ElMessage.info("已取消删除");
  }
};

const openAssistantPanel = () => {
  assistantStep.value = "initial";
  roleBrief.value = null;
  researchTasks.value = [];
  isAssistantPanelVisible.value = true;
};

const handleGenerateBrief = async () => {
  isAssistantLoading.value = true;
  try {
    const response = await characterApi.generateRoleBrief(route.params.id);
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
  isAssistantLoading.value = true;
  try {
    const response = await characterApi.getResearchTasks(route.params.id);
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
  isAssistantLoading.value = true;
  const payload = {
    conversationId: route.params.id,
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

const addMessage = (text, sender, ttsSegments = []) => {
  messages.value.push({id: Date.now(), text, sender, ttsSegments});
  scrollToBottom();
};

const scrollToBottom = async () => {
  await nextTick();
  if (messageListEl.value) {
    messageListEl.value.scrollTop = messageListEl.value.scrollHeight;
  }
};

const goBack = () => router.push("/");

const getAvatarUrl = (url) =>
    url || new URL("../assets/placeholder.svg", import.meta.url).href;

const initializeSpeechRecognition = () => {
  const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    systemMessage.value = "浏览器不支持语音识别功能";
    return;
  }
  recognition = new SpeechRecognition();
  recognition.continuous = false;
  recognition.lang = "zh-CN";
  recognition.interimResults = false;
  recognition.onstart = () => {
    isRecording.value = true;
    systemMessage.value = "正在倾听...";
  };
  recognition.onend = () => {
    isRecording.value = false;
    if (!isAIThinking.value && audioQueue.value.length === 0) {
      systemMessage.value = "点击麦克风开始对话";
    }
  };
  recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript.trim();
    if (transcript) processUserMessage(transcript);
  };
  recognition.onerror = (event) => {
    let errorMsg = "发生了一个错误";
    if (event.error === "not-allowed") errorMsg = "需要麦克风授权";
    else if (event.error === "no-speech") errorMsg = "未检测到语音";
    systemMessage.value = `错误: ${errorMsg}`;
    if (isRecording.value) isRecording.value = false;
  };
};

const toggleRecording = () => {
  if (!recognition || isAIThinking.value) return;
  stopAudioPlayback(); // 开始录音前停止所有播放
  if (isRecording.value) {
    recognition.stop();
  } else {
    recognition.start();
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
//ai 生成图片
const handleImageGeneration=async () => {
  if (!roleBrief.value || !roleBrief.value.description) {
      ElMessage.warning("请先填写角色描述，AI需要根据描述来生成头像");
      return;
  }
  isGeneratingImage.value=true;
  try {
    // 从角色描述和名称中提炼一个更丰富的 prompt
    const prompt = `为一位名叫“${roleBrief.value.name}”的角色生成一张头像。角色特征：${roleBrief.value.description}`;

    const response = await characterApi.generateImage(prompt);

    if (response.data && response.data.success && response.data.data.imageUrls.length > 0) {
      const imageUrl = response.data.data.imageUrls[0];
      roleBrief.value.avatarUrl = imageUrl; // 将生成的图片URL赋值给头像
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
}
watch(
    () => route.params.id,
    (newId) => {
      if (newId) loadConversation(newId);
    },
    {immediate: true}
);

onMounted(() => {
  initializeSpeechRecognition();
  fetchConversationHistory();
  fetchVoiceList();
});

onUnmounted(() => {
  if (recognition) recognition.stop();
  stopAudioPlayback(); // 组件卸载时清理
});
</script>

<style scoped>
/* --- 基础布局 --- */
.chat-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: var(--bg-main);
  color: var(--text-primary);
}

.chat-view {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  min-width: 300px;
}

/* --- 头部 Header --- */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.75rem 1.5rem;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
  z-index: 10;
}

.character-info {
  display: flex;
  align-items: center;
}

.chat-header-placeholder {
  height: 65px;
  flex-shrink: 0;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.back-button {
  background: none;
  border: none;
  color: var(--text-secondary);
  font-size: 1.5rem;
  cursor: pointer;
  margin-right: 1rem;
  padding: 0.5rem;
  border-radius: 50%;
  transition: background-color 0.2s, color 0.2s;
}

.back-button:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: var(--text-primary);
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  margin-right: 1rem;
  object-fit: cover;
  border: 2px solid var(--border-color);
}

.name {
  font-size: 1.2rem;
  font-weight: 600;
}

/* --- 消息列表 --- */
.message-list {
  flex-grow: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

.loading-messages,
.error-messages {
  text-align: center;
  color: var(--text-secondary);
  padding: 4rem 1rem;
  font-size: 1.2rem;
}

.error-messages {
  color: #ef4444;
  border: 1px dashed #ef4444;
  border-radius: 8px;
  margin: 2rem;
}

/* --- 底部 Footer --- */
.chat-footer {
  padding: 1rem 1.5rem;
  background-color: var(--bg-secondary);
  border-top: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.footer-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  max-width: 500px;
  margin-bottom: 1.25rem;
  min-height: 1.2rem;
}

.system-message {
  color: var(--text-muted);
  font-size: 0.875rem;
  text-align: left;
  flex-grow: 1;
}

/* --- 麦克风按钮 --- */
.mic-button {
  width: 72px;
  height: 72px;
  background-color: var(--primary-color);
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.2s ease-in-out;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.2);
}

.mic-button:hover:not(:disabled) {
  background-color: var(--primary-color-hover);
  transform: scale(1.1);
}

.mic-button:disabled {
  background-color: var(--text-muted);
  cursor: not-allowed;
  transform: scale(1);
}

.mic-button.recording {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(74, 144, 226, 0.7);
  }
  70% {
    box-shadow: 0 0 0 25px rgba(74, 144, 226, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(74, 144, 226, 0);
  }
}

.mic-icon {
  width: 28px;
  height: 28px;
  background-color: white;
}

.mic-icon-active {
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M192 0C139 0 96 43 96 96V256c0 53 43 96 96 96s96-43 96-96V96c0-53-43-96-96-96zM64 216c0-13.3-10.7-24-24-24s-24 10.7-24 24v40c0 89.1 66.2 162.7 152 174.4V464H120c-13.3 0-24 10.7-24 24s10.7 24 24 24h144c13.3 0 24-10.7 24-24s-10.7-24-24-24H200v-33.6c85.8-11.7 152-85.3 152-174.4V216c0-13.3-10.7-24-24-24s-24 10.7-24 24v40c0 70.7-57.3 128-128 128s-128-57.3-128-128V216z'/%3E%3C/svg%3E") no-repeat center;
}

.stop-icon {
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M0 128C0 92.7 28.7 64 64 64H320c35.3 0 64 28.7 64 64V384c0 35.3-28.7 64-64 64H64c-35.3 0-64-28.7-64-64V128z'/%3E%3C/svg%3E") no-repeat center;
}

/* --- 开关 Switch --- */
.search-toggle-container {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: var(--text-secondary);
  font-size: 0.875rem;
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
  background-color: var(--primary-color);
}

input:focus + .slider {
  box-shadow: 0 0 1px var(--primary-color);
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

/* --- 角色助手 --- */
.assistant-trigger-btn {
  background-color: #4a5568;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  font-weight: 500;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.assistant-trigger-btn:hover {
  background-color: #6b7280;
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
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--text-secondary);
  font-size: 0.875rem;
  font-weight: 500;
}

.form-group input,
.form-group textarea {
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

/* --- 角色助手面板优化样式 --- */

/* 优化表单组的间距和标签样式 */
.assistant-panel .form-group {
  margin-bottom: 1.75rem;
}

.assistant-panel .form-group label {
  font-weight: 500;
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin-bottom: 0.6rem;
  display: block;
}

/* --- 头像上传区域优化 --- */
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

/* --- TTS声音选择区域优化 --- */
.voice-type-group {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.voice-type-group select {
  flex-grow: 1;
  /* 美化下拉框，移除原生箭头并替换为SVG */
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
  background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3e%3cpath stroke='%239ca3af' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='M6 8l4 4 4-4'/%3e%3c/svg%3e");
  background-position: right 0.75rem center;
  background-repeat: no-repeat;
  background-size: 1.25em;
  padding-right: 2.5rem; /* 为自定义箭头留出空间 */
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
/*ai生成头像样式*/
.upload-inputs .button-group {
  display: flex;
  gap: 0.75rem; /* 让按钮之间有空隙 */
}

.button-group button {
  flex-grow: 1; /* 让两个按钮平分空间 */
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

.btn-generate:disabled {
  background-color: #9ca3af;
  cursor: not-allowed;
  opacity: 0.7;
}
</style>
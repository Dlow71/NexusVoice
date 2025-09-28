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
        <button @click="goBack" class="back-button">&lt;</button>
        <img
            :src="getAvatarUrl(currentCharacter.avatarUrl)"
            alt="avatar"
            class="avatar"
        />
        <h2 class="name">{{ currentCharacter.name }}</h2>
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
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import ConversationSidebar from "../components/ConversationSidebar.vue";
import ChatMessage from "../components/ChatMessage.vue";
import characterApi from "../services/character";
import { ElMessage,ElMessageBox } from "element-plus";

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
const isRecording = ref(false);
const isAIThinking = ref(false);
const systemMessage = ref("正在加载...");
let recognition;

const loadConversation = async (convId) => {
  isLoading.value = true;
  hasError.value = false;
  errorMessage.value = "";
  systemMessage.value = "正在加载会话...";
  messages.value = [];
  currentCharacter.value = null;
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
        currentCharacter.value = { name: "未知角色", avatarUrl: "" };
      }
      messages.value = (Array.isArray(data) ? data : []).map((msg) => ({
        id: msg.id,
        text: msg.content,
        sender: msg.role === "USER" ? "user" : "ai",
      }));
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

// 恢复为您原来的 processUserMessage 逻辑
const processUserMessage = async (text) => {
  const conversationId = route.params.id;
  if (!currentCharacter.value || !conversationId) {
    ElMessage.error("会话或角色信息丢失，无法发送消息");
    return;
  }
  addMessage(text, "user");
  const isNewConversation = !messages.value.some((m) => m.sender === "user");
  try {
    isAIThinking.value = true;
    systemMessage.value = `${currentCharacter.value.name} 正在思考...`;

    //调用大模型回复
    const response = await characterApi.getLLMReply({
      conversationId: conversationId,
      message: text,
      roleId: currentCharacter.value.id,
      enableWebSearch: enableWebSearch.value,
    });

    if (response.data.success) {
      const aiResponseText = response.data.data.content;
      const audioUrl = response.data.data.audioUrl;
      if (aiResponseText) {
        addMessage(aiResponseText, "ai");
        if (audioUrl) await playAudio(audioUrl);
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
    systemMessage.value = "点击麦克风开始对话";
  }
};

const handleSwitchConversation = (conversationId) => {
  if (route.params.id !== conversationId) {
    router.push(`/chat/${conversationId}`);
  }
};

const handleDeleteConversation = async (conversationId) => {
  try {
    // 调用 Element Plus 的确认弹窗，它会返回一个 Promise
    await ElMessageBox.confirm(
        "此操作将永久删除该对话, 是否继续?", // 提示内容
        "警告", // 标题
        {
          confirmButtonText: "确认删除",
          cancelButtonText: "取消",
          type: "warning",
        }
    );

    // 如果用户点击了“确认删除”，代码会继续执行到这里
    try {
      await characterApi.deleteConversation(conversationId);
      ElMessage.success("对话删除成功！");

      const index = conversationHistory.value.findIndex(c => c.id === conversationId);
      if (index !== -1) {
        conversationHistory.value.splice(index, 1);
      }

      if (route.params.id === conversationId) {
        router.push("/");
      }
    } catch (apiError) {
      console.error("删除对话API调用失败:", apiError);
      ElMessage.error(apiError.response?.data?.message || "删除对话失败，请稍后再试。");
    }

  } catch (cancelAction) {
    // 如果用户点击了“取消”或关闭弹窗，Promise 会被 reject，代码会进入 catch 块
    ElMessage.info("已取消删除");
  }
};

const addMessage = (text, sender) => {
  messages.value.push({id: Date.now(), text, sender});
  scrollToBottom();
};
const scrollToBottom = async () => {
  await nextTick();
  if (messageListEl.value) {
    messageListEl.value.scrollTop = messageListEl.value.scrollHeight;
  }
};
const goBack = () => {
  router.push("/");
};
const getAvatarUrl = (url) => {
  return url || new URL("../assets/placeholder.svg", import.meta.url).href;
};
const playAudio = async (url) => {
  systemMessage.value = "正在播放...";
  const audio = new Audio(url);
  await audio.play();
  systemMessage.value = "播放完毕";
};
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
    if (!isAIThinking.value) {
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
  if (isRecording.value) {
    recognition.stop();
  } else {
    recognition.start();
  }
};

watch(
    () => route.params.id,
    (newId) => {
      if (newId) loadConversation(newId);
    },
    { immediate: true }
);
onMounted(() => {
  initializeSpeechRecognition();
  fetchConversationHistory();
});
onUnmounted(() => {
  if (recognition) recognition.stop();
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
  padding: 0.75rem 1.5rem;
  background-color: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
  z-index: 10;
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

/* --- 麦克风按钮（蓝色主题） --- */
.mic-button {
  width: 72px;
  height: 72px;
  background-color: #3b82f6; /* 蓝色 */
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
  background-color: #2563eb; /* hover 更深蓝 */
  transform: scale(1.1);
}

.mic-button:disabled {
  background-color: #9ca3af; /* 灰色 */
  cursor: not-allowed;
  transform: scale(1);
}

.mic-button.recording {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.7);
  }
  70% {
    box-shadow: 0 0 0 25px rgba(59, 130, 246, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(59, 130, 246, 0);
  }
}

.mic-icon {
  width: 28px;
  height: 28px;
  background-color: white;
}

.mic-icon-active {
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M192 0C139 0 96 43 96 96V256c0 53 43 96 96 96s96-43 96-96V96c0-53-43-96-96-96zM64 216c0-13.3-10.7-24-24-24s-24 10.7-24 24v40c0 89.1 66.2 162.7 152 174.4V464H120c-13.3 0-24 10.7-24 24s10.7 24 24 24h144c13.3 0 24-10.7 24-24s-10.7-24-24-24H200v-33.6c85.8-11.7 152-85.3 152-174.4V216c0-13.3-10.7-24-24-24s-24 10.7-24 24v40c0 70.7-57.3 128-128 128s-128-57.3-128-128V216z'/%3E%3C/svg%3E")
  no-repeat center;
}

.stop-icon {
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M0 128C0 92.7 28.7 64 64 64H320c35.3 0 64 28.7 64 64V384c0 35.3-28.7 64-64 64H64c-35.3 0-64-28.7-64-64V128z'/%3E%3C/svg%3E")
  no-repeat center;
}

/* --- 开关 Switch（蓝色主题） --- */
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
  background-color: #93c5fd; /* 默认浅蓝 */
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
  background-color: #3b82f6; /* 选中蓝色 */
}

input:focus + .slider {
  box-shadow: 0 0 1px #2563eb;
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

</style>
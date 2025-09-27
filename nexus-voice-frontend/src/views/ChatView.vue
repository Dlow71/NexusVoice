<template>
  <div class="chat-container">
    <ConversationSidebar
        :history="conversationHistory"
        :active-id="conversationId"
        @switch-conversation="switchConversation"
        @new-conversation="startNewConversation"
    />

    <div class="chat-view">
      <header class="chat-header" v-if="currentCharacter">
        <button @click="goBack" class="back-button">&lt;</button>
        <img :src="getAvatarUrl(currentCharacter.avatar)" alt="avatar" class="avatar">
        <h2 class="name">{{ currentCharacter.name }}</h2>
      </header>

      <div v-if="messages.length === 0" class="welcome-screen">
        <h1>与 {{ currentCharacter?.name || '角色' }} 对话</h1>
        <p>点击下方的麦克风按钮开始</p>
      </div>

      <main v-else class="message-list" ref="messageListEl">
        <ChatMessage
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
              <input type="checkbox" v-model="enableWebSearch">
              <span class="slider round"></span>
            </label>
            <span>联网搜索</span>
          </div>
        </div>
        <button
            @click="toggleRecording"
            :class="['mic-button', { recording: isRecording }]"
            :disabled="isAIThinking"
        >
          <i class="mic-icon" :class="isRecording ? 'stop-icon' : 'mic-icon-active'"></i>
        </button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted, onUnmounted, nextTick} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import ConversationSidebar from '../components/ConversationSidebar.vue';
import ChatMessage from '../components/ChatMessage.vue';
import characterApi from '../services/character';

// --- Vue Router 和基础数据 ---
const route = useRoute();
const router = useRouter();
const allCharacters = ref([
  {id: 'harry_potter', name: '哈利·波特', avatar: 'placeholder.svg', systemPrompt: "你将扮演哈利·波特..."},
  {id: 'socrates', name: '苏格拉底', avatar: 'placeholder.svg', systemPrompt: "你将扮演古LG希腊哲学家苏格拉底..."},
  {id: 'eva_explorer', name: '宇航员伊娃', avatar: 'placeholder.svg', systemPrompt: "你将扮演星际宇航员伊娃..."},
]);
const currentCharacter = ref(null);
const messages = ref([]);
const messageListEl = ref(null);
const conversationId = ref(null);

// 会话历史状态
const conversationHistory = ref([]);
const enableWebSearch = ref(false);

// 语音功能核心状态
const isRecording = ref(false);
const isAIThinking = ref(false);
const systemMessage = ref('点击麦克风开始对话');
let recognition;

// --- STT 初始化 与 数据获取 ---
onMounted(() => {
  initializeSpeechRecognition();

  const characterId = route.params.id;
  currentCharacter.value = allCharacters.value.find(c => c.id === characterId);
  if (!currentCharacter.value) {
    systemMessage.value = "错误：未找到角色信息。";
  } else {
    startNewConversation();
  }

  fetchConversationHistory();
});

onUnmounted(() => {
  if (recognition) {
    recognition.stop();
    recognition.onstart = null;
    recognition.onend = null;
    recognition.onresult = null;
    recognition.onerror = null;
  }
});

// --- 核心业务逻辑函数 ---
const fetchConversationHistory = async () => {
  try {
    const response = await characterApi.getConversationHistory();
    // 后端直接返回了 { success: true, data: [...] } 格式
    if (response.data.success) {
      conversationHistory.value = response.data.data;
    }
  } catch (error) {
    console.error("获取会话历史失败:", error);

  }
};

const switchConversation = async (id) => {
  if (conversationId.value === id) return;

  conversationId.value = id;
  systemMessage.value = "正在加载历史消息...";
  messages.value = [];

  try {
    const response = await characterApi.getMessagesByConversationId(id);
    if (response.data.success) {
      messages.value = response.data.data.map(msg => ({
        id: msg.id,
        text: msg.content,
        sender: msg.role === 'USER' ? 'user' : 'ai',
      }));
      await scrollToBottom();
    }
    systemMessage.value = '点击麦克风继续对话';
  } catch (error) {
    console.error("获取历史消息失败:", error);
    systemMessage.value = '历史消息加载失败';
  }
};

const startNewConversation = () => {
  conversationId.value = null;
  messages.value = [];
  systemMessage.value = '点击麦克风开始新对话';
};

const processUserMessage = async (text) => {
  if (!currentCharacter.value) {
    addMessage("抱歉，内部出现错误，角色信息丢失了。", "ai");
    return;
  }

  // 在API调用前，记录下这是否为一个新对话
  const isNewConversation = conversationId.value === null;

  try {
    isAIThinking.value = true;
    systemMessage.value = `${currentCharacter.value.name} 正在思考...`;

    const response = await characterApi.getLLMReply({
      message: text,
      conversationId: conversationId.value,
      systemPrompt: currentCharacter.value.systemPrompt,
      enableWebSearch: enableWebSearch.value
    });

    if (response.data && response.data.code === 200 && response.data.success === true) {
      const responseData = response.data.data;

      if (responseData && responseData.content) {
        const aiResponseText = responseData.content;
        const audioUrl = responseData.audioUrl;

        // 更新 conversationId
        conversationId.value = responseData.conversationId;

        // 如果之前是新对话，现在就调用函数刷新侧边栏
        if (isNewConversation) {
          fetchConversationHistory();
        }

        addMessage(aiResponseText, 'ai');

        if (audioUrl) {
          await playAudio(audioUrl);
        }

      } else {
        throw new Error("AI未能生成有效的回复文本。");
      }
    } else {
      throw new Error(response.data.message || "AI 响应错误");
    }

  } catch (error) {
    console.error("处理用户消息时出错:", error);
    const errorMsg = error.response?.data?.message || error.message || "处理您的消息时遇到了点麻烦";
    addMessage(errorMsg, "ai");
  } finally {
    isAIThinking.value = false;
    systemMessage.value = '点击麦克风开始对话';
  }
};


// --- STT, TTS 及辅助函数 ---

const playAudio = async (url) => {
  systemMessage.value = '正在播放...';
  try {
    const audio = new Audio(url);
    audio.play();

    return new Promise((resolve, reject) => {
      audio.onended = () => {
        systemMessage.value = '播放完毕';
        resolve();
      };
      audio.onerror = (e) => {
        console.error('音频播放失败:', e);
        systemMessage.value = '音频播放失败';
        reject(new Error('音频播放失败'));
      };
    });
  } catch (error) {
    console.error("播放音频时出错:", error);
    systemMessage.value = `音频播放失败`;
    throw error;
  }
};

const initializeSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    systemMessage.value = "浏览器不支持语音识别功能";
    return;
  }
  recognition = new SpeechRecognition();
  recognition.continuous = false;
  recognition.lang = 'zh-CN';
  recognition.interimResults = false;

  recognition.onstart = () => {
    isRecording.value = true;
    systemMessage.value = '正在倾听...';
  };
  recognition.onend = () => {
    isRecording.value = false;
    if (!isAIThinking.value) systemMessage.value = '点击麦克风开始对话';
  };
  recognition.onresult = (event) => {
    const transcript = event.results[0][0].transcript.trim();
    if (transcript) {
      addMessage(transcript, 'user');
      processUserMessage(transcript);
    }
  };
  recognition.onerror = (event) => {
    console.error('语音识别错误:', event.error);
    let errorMsg = event.error === 'not-allowed' ? '需要麦克风授权' : '未检测到语音';
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

const addMessage = (text, sender) => {
  messages.value.push({id: Date.now(), text, sender});
  scrollToBottom();
};

const scrollToBottom = async () => {
  await nextTick();
  const el = messageListEl.value;
  if (el) el.scrollTop = el.scrollHeight;
};

const goBack = () => router.push('/');

const getAvatarUrl = (name) => new URL(`../assets/${name}`, import.meta.url).href;
</script>

<style scoped>
/* 主容器， flex 布局 */
.chat-container {
  display: flex;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  background-color: #111827;
}

/* 主聊天区占据剩余空间 */
.chat-view {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  background-color: #111827;
  min-width: 300px;
}

/* 欢迎界面样式 */
.welcome-screen {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: #9ca3af;
}

.welcome-screen h1 {
  font-size: 2rem;
  font-weight: bold;
}

.welcome-screen p {
  font-size: 1.1rem;
  margin-top: 0.5rem;
}

/* chat-header, message-list, chat-footer 的内部样式 */
.chat-header {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  background-color: #1e1e1e;
  border-bottom: 1px solid #374151;
  flex-shrink: 0;
}

.message-list {
  flex-grow: 1;
  overflow-y: auto;
  padding: 1.5rem;
}

.chat-footer {
  padding: 1rem;
  background-color: #1e1e1e;
  border-top: 1px solid #374151;
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
  max-width: 400px;
  margin-bottom: 1rem;
}

.search-toggle-container {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #9ca3af;
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
  -webkit-transition: .4s;
  transition: .4s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  -webkit-transition: .4s;
  transition: .4s;
}

input:checked + .slider {
  background-color: #3b82f6;
}

input:focus + .slider {
  box-shadow: 0 0 1px #3b82f6;
}

input:checked + .slider:before {
  -webkit-transform: translateX(18px);
  -ms-transform: translateX(18px);
  transform: translateX(18px);
}

.slider.round {
  border-radius: 22px;
}

.slider.round:before {
  border-radius: 50%;
}

.back-button {
  background: none;
  border: none;
  color: white;
  font-size: 1.5rem;
  cursor: pointer;
  margin-right: 1rem;
}

.avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  margin-right: 1rem;
}

.name {
  font-size: 1.2rem;
  font-weight: bold;
}

.system-message {
  color: #888;
  font-size: 0.9rem;
  height: 1.2rem;
  margin: 0;
  text-align: left;
  flex-grow: 1;
}

.mic-button {
  width: 72px;
  height: 72px;
  background-color: #3b82f6;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.2s ease;
}

.mic-button:hover:not(:disabled) {
  background-color: #2563eb;
  transform: scale(1.1);
}

.mic-button:disabled {
  background-color: #4b5563;
  cursor: not-allowed;
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
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M192 0C139 0 96 43 96 96V256c0 53 43 96 96 96s96-43 96-96V96c0-53-43-96-96-96zM64 216c0-13.3-10.7-24-24-24s-24 10.7-24 24v40c0 89.1 66.2 162.7 152 174.4V464H120c-13.3 0-24 10.7-24 24s10.7 24 24 24h144c13.3 0 24-10.7 24-24s-10.7-24-24-24H200v-33.6c85.8-11.7 152-85.3 152-174.4V216c0-13.3-10.7-24-24-24s-24 10.7-24 24v40c0 70.7-57.3 128-128 128s-128-57.3-128-128V216z'/%3E%3C/svg%3E") no-repeat center;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M192 0C139 0 96 43 96 96V256c0 53 43 96 96 96s96-43 96-96V96c0-53-43-96-96-96zM64 216c0-13.3-10.7-24-24-24s-24 10.7-24 24v40c0 89.1 66.2 162.7 152 174.4V464H120c-13.3 0-24 10.7-24 24s10.7 24 24 24h144c13.3 0 24-10.7 24-24s-10.7-24-24-24H200v-33.6c85.8-11.7 152-85.3 152-174.4V216c0-13.3-10.7-24-24-24s-24 10.7-24 24v40c0 70.7-57.3 128-128 128s-128-57.3-128-128V216z'/%3E%3C/svg%3E") no-repeat center;
}

.stop-icon {
  -webkit-mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M0 128C0 92.7 28.7 64 64 64H320c35.3 0 64 28.7 64 64V384c0 35.3-28.7 64-64 64H64c-35.3 0-64-28.7-64-64V128z'/%3E%3C/svg%3E") no-repeat center;
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M0 128C0 92.7 28.7 64 64 64H320c35.3 0 64 28.7 64 64V384c0 35.3-28.7 64-64 64H64c-35.3 0-64-28.7-64-64V128z'/%3E%3C/svg%3E") no-repeat center;
}
</style>
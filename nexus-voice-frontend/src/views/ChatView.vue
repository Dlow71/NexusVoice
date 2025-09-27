<template>
  <div class="chat-view">
    <!-- 顶部的 header -->
    <header class="chat-header" v-if="currentCharacter">
      <button @click="goBack" class="back-button">&lt;</button>
      <img :src="getAvatarUrl(currentCharacter.avatar)" alt="avatar" class="avatar">
      <h2 class="name">{{ currentCharacter.name }}</h2>
    </header>

    <!-- 中间的消息列表区域 -->
    <main class="message-list" ref="messageListEl">
      <ChatMessage
          v-for="message in messages"
          :key="message.id"
          :message="message"
      />
    </main>

    <!-- 底部的控制区域 -->
    <footer class="chat-footer">
      <p class="system-message">{{ systemMessage }}</p>

      <!-- 核心：麦克风按钮 -->
      <button
          @click="toggleRecording"
          :class="['mic-button', { recording: isRecording }]"
          :disabled="isAIThinking"
      >
        <i class="mic-icon" :class="isRecording ? 'stop-icon' : 'mic-icon-active'"></i>
      </button>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ChatMessage from '../components/ChatMessage.vue';
// 1. 引入 chat.js API 服务
import chatApi from '../services/character.js';

// --- Vue Router 和基础数据 ---
const route = useRoute();
const router = useRouter();
const allCharacters = ref([
  // 为每个角色添加 systemPrompt
  {
    id: 'harry_potter',
    name: '哈利·波特',
    avatar: 'placeholder.svg',
    systemPrompt: "你将扮演哈利·波特。你的性格勇敢、有点叛逆但内心善良。你对朋友非常忠诚。你可以分享你在霍格沃茨的经历，谈论魔法、魁地奇比赛和你的朋友罗恩、赫敏。使用年轻、友好的语气。不要说你是AI或语言模型。"
  },
  {
    id: 'socrates',
    name: '苏格拉底',
    avatar: 'placeholder.svg',
    systemPrompt: "你将扮演古希腊哲学家苏格拉底。你的核心方法是“诘问法”，即通过不断提问来引导对方思考，而不是直接给出答案。你的语气谦逊而充满智慧，经常说“我唯一知道的是我一无所知”。"
  },
  {
    id: 'eva_explorer',
    name: '宇航员伊娃',
    avatar: 'placeholder.svg',
    systemPrompt: "你将扮演星际宇航员伊娃。你的性格开朗、乐观、充满好奇心。你热爱探索未知，对科学和宇宙有极大的热情。你可以分享你在太空中的经历，描述你见过的奇特星球和外星景象。"
  },
]);
const currentCharacter = ref(null);
const messages = ref([]);
const messageListEl = ref(null);
// 用于维持多轮对话
const conversationId = ref(null);

// --- 语音功能核心状态 ---
const isRecording = ref(false);
const isAIThinking = ref(false);
const systemMessage = ref('点击麦克风开始对话');
let recognition; // 语音识别实例

// --- STT 初始化 ---
onMounted(() => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.lang = 'zh-CN';
    recognition.interimResults = false;

    // --- STT 事件监听 ---
    recognition.onstart = () => { isRecording.value = true; systemMessage.value = '正在倾听...'; };
    recognition.onend = () => { isRecording.value = false; if (!isAIThinking.value) systemMessage.value = '点击麦克风开始对话'; };
    recognition.onresult = (event) => {
      const transcript = event.results[0][0].transcript.trim();
      if (transcript) {
        addMessage(transcript, 'user');
        processUserMessage(transcript);
      }
    };
    recognition.onerror = (event) => {
      console.error('语音识别错误:', event.error);
      let errorMsg = '发生了一个错误';
      if (event.error === 'not-allowed') errorMsg = '需要麦克风授权';
      else if (event.error === 'no-speech') errorMsg = '未检测到语音';
      systemMessage.value = `错误: ${errorMsg}`;
      if (isRecording.value) isRecording.value = false;
    };
  } else {
    systemMessage.value = "浏览器不支持语音识别功能";
  }

  // 组件加载时的初始逻辑
  const characterId = route.params.id;
  currentCharacter.value = allCharacters.value.find(c => c.id === characterId);
  if (currentCharacter.value) {
    messages.value.push({ id: Date.now(), text: `你好！我是${currentCharacter.value.name}。请点击下方的麦克风按钮对我说话。`, sender: 'ai' });
  } else {
    systemMessage.value = "错误：未找到角色信息。";
    console.error("未能根据路由参数找到角色:", characterId);
  }
});

// 组件销毁时清理副作用
onUnmounted(() => {
  if (recognition) {
    recognition.stop();
    recognition.onstart = null;
    recognition.onend = null;
    recognition.onresult = null;
    recognition.onerror = null;
  }
});

// STT 控制函数
const toggleRecording = () => {
  if (!recognition || isAIThinking.value) return;
  if (isRecording.value) {
    recognition.stop();
  } else {
    recognition.start();
  }
};

//AI 处理流程
const processUserMessage = async (text) => {
  if (!currentCharacter.value) {
    addMessage("抱歉，内部出现错误，角色信息丢失了。", "ai");
    return;
  }
  try {
    isAIThinking.value = true;
    systemMessage.value = `${currentCharacter.value.name} 正在思考...`;

    // 调用后端的 LLM 接口获取文本回复
    const response = await chatApi.getLLMReply({
      message: text,
      conversationId: conversationId.value, // 传递当前对话ID (第一次为 null)
      systemPrompt: currentCharacter.value.systemPrompt
    });

    // *检查业务是否成功 (code: 200) 和 success 字段
    if (response.data && (response.data.code === 200 || response.data.code === 0) && response.data.success === true) {
      const responseData = response.data.data;
      // 从 data.content 获取真正的回复文本
      const aiResponseText = responseData.content;

      // 更新对话ID，用于下一轮对话
      conversationId.value = responseData.conversationId;

      if (aiResponseText) {
        addMessage(aiResponseText, 'ai');

        // 暂时注释掉语音播放，只专注于文本
        // await speak(aiResponseText);
      } else {
        throw new Error("AI 未能生成有效的回复文本");
      }
    } else {
      // 如果业务失败，则抛出后端返回的错误信息
      throw new Error(response.data.message || "AI 响应错误");
    }

  } catch (error) {
    console.error("处理用户消息时出错:", error);
    const errorMsg = error.response?.data?.message || error.message || "抱歉，处理您的消息时遇到了点麻烦。";
    addMessage(errorMsg, "ai");
  } finally {
    isAIThinking.value = false;
    systemMessage.value = '点击麦克风开始对话';
  }
};

// TTS 核心函数
const speak = async (text) => {
  systemMessage.value = '正在生成语音...';
  try {
    const response = await chatApi.textToSpeech({ text: text });

    if (response.data && response.data.code === 0) {
      const audioUrl = response.data.data;
      if (!audioUrl) {
        throw new Error("后端未返回有效的音频URL");
      }
      systemMessage.value = '正在播放...';
      const audio = new Audio(audioUrl);
      audio.play();

      return new Promise((resolve, reject) => {
        audio.onended = () => {
          systemMessage.value = '播放完毕';
          resolve();
        };
        audio.onerror = () => {
          reject(new Error('音频播放失败'));
        }
      });
    } else {
      throw new Error(response.data.message || "语音生成失败");
    }
  } catch (error) {
    console.error("TTS 处理失败:", error);
    systemMessage.value = `语音生成失败`;
    throw error;
  }
};

// --- 辅助函数 ---
const addMessage = (text, sender) => {
  messages.value.push({ id: Date.now(), text, sender });
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
.chat-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #1e1e1e;
  overflow: hidden;
}
.chat-header {
  display: flex;
  align-items: center;
  padding: 0.75rem 1rem;
  background-color: #2f2f2f;
  flex-shrink: 0;
  border-bottom: 1px solid #444;
}
.back-button { background: none; border: none; color: white; font-size: 1.5rem; cursor: pointer; margin-right: 1rem; }
.avatar { width: 48px; height: 48px; border-radius: 50%; margin-right: 1rem; }
.name { font-size: 1.2rem; font-weight: bold; }
.message-list {
  flex-grow: 1;
  overflow-y: auto;
  padding: 1.5rem;
}
.chat-footer {
  padding: 1rem;
  background-color: #2f2f2f;
  flex-shrink: 0;
  border-top: 1px solid #444;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.system-message {
  color: #888;
  font-size: 0.9rem;
  height: 1.2rem;
  margin-bottom: 1rem;
  text-align: center;
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
  0% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.7); }
  70% { box-shadow: 0 0 0 25px rgba(59, 130, 246, 0); }
  100% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0); }
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


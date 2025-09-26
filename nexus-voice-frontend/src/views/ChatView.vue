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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'; // 1. 引入 onUnmounted
import { useRoute, useRouter } from 'vue-router';
import ChatMessage from '../components/ChatMessage.vue';

// --- Vue Router 和基础数据 ---
const route = useRoute();
const router = useRouter();
const allCharacters = ref([
  { id: 'harry_potter', name: '哈利·波特', avatar: 'placeholder.svg' },
  { id: 'socrates', name: '苏格拉底', avatar: 'placeholder.svg' },
  { id: 'eva_explorer', name: '宇航员伊娃', avatar: 'placeholder.svg' },
]);
const currentCharacter = ref(null);
const messages = ref([]);
const messageListEl = ref(null);

// --- 语音功能核心状态 ---
const isRecording = ref(false); //是否开启录音
const isAIThinking = ref(false);
const systemMessage = ref('点击麦克风开始对话');
let recognition; // 语音识别实例

// --- 1. 语音转文本 (STT) 初始化 ---
onMounted(() => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (SpeechRecognition) {
    recognition = new SpeechRecognition();
    recognition.continuous = false;
    recognition.lang = 'zh-CN';
    recognition.interimResults = false;
    // --- STT 事件监听 ---
    recognition.onstart = () => {
      isRecording.value = true;
      systemMessage.value = '正在倾听...';
    };
    recognition.onend = () => {
      isRecording.value = false;
      // 只有在 AI 没有思考时，才重置提示文本
      if (!isAIThinking.value) {
        systemMessage.value = '点击麦克风开始对话';
      }
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
      let errorMsg = '发生了一个错误';
      if (event.error === 'not-allowed') errorMsg = '需要麦克风授权';
      else if (event.error === 'no-speech') errorMsg = '未检测到语音';
      systemMessage.value = `错误: ${errorMsg}`;
      if (isRecording.value) {
        // 确保在出错时也停止录音状态
        isRecording.value = false;
      }
    };
  } else {
    systemMessage.value = "浏览器不支持语音识别功能";
  }
  // 组件加载时的初始逻辑
  const characterId = route.params.id;
  currentCharacter.value = allCharacters.value.find(c => c.id === characterId);
  if (currentCharacter.value) {
    messages.value.push({
      id: Date.now(),
      text: `你好！我是${currentCharacter.value.name}。请点击下方的麦克风按钮对我说话。`,
      sender: 'ai'
    });
  } else {
    // 如果找不到角色，给出提示
    systemMessage.value = "错误：未找到角色信息。";
    console.error("未能根据路由参数找到角色:", characterId);
  }
});

// 添加 onUnmounted 生命周期钩子来清理副作用
onUnmounted(() => {
  // 当组件被销毁时 (例如，返回上一页或页面刷新)
  // 确保停止任何正在进行的语音识别，并移除所有监听器
  // 这可以防止旧的监听器在组件重新加载后意外触发，从而导致状态错乱
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
  if (!recognition || isAIThinking.value) return; // AI思考时禁用
  if (isRecording.value) {
    recognition.stop();
  } else {
    recognition.start();
  }
};

// AI 处理流程 (未来将集成真实 LLM)
const processUserMessage = async (text) => {
  //  增加安全检查，防止 currentCharacter.value 为 undefined
  if (!currentCharacter.value) {
    console.error("processUserMessage 被调用，但 currentCharacter 未设置。");
    addMessage("抱歉，内部出现错误，角色信息丢失了。", "ai");
    return;
  }

  try {
    // 将状态设置移入 try 块内部
    isAIThinking.value = true;
    systemMessage.value = `${currentCharacter.value.name} 正在思考...`;

    //todo 在这里，会调用后端 Spring AI 接口获取文本回复
    // 现在先模拟一个回复
    const aiResponseText = `这是对“${text}”的AI模拟回复。现在将为您播放语音。`;
    addMessage(aiResponseText, 'ai');

    await speak(aiResponseText); // 调用 TTS 函数
  } catch (error) {
    console.error("处理用户消息时出错:", error);
    addMessage("抱歉，处理您的消息时遇到了点麻烦。", "ai");
  } finally {

    isAIThinking.value = false;
    systemMessage.value = '点击麦克风开始对话';
  }
};

// --- 2. 文本转语音 (TTS) 核心函数 ---
const speak = async (text) => {
  systemMessage.value = '正在生成语音...';

  // 与后端(七牛云)集成
  const BACKEND_TTS_URL = '/api/tts';

  try {
    const response = await fetch(BACKEND_TTS_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text: text })
    });

    if (!response.ok) {
      throw new Error(`后端 TTS 接口请求失败: ${response.status}`);
    }

    const result = await response.json();
    const audioData = result.audioData;

    if (!audioData) {
      throw new Error("后端未返回有效的音频数据");
    }

    // 前端音频处理
    systemMessage.value = '正在解码音频...';
    const pcmData = base64ToArrayBuffer(audioData);
    const pcm16 = new Int16Array(pcmData);
    const wavBlob = pcmToWav(pcm16, 16000); // 采样率需与七牛云设置一致
    const audioUrl = URL.createObjectURL(wavBlob);
    const audio = new Audio(audioUrl);
    audio.play();

    return new Promise(resolve => {
      audio.onended = () => {
        systemMessage.value = '播放完毕';
        resolve();
      };
    });
  } catch (error) {
    console.error("TTS 处理失败:", error);
    systemMessage.value = `语音生成失败`;
    // 即使语音失败，也要抛出错误，让上层知道
    throw error;
  }
};

// --- 3. 音频处理工具函数 ---
function base64ToArrayBuffer(base64) {
  const binaryString = window.atob(base64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes.buffer;
}
function pcmToWav(pcmData, sampleRate) {
  const numChannels = 1, bitsPerSample = 16;
  const blockAlign = (numChannels * bitsPerSample) / 8;
  const byteRate = sampleRate * blockAlign;
  const dataSize = pcmData.length * 2;
  const buffer = new ArrayBuffer(44 + dataSize);
  const view = new DataView(buffer);
  writeString(view, 0, 'RIFF');
  view.setUint32(4, 36 + dataSize, true);
  writeString(view, 8, 'WAVE');
  writeString(view, 12, 'fmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, numChannels, true);
  view.setUint32(24, sampleRate, true);
  view.setUint32(28, byteRate, true);
  view.setUint16(32, blockAlign, true);
  view.setUint16(34, bitsPerSample, true);
  writeString(view, 36, 'data');
  view.setUint32(40, dataSize, true);
  for (let i = 0; i < pcmData.length; i++) {
    view.setInt16(44 + i * 2, pcmData[i], true);
  }
  return new Blob([view], { type: 'audio/wav' });
}
function writeString(view, offset, string) {
  for (let i = 0; i < string.length; i++) {
    view.setUint8(offset + i, string.charCodeAt(i));
  }
}


// --- 辅助函数 --- 用于添加消息到页面
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
  mask: url("data:image/svg+xml,%3Csvg xmlns='http://www.w.org/2000/svg' viewBox='0 0 384 512'%3E%3Cpath d='M0 128C0 92.7 28.7 64 64 64H320c35.3 0 64 28.7 64 64V384c0 35.3-28.7 64-64 64H64c-35.3 0-64-28.7-64-64V128z'/%3E%3C/svg%3E") no-repeat center;
}
</style>


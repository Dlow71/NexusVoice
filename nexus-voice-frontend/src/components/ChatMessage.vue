<template>
  <!-- 消息容器，用于控制气泡在左侧还是右侧 -->
  <div class="message-wrapper" :class="wrapperClass">
    <!-- 真正的消息气泡 -->
    <div class="message-bubble" :class="bubbleClass">
      <p class="message-text">{{ message.text }}</p>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

// 定义组件接收的属性
const props = defineProps({
  message: {
    type: Object,
    required: true // 包含 { text: String, sender: 'user' | 'ai' }
  }
});

// 计算属性，根据发送者动态决定外层容器的 class
const wrapperClass = computed(() => {
  return props.message.sender === 'user' ? 'justify-end' : 'justify-start';
});

// 计算属性，根据发送者动态决定气泡本身的 class
const bubbleClass = computed(() => {
  return props.message.sender === 'user' ? 'user-bubble' : 'ai-bubble';
});
</script>

<style scoped>
/* 外层容器使用 flex 布局来控制对齐 */
.message-wrapper {
  display: flex;
  margin-bottom: 1rem;
}
.justify-end {
  justify-content: flex-end; /* user 消息靠右 */
}
.justify-start {
  justify-content: flex-start; /* ai 消息靠左 */
}

/* 消息气泡的通用样式 */
.message-bubble {
  max-width: 75%;
  padding: 0.75rem 1rem;
  border-radius: 18px;
  line-height: 1.6;
}

/* 消息内容的段落样式 */
.message-text {
  margin: 0;
  white-space: pre-wrap; /* 保留换行符 */
  word-wrap: break-word; /* 长单词自动换行 */
}

/* 用户气泡的特定样式 */
.user-bubble {
  background-color: #3b82f6; /* 蓝色 */
  color: white;
  border-bottom-right-radius: 4px; /* 右下角变为直角，更有对话感 */
}

/* AI 气泡的特定样式 */
.ai-bubble {
  background-color: #4b5563; /* 灰色 */
  color: white;
  border-bottom-left-radius: 4px; /* 左下角变为直角 */
}
</style>


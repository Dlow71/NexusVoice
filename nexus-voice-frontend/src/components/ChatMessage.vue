<template>
  <div
      class="chat-message-container"
      :class="message.sender === 'ai' ? 'ai' : 'user'"
  >
    <div class="message-bubble">
      <div class="content" v-html="renderedHtml"></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";

const props = defineProps({
  message: {
    type: Object,
    required: true,
  },
  // 接收父组件传来的、当前播放的片段索引
  playingIndex: {
    type: Number,
    default: null,
  },
});

// 重构渲染逻辑以支持分段高亮
const renderedHtml = computed(() => {
  const text = props.message.text || "";
  const segments = props.message.ttsSegments;

  if (
      !segments ||
      segments.length === 0 ||
      props.message.sender !== "ai"
  ) {
    // 如果没有分段信息，或消息不是来自AI，就按老方法渲染完整内容
    const rawHtml = marked.parse(text.replace(/\n/g, "<br>"));
    return DOMPurify.sanitize(rawHtml);
  }

  // 如果有分段信息，就构造带有高亮逻辑的HTML
  let htmlContent = "";
  segments.forEach((segment) => {
    // 检查当前片段的索引是否与正在播放的索引一致
    const isPlaying = props.playingIndex === segment.index;
    const segmentText = segment.text.replace(/\n/g, "<br>");
    // 为每个片段包裹一个span，并根据是否播放来添加高亮class
    htmlContent += `<span class="tts-segment ${
        isPlaying ? "is-playing" : ""
    }">${segmentText}</span>`;
  });

  // 依然使用marked处理整个拼接好的字符串，以支持内部的Markdown格式
  return DOMPurify.sanitize(marked.parse(htmlContent));
});
</script>

<style scoped>
.chat-message-container {
  display: flex;
  margin-bottom: 1rem;
  max-width: 85%;
  line-height: 1.6;
}
.chat-message-container.ai {
  justify-content: flex-start;
}
.chat-message-container.user {
  justify-content: flex-end;
  margin-left: auto;
}
.message-bubble {
  padding: 0.75rem 1.25rem;
  border-radius: 1.5rem;
  word-wrap: break-word;
  white-space: normal;
  text-align: left;
}
.ai .message-bubble {
  background-color: var(--bg-secondary);
  color: var(--text-primary);
  border-bottom-left-radius: 0.375rem;
}
.user .message-bubble {
  background-color: var(--primary-color);
  color: white;
  border-bottom-right-radius: 0.375rem;
}

/* 为分段文本和高亮效果添加样式 */
.content :deep(.tts-segment) {
  transition: background-color 0.3s ease, color 0.3s ease;
  border-radius: 4px;
}
.content :deep(.tts-segment.is-playing) {
  background-color: rgba(74, 144, 226, 0.3); /* 淡蓝色高亮背景 */
  color: var(--text-primary);
}

/* Markdown 内容样式 */
.content :deep(h1),
.content :deep(h2),
.content :deep(h3) {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
  line-height: 1.3;
  font-weight: 600;
}
.content :deep(p) {
  margin: 0.5em 0;
}
.content :deep(> *:first-child) {
  margin-top: 0;
}
.content :deep(> *:last-child) {
  margin-bottom: 0;
}
.content :deep(ul),
.content :deep(ol) {
  padding-left: 1.5em;
}
.content :deep(strong) {
  font-weight: 600;
}
</style>
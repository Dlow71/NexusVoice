<template>
  <div class="chat-message-container" :class="message.sender === 'ai' ? 'ai' : 'user'">
    <div class="message-bubble">
      <div class="content" v-html="renderedHtml"></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { marked } from 'marked';
import DOMPurify from 'dompurify';

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
});

const renderedHtml = computed(() => {
  if (props.message && props.message.text) {
    const rawHtml = marked.parse(props.message.text.replace(/\n/g, '<br>'));
    const sanitizedHtml = DOMPurify.sanitize(rawHtml);
    return sanitizedHtml;
  }
  return '';
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
}

.ai .message-bubble {
  background-color: var(--bg-secondary) ;
  color: var(--text-primary) ;
  border-bottom-left-radius: 0.375rem;
}

/* 用户消息气泡的样式 */
.user .message-bubble {
  background-color: var(--primary-color) ; /* 使用主题蓝色作为背景 */
  color: white ; /* 确保文字是白色，形成对比 */
  border-bottom-right-radius: 0.375rem;
}

/* 为 v-html 渲染出来的内容添加样式 */
.content :deep(h1),
.content :deep(h2),
.content :deep(h3) {
  margin-top: 0.5em;
  margin-bottom: 0.5em;
  line-height: 1.3;
  font-weight: 600;
}

.content :deep(h1) {
  font-size: 1.5em;
}

.content :deep(h2) {
  font-size: 1.3em;
}

.content :deep(h3) {
  font-size: 1.1em;
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

/* 确保用户消息中的加粗文字也是白色 */
.user .message-bubble .content :deep(strong) {
  color: white;
}


.content :deep(code) {
  background-color: rgba(0, 0, 0, 0.2);
  padding: 0.2em 0.4em;
  border-radius: 4px;
  font-family: 'Courier New', Courier, monospace;
}

.content :deep(table) {
  border-collapse: collapse;
  margin: 1em 0;
  width: 100%;
}

.content :deep(th),
.content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 0.5em 0.75em;
}

.content :deep(th) {
  background-color: rgba(255, 255, 255, 0.05);
}
</style>
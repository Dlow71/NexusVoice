<template>
  <div class="sidebar-container">
    <button @click="goHome" class="new-chat-btn">+ 发起新对话</button>
    <h3 class="history-title">最近对话</h3>
    <ul class="history-list">
      <li
          v-for="convo in history"
          :key="convo.id"
          class="history-item"
          :class="{ active: convo.id === activeId }"
          :title="convo.conversationRole ? convo.conversationRole.name : '未知角色'"
          @click="switchConversation(convo.id)"
      >
        <img
            :src="(convo.conversationRole && convo.conversationRole.avatarUrl) ? convo.conversationRole.avatarUrl : defaultAvatar"
            @error="onImageError"
            alt="avatar"
            class="avatar"
        />
        <div class="convo-info">
          <span class="convo-content">{{ convo.lastMessage || convo.title || '新对话' }}</span>
        </div>
        <button @click.stop="deleteConvo(convo.id)" class="delete-btn" title="删除对话">
          ×
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { useRouter } from "vue-router";
import defaultAvatar from "../assets/placeholder.svg";

defineProps({
  history: { type: Array, required: true },
  activeId: { type: String, default: null },
});

// 增加 delete-conversation 事件
const emit = defineEmits(["switch-conversation", "delete-conversation"]);
const router = useRouter();

const goHome = () => router.push("/");
const switchConversation = (id) => emit("switch-conversation", id);
const onImageError = (event) => {
  event.target.src = defaultAvatar;
};

// 触发删除事件的函数
const deleteConvo = (id) => {
  emit("delete-conversation", id);
};
</script>

<style scoped>
.sidebar-container {
  width: 260px;
  background-color: #1f2937;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  height: 100vh;
  border-right: 1px solid #374151;
}
.new-chat-btn {
  width: 100%;
  padding: 0.75rem;
  background-color: transparent;
  border: 1px solid #4b5563;
  color: #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  font-size: 1rem;
  transition: all 0.2s;
  margin-bottom: 1rem;
}
.new-chat-btn:hover {
  background-color: #374151;
  border-color: #6b7280;
}
.history-title {
  color: #9ca3af;
  font-size: 0.875rem;
  margin: 1rem 0 0.5rem;
  text-transform: uppercase;
  padding: 0 0.5rem;
}
.history-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
}
.history-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  padding: 0.75rem;
  border-radius: 6px;
  transition: background-color 0.2s;
  color: #d1d5db;
  position: relative;
}
.history-item:hover {
  background-color: #374151;
}
.history-item.active {
  background-color: #4a90e2;
  color: white;
}
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
}
.convo-info {
  flex-grow: 1;
  overflow: hidden;
  white-space: nowrap;
}
.convo-content {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.9rem;
  line-height: 1.4;
}
/* 删除按钮样式 */
.delete-btn {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.1);
  border: none;
  color: #9ca3af;
  font-size: 1rem;
  line-height: 18px;
  text-align: center;
  cursor: pointer;
  opacity: 0; /* 默认隐藏 */
  transition: opacity 0.2s, background-color 0.2s;
}
.history-item:hover .delete-btn {
  opacity: 1; /* 悬浮时显示 */
}
.delete-btn:hover {
  background-color: #ef4444; /* 悬浮时变红 */
  color: white;
}
</style>
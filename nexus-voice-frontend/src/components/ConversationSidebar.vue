<template>
  <div class="sidebar-container">
    <button @click="goHome" class="new-chat-btn">
      <el-icon class="btn-icon"><Plus /></el-icon>
      发起新对话
    </button>

    <!-- 搜索框 -->
    <div class="search-container">
      <el-input
        v-model="searchQuery"
        placeholder="搜索对话..."
        prefix-icon="Search"
        class="search-input"
        clearable
      />
    </div>

    <h3 class="history-title">最近对话</h3>
    <ul class="history-list">
      <li
          v-for="convo in filteredHistory"
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
          <span class="convo-content">{{ convo.title || convo.lastMessage || '新对话' }}</span>
        </div>
        <button @click.stop="deleteConvo(convo.id)" class="delete-btn" title="删除对话">
          <el-icon><Close /></el-icon>
        </button>
      </li>
    </ul>

    <!-- 底部设置 - 隐藏 -->
    <!-- <div class="sidebar-footer">
      <button class="settings-btn">
        <el-icon><Setting /></el-icon>
        设置
      </button>
    </div> -->
  </div>
</template>

<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import { Plus, Search, Close, Setting } from "@element-plus/icons-vue";
import { STAR_IMAGE_FALLBACK, replaceImageWithStarFallback } from "../utils/starFallback";

const props = defineProps({
  history: { type: Array, required: true },
  activeId: { type: String, default: null },
});

const searchQuery = ref("");

const filteredHistory = computed(() => {
  if (!searchQuery.value.trim()) return props.history;
  const keyword = searchQuery.value.toLowerCase();
  return props.history.filter(convo => {
    const title = convo.title || convo.lastMessage || '新对话';
    return title.toLowerCase().includes(keyword);
  });
});

// 增加 delete-conversation 事件
const emit = defineEmits(["switch-conversation", "delete-conversation"]);
const router = useRouter();

const goHome = () => router.push("/");
const switchConversation = (id) => emit("switch-conversation", id);
const defaultAvatar = STAR_IMAGE_FALLBACK;

const onImageError = (event) => {
  replaceImageWithStarFallback(event.target);
};

// 触发删除事件的函数
const deleteConvo = (id) => {
  emit("delete-conversation", id);
};
</script>

<style scoped>
.sidebar-container {
  width: 280px;
  background: var(--surface-elevated);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  padding: 1.25rem 1rem;
  display: flex;
  flex-direction: column;
  height: 100%; /* Changed to 100% since app covers vh */
  border-right: 1px solid var(--border-subtle);
  position: relative;
  z-index: 1;
}

.new-chat-btn {
  width: 100%;
  padding: 0.8rem 1rem;
  background: var(--primary-soft-bg);
  border: 1px solid var(--primary-soft-border);
  color: var(--primary-color);
  border-radius: 99px; /* Fully rounded as in image */
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  font-size: 0.95rem;
  font-weight: 500;
  transition: all 0.3s ease;
  margin-bottom: 1.25rem;
  flex-shrink: 0;
}

.new-chat-btn:hover {
  background: var(--primary-soft-bg-hover);
  border-color: var(--primary-color);
  box-shadow: var(--shadow-glow);
  transform: translateY(-1px);
}

.search-container {
  margin-bottom: 1rem;
  flex-shrink: 0;
}

.search-input :deep(.el-input__wrapper) {
  background-color: var(--surface-glass) !important;
  border: 1px solid var(--border-default) !important;
  box-shadow: none !important;
  border-radius: 12px !important;
  padding: 4px 12px;
}

.search-input :deep(.el-input__inner) {
  color: var(--text-primary) !important;
}

.search-input :deep(.el-input__inner::placeholder) {
  color: var(--text-subtle) !important;
}

.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: var(--primary-color) !important;
  background-color: var(--surface-overlay) !important;
}

.history-title {
  color: var(--text-dim);
  font-size: 0.8rem;
  font-weight: 500;
  margin: 0.5rem 0;
  padding: 0 0.5rem;
  flex-shrink: 0;
}

.history-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex: 1;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  cursor: pointer;
  padding: 0.7rem 0.75rem;
  border-radius: 10px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  color: var(--text-subtle);
  position: relative;
  margin-bottom: 2px;
  border: 1px solid transparent;
}

.history-item:hover {
  background: var(--surface-glass-hover);
  border-color: var(--border-subtle);
  color: var(--text-default);
}

.history-item.active {
  background: var(--primary-soft-bg);
  border-color: transparent;
  color: var(--primary-color);
}

.history-item.active .convo-content {
  font-weight: 600;
}

.avatar {
  width: 38px;
  height: 38px;
  display: block;
  border-radius: 10px;
  object-fit: cover;
  flex-shrink: 0;
  border: 1px solid var(--border-glass);
  background: #e0f2fe;
  transition: border-color 0.25s;
}

.history-item.active .avatar {
  border-color: var(--switch-track-active-border);
}

.convo-info {
  flex-grow: 1;
  overflow: hidden;
  white-space: nowrap;
}

.convo-content {
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 0.875rem;
  line-height: 1.4;
  font-weight: 450;
}

.delete-btn {
  position: absolute;
  right: 0.5rem;
  top: 50%;
  transform: translateY(-50%);
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: transparent;
  border: none;
  color: var(--text-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: var(--error-soft-bg);
  color: var(--error-color);
}

.sidebar-footer {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-subtle);
  flex-shrink: 0;
}

.settings-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  background: transparent;
  border: none;
  color: var(--text-subtle);
  padding: 0.75rem;
  border-radius: 10px;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.settings-btn:hover {
  background: var(--surface-glass-hover);
  color: var(--text-primary);
}
</style>

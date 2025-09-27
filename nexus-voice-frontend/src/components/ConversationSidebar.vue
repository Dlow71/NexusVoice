<template>
  <!-- 侧边栏容器，根据 isCollapsed 状态动态添加 'collapsed' class -->
  <aside class="sidebar" :class="{ collapsed: isCollapsed }">
    <!-- 顶部区域，包含logo和折叠按钮 -->
    <div class="sidebar-header">
      <h1 v-if="!isCollapsed" class="sidebar-title">Nexus Voice</h1>
      <button @click="toggleCollapse" class="collapse-btn">
        <!-- 根据状态显示不同图标 -->
        <span v-if="!isCollapsed">‹</span>
        <span v-else>›</span>
      </button>
    </div>

    <!-- 发起新对话按钮 -->
    <div class="new-chat-section">
      <button @click="startNew" class="new-chat-btn">
        <span>+</span>
        <span v-if="!isCollapsed">发起新对话</span>
      </button>
    </div>

    <!-- 历史会话列表 -->
    <div class="history-list-container">
      <p v-if="!isCollapsed" class="history-title">最近对话</p>
      <div
          v-for="session in history"
          :key="session.id"
          class="history-item"
          :class="{ active: session.id === activeId }"
          @click="selectSession(session.id)"
          :title="session.title || '新的对话'"
      >
        <span>•</span>
        <span v-if="!isCollapsed">{{ session.title || '新的对话' }}</span>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { ref ,defineProps} from 'vue';
    defineProps({
      history: { // 接收父组件传递的会话历史数组
        type: Array,
        required: true,
      },
      activeId: { // 接收当前激活的会话ID，用于高亮显示
        type: [String, Number, null],
        default: null,
      }
    });

// 组件事件
const emit = defineEmits(['switch-conversation', 'new-conversation']);

// 组件内部状态
const isCollapsed = ref(false); // 控制侧边栏的折叠状态

// 组件方法

// 切换折叠状态
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value;
};

// 当用户点击“发起新对话”时，向父组件发送事件
const startNew = () => {
  emit('new-conversation');
};

// 当用户点击某个历史会话时，向父组件发送事件并传递会话ID
const selectSession = (id) => {
  emit('switch-conversation', id);
};
</script>

<style scoped>
/* 侧边栏基础样式 */
.sidebar {
  width: 260px;
  background-color: #1e1e1e;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #374151;
  padding: 1rem;
  transition: width 0.3s ease; /* 平滑的宽度变化动画 */
}
/* 折叠后的样式 */
.sidebar.collapsed {
  width: 68px; /* 仅显示图标的宽度 */
  padding: 1rem 0.5rem;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  padding: 0 0.5rem;
}
.sidebar-title {
  color: white;
  font-size: 1.5rem;
  font-weight: bold;
  white-space: nowrap; /* 防止文字换行 */
}
.collapse-btn {
  background: #374151;
  color: #d1d5db;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  cursor: pointer;
  font-size: 1.5rem;
  display: flex;
  justify-content: center;
  align-items: center;
}

.new-chat-section {
  padding: 0 0.5rem;
}
.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  background-color: transparent;
  border: 1px solid #4b5563;
  color: #d1d5db;
  border-radius: 8px;
  text-align: left;
  font-size: 1rem;
  cursor: pointer;
}
.sidebar.collapsed .new-chat-btn {
  justify-content: center;
}

.history-list-container {
  flex-grow: 1;
  overflow-y: auto;
  margin-top: 1.5rem;
}
.history-title {
  color: #9ca3af;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
  padding: 0 0.5rem;
}
.history-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: #d1d5db;
  padding: 0.75rem 0.5rem;
  border-radius: 6px;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.history-item:hover {
  background-color: #374151;
}
.history-item.active {
  background-color: #3b82f6;
  color: white;
}
.sidebar.collapsed .history-item {
  justify-content: center;
}
</style>

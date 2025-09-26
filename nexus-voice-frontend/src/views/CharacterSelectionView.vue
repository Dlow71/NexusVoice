<template>
  <div class="selection-container">
    <!-- 用户信息及登出按钮 -->
    <div v-if="user" class="user-info-bar">
      <span class="welcome-text">欢迎, {{ user.nickname || '用户' }}</span>
      <button @click="handleLogout" class="logout-button">退出登录</button>
    </div>

    <!-- 页面的头部 -->
    <header class="header">
      <h1 class="title">Nexus Voice</h1>
      <p class="subtitle">选择一位角色，开启对话</p>
      <!-- "创建新角色" 按钮 -->
      <button class="create-character-btn">+ 创建新角色</button>
    </header>

    <!-- 主内容区域 -->
    <main class="card-grid">
      <!-- 加载状态 -->
      <div v-if="isLoading" class="loading-state">
        正在加载角色...
      </div>
      <!-- 错误状态 -->
      <div v-else-if="error" class="error-state">
        {{ error }}
      </div>
      <!-- 成功获取数据后，渲染角色卡片 -->
      <CharacterCard
          v-else
          v-for="character in characters"
          :key="character.id"
          :character="character"
      />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '../stores/auth';
import CharacterCard from '../components/CharacterCard.vue';
// 1. 导入创建的 character.js API 服务
import characterApi from '../services/character.js';

const authStore = useAuthStore();
const user = computed(() => authStore.user);

// 2. 定义新的状态来管理数据、加载和错误
const characters = ref([]);
const isLoading = ref(true);
const error = ref(null);

// 3. 在 onMounted 生命周期钩子中调用 API 获取数据
onMounted(async () => {
  try {
    // 调用 API 获取公共角色列表 (假设第一页，每页10个)
    const response = await characterApi.getPublicCharacters({ page: 1, size: 10 });
    // 假设后端返回的数据在 response.data.data.records 中
    characters.value = response.data.data.records;
  } catch (err) {
    console.error("获取角色列表失败:", err);
    error.value = '无法加载角色列表，请稍后再试。';
  } finally {
    isLoading.value = false;
  }
});

// 定义登出处理函数
const handleLogout = () => {
  authStore.logout();
};
</script>

<style scoped>
/* 页面主容器的样式 */
.selection-container {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 2rem;
  box-sizing: border-box;
}

/* 用户信息栏样式 */
.user-info-bar {
  position: absolute;
  top: 2rem;
  right: 2rem;
  display: flex;
  align-items: center;
  gap: 1rem;
  background-color: rgba(47, 47, 47, 0.5);
  padding: 0.5rem 1rem;
  border-radius: 8px;
  backdrop-filter: blur(5px);
}
.welcome-text {
  color: #d1d5db;
  font-weight: 500;
}
.logout-button {
  background-color: transparent;
  border: 1px solid #4b5563;
  color: #d1d5db;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}
.logout-button:hover {
  background-color: #374151;
  border-color: #6b7280;
}

/* 头部区域的样式 */
.header {
  text-align: center;
  margin-bottom: 3rem;
}
.title {
  font-size: 3.2rem;
  font-weight: bold;
}
.subtitle {
  font-size: 1.2rem;
  color: #888;
  margin-top: 0.5rem;
}

/* 创建新角色 按钮样式 */
.create-character-btn {
  margin-top: 1.5rem;
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}
.create-character-btn:hover {
  background-color: #2563eb;
}

/* 角色卡片网格的样式 */
.card-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem;
  width: 100%;
  max-width: 1200px;
}

/* 加载和错误状态的样式 */
.loading-state, .error-state {
  grid-column: 1 / -1; /* 让这个元素横跨整个网格 */
  text-align: center;
  padding: 3rem;
  color: #9ca3af;
  font-size: 1.1rem;
}
.error-state {
  color: #f87171;
}


/* 响应式布局 */
@media (max-width: 1024px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
  .title {
    font-size: 2.5rem;
  }
  .user-info-bar {
    position: static;
    margin-bottom: 2rem;
    order: -1;
  }
}
</style>


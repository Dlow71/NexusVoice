<template>
  <div class="auth-container">
    <div class="auth-box">
      <h1 class="title">Nexus Voice</h1>
      <h2 class="subtitle">{{ isLoginMode ? '欢迎回来' : '创建新账户' }}</h2>

      <form @submit.prevent="handleSubmit" class="form">
        <!-- 注册时显示的额外字段 -->
        <template v-if="!isLoginMode">
          <div class="input-group">
            <label for="nickname">昵称</label>
            <input id="nickname" v-model="form.nickname" type="text" placeholder="请输入您的昵称" required>
          </div>
        </template>

        <!-- 公共字段 -->
        <div class="input-group">
          <label for="email">邮箱</label>
          <input id="email" v-model="form.email" type="email" placeholder="请输入您的邮箱" required>
        </div>
        <div class="input-group">
          <label for="password">密码</label>
          <input id="password" v-model="form.password" type="password" placeholder="请输入您的密码" required>
        </div>

        <!-- 仅在注册模式下显示 -->
        <template v-if="!isLoginMode">
          <div class="input-group">
            <label for="confirmPassword">确认密码</label>
            <input id="confirmPassword" v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" required>
          </div>
          <div class="input-group">
            <label for="phone">手机号</label>
            <input id="phone" v-model="form.phone" type="tel" placeholder="请输入您的手机号" required>
          </div>
        </template>

        <!-- 仅在登录模式下显示“记住我” -->
        <div v-if="isLoginMode" class="remember-me-group">
          <input type="checkbox" id="rememberMe" v-model="form.rememberMe">
          <label for="rememberMe">记住我</label>
        </div>

        <!-- 用于展示API或校验的错误信息 -->
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

        <button type="submit" class="submit-button" :disabled="isLoading">
          {{ isLoading ? '处理中...' : (isLoginMode ? '登录' : '注册') }}
        </button>
      </form>

      <p class="toggle-mode">
        {{ isLoginMode ? '还没有账户？' : '已有账户？' }}
        <a @click.prevent="toggleMode" class="toggle-link">
          {{ isLoginMode ? '立即注册' : '立即登录' }}
        </a>
      </p>

      <!-- 第三方登录分隔线 -->
      <div class="divider">
        <span>或使用以下方式登录</span>
      </div>

      <!-- GitHub登录按钮 -->
      <button @click="handleGitHubLogin" type="button" class="github-login-button">
        <svg class="github-icon" viewBox="0 0 16 16" width="20" height="20">
          <path fill="currentColor" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
        </svg>
        使用 GitHub 登录
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../stores/auth';

// --- 组件状态定义 ---

// 控制UI在登录和注册模式间切换
const isLoginMode = ref(true);
// 控制提交按钮的加载状态，防止重复提交
const isLoading = ref(false);
// 存储并显示错误信息
const errorMessage = ref('');
// 响应式对象，绑定表单所有输入框的数据
const form = ref({
  email: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  rememberMe: false // 新增 rememberMe 字段
});

// 获取 Pinia store 实例
const authStore = useAuthStore();

// --- 组件方法定义 ---

// 切换登录/注册模式
const toggleMode = () => {
  isLoginMode.value = !isLoginMode.value;
  errorMessage.value = ''; // 切换时清空错误
  // 重置部分表单字段
  form.value.confirmPassword = '';
  form.value.nickname = '';
  form.value.phone = '';
  form.value.rememberMe = false;
};

// 表单提交处理
const handleSubmit = async () => {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    if (isLoginMode.value) {
      // 执行登录操作，调用 store 的 action
      await authStore.login({
        email: form.value.email,
        password: form.value.password,
        rememberMe: form.value.rememberMe // 传递 rememberMe 状态
      });
    } else {
      // --- 客户端校验 ---
      if (form.value.password !== form.value.confirmPassword) {
        throw new Error('两次输入的密码不一致');
      }
      const phoneRegex = /^1[3-9]\d{9}$/;
      if (!phoneRegex.test(form.value.phone)) {
        throw new Error('请输入有效的11位手机号码');
      }

      // 执行注册操作，调用 store 的 action
      await authStore.register({
        email: form.value.email,
        password: form.value.password,
        confirmPassword: form.value.confirmPassword,
        nickname: form.value.nickname,
        phone: form.value.phone,
      });
    }
    // 成功后，路由跳转由 store 的 action 负责
  } catch (error) {
    // 捕获从 store action 抛出的错误，并显示
    errorMessage.value = error.message || '操作失败，请稍后重试';
  } finally {
    // 无论成功或失败，最后都结束加载状态
    isLoading.value = false;
  }
};

// GitHub登录处理
const handleGitHubLogin = () => {
  // 跳转到后端的OAuth2授权端点
  window.location.href = 'http://localhost:8081/oauth2/authorization/github';
};
</script>

<style scoped>
.auth-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #111827;
}
.auth-box {
  background-color: #1f2937;
  padding: 2.5rem;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  width: 100%;
  max-width: 420px;
  text-align: center;
  color: #d1d5db;
}
.title {
  font-size: 2rem;
  font-weight: bold;
  color: #fff;
}
.subtitle {
  margin-top: 0.5rem;
  margin-bottom: 2rem;
  font-size: 1.1rem;
}
.form {
  display: flex;
  flex-direction: column;
}
.input-group {
  margin-bottom: 1rem;
  text-align: left;
}
.input-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-size: 0.9rem;
  color: #9ca3af;
}
.input-group input {
  width: 100%;
  box-sizing: border-box;
  background-color: #374151;
  border: 1px solid #4b5563;
  color: #d1d5db;
  border-radius: 8px;
  padding: 0.75rem 1rem;
  font-size: 1rem;
}
.remember-me-group {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 1rem;
}
.remember-me-group label {
  margin-left: 0.5rem;
  color: #9ca3af;
  font-size: 0.9rem;
}
.error-message {
  color: #f87171;
  text-align: center;
  margin: 0.5rem 0;
}
.submit-button {
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.75rem;
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  margin-top: 1rem;
  transition: background-color 0.2s;
}
.submit-button:disabled {
  background-color: #6b7280;
  cursor: not-allowed;
}
.toggle-mode {
  margin-top: 1rem;
  color: #9ca3af;
  font-size: 0.9rem;
}
.toggle-link {
  color: #3b82f6;
  text-decoration: none;
  cursor: pointer;
}
.toggle-link:hover {
  text-decoration: underline;
}

/* 第三方登录分隔线样式 */
.divider {
  margin: 1.5rem 0;
  text-align: center;
  position: relative;
}
.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 42%;
  height: 1px;
  background-color: #4b5563;
}
.divider::before {
  left: 0;
}
.divider::after {
  right: 0;
}
.divider span {
  background-color: #1f2937;
  padding: 0 1rem;
  color: #9ca3af;
  font-size: 0.85rem;
  position: relative;
}

/* GitHub登录按钮样式 */
.github-login-button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  background-color: #24292e;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.75rem;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.2s;
}
.github-login-button:hover {
  background-color: #1a1e22;
}
.github-icon {
  flex-shrink: 0;
}
</style>

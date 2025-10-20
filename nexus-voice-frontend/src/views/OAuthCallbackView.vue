<template>
  <div class="callback-container">
    <div v-if="isLoading" class="loading">
      <div class="spinner"></div>
      <p>正在登录...</p>
    </div>
    <div v-else-if="error" class="error">
      <svg class="error-icon" width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
      <h2>登录失败</h2>
      <p>{{ errorMessage }}</p>
      <button @click="backToLogin" class="back-button">返回登录页</button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import {
  validateJWTFormat,
  sanitizeString,
  sanitizeURL,
  validateEmail,
  validateDateTime,
  validateUserId
} from '../utils/securityUtils';

const router = useRouter();
const authStore = useAuthStore();

// 组件状态
const isLoading = ref(true);
const error = ref(false);
const errorMessage = ref('');

onMounted(async () => {
  try {
    // 从URL参数中提取token和错误信息
    const urlParams = new URLSearchParams(window.location.search);
    
    // 安全提取和验证参数
    const accessToken = urlParams.get('access_token');
    const refreshToken = urlParams.get('refresh_token');
    const expiresAt = urlParams.get('expires_at');
    const errorCode = sanitizeString(urlParams.get('error'), 50);
    const errorMsg = sanitizeString(urlParams.get('message'), 200);
    
    // 用户信息参数（可选）
    const userId = urlParams.get('user_id');
    const email = urlParams.get('email');
    const nickname = urlParams.get('nickname');
    const avatarUrl = urlParams.get('avatar_url');
    
    // 检查是否有错误
    if (errorCode || errorMsg) {
      // OAuth登录失败
      isLoading.value = false;
      error.value = true;
      errorMessage.value = errorMsg || 'OAuth登录失败，请重试';
      console.error('OAuth登录失败:', errorCode, errorMsg);
      return;
    }
    
    // 验证JWT Token格式
    if (!validateJWTFormat(accessToken)) {
      throw new Error('访问令牌格式无效');
    }
    
    if (!validateJWTFormat(refreshToken)) {
      throw new Error('刷新令牌格式无效');
    }
    
    // 验证和清理所有参数
    const validatedUserId = validateUserId(userId);
    const validatedEmail = validateEmail(email);
    const validatedNickname = sanitizeString(nickname, 50);
    const validatedAvatarUrl = sanitizeURL(avatarUrl);
    const validatedExpiresAt = validateDateTime(expiresAt);
    
    // 构建认证响应数据
    const authData = {
      accessToken,
      refreshToken,
      expiresAt: validatedExpiresAt,
      userInfo: {
        id: validatedUserId,
        email: validatedEmail,
        nickname: validatedNickname,
        avatarUrl: validatedAvatarUrl
      }
    };
    
    // 保存认证数据到store
    authStore.setAuthData(authData);
    
    console.log('OAuth登录成功');
    
    // 延迟一下让用户看到成功状态，然后跳转到首页
    setTimeout(() => {
      router.push('/');
    }, 1000);
    
  } catch (err) {
    console.error('处理OAuth回调数据失败:', err);
    isLoading.value = false;
    error.value = true;
    errorMessage.value = '登录参数验证失败，请重新登录';
  }
});

// 返回登录页
const backToLogin = () => {
  router.push('/login');
};
</script>

<style scoped>
.callback-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background-color: #111827;
}

.loading,
.error {
  text-align: center;
  color: #d1d5db;
}

.spinner {
  border: 4px solid #374151;
  border-top: 4px solid #3b82f6;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
  margin: 0 auto 1.5rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading p {
  font-size: 1.1rem;
  color: #9ca3af;
}

.error {
  background-color: #1f2937;
  padding: 2.5rem;
  border-radius: 12px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  max-width: 400px;
}

.error-icon {
  color: #ef4444;
  margin: 0 auto 1rem;
  display: block;
}

.error h2 {
  font-size: 1.5rem;
  color: #fff;
  margin-bottom: 0.5rem;
}

.error p {
  color: #9ca3af;
  margin-bottom: 1.5rem;
  line-height: 1.5;
}

.back-button {
  background-color: #3b82f6;
  color: white;
  border: none;
  border-radius: 8px;
  padding: 0.75rem 1.5rem;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.2s;
}

.back-button:hover {
  background-color: #2563eb;
}
</style>

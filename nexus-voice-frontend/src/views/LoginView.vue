<template>
  <div class="auth-container">
    <!-- 背景装饰层 -->
    <div class="background-decorations">
      <div class="glow-orb glow-orb-1"></div>
      <div class="glow-orb glow-orb-2"></div>
      <div class="glow-orb glow-orb-3"></div>
      <div class="grid-pattern"></div>
    </div>

    <!-- 视频入口按钮 - 已隐藏 -->
    <!-- <button @click="goToRandomVideo" class="video-entrance-btn">
      <span class="video-icon">🎬</span>
      <span>随机视频</span>
    </button> -->

    <!-- 主卡片 -->
    <div class="card-wrapper">
      <div class="card-glow-border"></div>
      <div class="auth-card">
        <!-- Logo 区域 -->
        <div class="logo-section">
          <SparklesBadge class="logo-icon-badge" :hoverable="true" :size="52" :icon-size="30" />
          <h1 class="brand-name">Nexus Voice</h1>
          <p class="brand-subtitle">{{ isLoginMode ? '欢迎回来，进驻您的 AI 之旅' : '创建账号，开启您的 AI 之旅' }}</p>
        </div>

        <!-- 登录/注册切换 Pill -->
        <div class="mode-toggle-pill">
          <button 
            :class="['mode-btn', { active: isLoginMode }]" 
            @click="switchToLogin"
          >
            登录
          </button>
          <button 
            :class="['mode-btn', { active: !isLoginMode }]" 
            @click="switchToRegister"
          >
            注册
          </button>
        </div>

        <!-- 表单 -->
        <form @submit.prevent="handleSubmit" class="auth-form">
          <!-- 注册模式：用户名字段 -->
          <div v-if="!isLoginMode" class="field-wrapper" :class="{ 'field-enter': !isLoginMode }">
            <label class="field-label">用户名</label>
            <div class="input-wrapper" :class="{ focused: focusedField === 'nickname' }">
              <svg class="input-icon" :class="{ focused: focusedField === 'nickname' }" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                <circle cx="12" cy="7" r="4"></circle>
              </svg>
              <input 
                v-model="form.nickname" 
                type="text" 
                placeholder="请输入您的昵称" 
                @focus="focusedField = 'nickname'"
                @blur="focusedField = ''"
                :required="!isLoginMode"
              />
            </div>
          </div>

          <!-- 邮箱字段 -->
          <div class="field-wrapper field-enter">
            <label class="field-label">邮箱</label>
            <div class="input-wrapper" :class="{ focused: focusedField === 'email' }">
              <svg class="input-icon" :class="{ focused: focusedField === 'email' }" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="5" width="18" height="14" rx="2"></rect>
                <path d="m3 7 9 6 9-6"></path>
              </svg>
              <input 
                v-model="form.email" 
                type="email" 
                placeholder="请输入您的邮箱" 
                @focus="focusedField = 'email'"
                @blur="focusedField = ''"
                required
              />
            </div>
          </div>

          <!-- 密码字段 -->
          <div class="field-wrapper field-enter">
            <label class="field-label">密码</label>
            <div class="input-wrapper" :class="{ focused: focusedField === 'password' }">
              <svg class="input-icon" :class="{ focused: focusedField === 'password' }" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="5" y="11" width="14" height="10" rx="2"></rect>
                <path d="M12 17a1 1 0 1 0 0-2 1 1 0 0 0 0 2z"></path>
                <path d="M8 11V7a4 4 0 0 1 8 0v4"></path>
              </svg>
              <input 
                v-model="form.password" 
                :type="showPassword ? 'text' : 'password'" 
                placeholder="请输入您的密码" 
                @focus="focusedField = 'password'"
                @blur="focusedField = ''"
                required
              />
              <button type="button" class="toggle-password" @click="showPassword = !showPassword">
                <svg v-if="!showPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </button>
            </div>
          </div>

          <!-- 注册模式：确认密码字段 -->
          <div v-if="!isLoginMode" class="field-wrapper" :class="{ 'field-enter': !isLoginMode }">
            <label class="field-label">确认密码</label>
            <div class="input-wrapper" :class="{ focused: focusedField === 'confirmPassword' }">
              <svg class="input-icon" :class="{ focused: focusedField === 'confirmPassword' }" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="5" y="11" width="14" height="10" rx="2"></rect>
                <path d="M12 17a1 1 0 1 0 0-2 1 1 0 0 0 0 2z"></path>
                <path d="M8 11V7a4 4 0 0 1 8 0v4"></path>
              </svg>
              <input 
                v-model="form.confirmPassword" 
                :type="showConfirmPassword ? 'text' : 'password'" 
                placeholder="请再次输入密码" 
                @focus="focusedField = 'confirmPassword'"
                @blur="focusedField = ''"
                :required="!isLoginMode"
              />
              <button type="button" class="toggle-password" @click="showConfirmPassword = !showConfirmPassword">
                <svg v-if="!showConfirmPassword" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                  <circle cx="12" cy="12" r="3"></circle>
                </svg>
                <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                  <line x1="1" y1="1" x2="23" y2="23"></line>
                </svg>
              </button>
            </div>
          </div>

          <!-- 登录模式：记住我 + 忘记密码 -->
          <div v-if="isLoginMode" class="remember-forgot-row">
            <label class="checkbox-label">
              <input type="checkbox" v-model="form.rememberMe" />
              <span>记住我</span>
            </label>
            <a href="#" class="forgot-link">忘记密码?</a>
          </div>

          <!-- 错误信息 -->
          <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

          <!-- 提交按钮 -->
          <button type="submit" class="submit-btn" :disabled="isLoading">
            <span v-if="!isLoading">{{ isLoginMode ? '登录' : '创建账号' }}</span>
            <span v-else class="loading-content">
              <span class="spinner"></span>
              处理中...
            </span>
            <svg class="arrow-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14M12 5l7 7-7 7"></path>
            </svg>
          </button>
        </form>

        <!-- 分割线 -->
        <div class="divider">
          <span>或使用以下方式{{ isLoginMode ? '登录' : '注册' }}</span>
        </div>

        <!-- GitHub OAuth -->
        <button @click="handleGitHubLogin" type="button" class="github-btn">
          <svg class="github-icon" viewBox="0 0 16 16" width="18" height="18">
            <path fill="currentColor" d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z"/>
          </svg>
          使用 GitHub {{ isLoginMode ? '登录' : '注册' }}
        </button>

        <!-- 底部切换链接 -->
        <p class="toggle-text">
          {{ isLoginMode ? '还没有账号?' : '已有账号?' }}
          <a @click.prevent="toggleMode" class="toggle-link">
            {{ isLoginMode ? '立即注册' : '立即登录' }}
          </a>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../stores/auth';
import SparklesBadge from '../components/SparklesBadge.vue';

const isLoginMode = ref(true);
const isLoading = ref(false);
const errorMessage = ref('');
const focusedField = ref('');
const showPassword = ref(false);
const showConfirmPassword = ref(false);

const form = ref({
  email: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  rememberMe: false
});

const authStore = useAuthStore();

const switchToLogin = () => {
  if (!isLoginMode.value) {
    isLoginMode.value = true;
    errorMessage.value = '';
    form.value.confirmPassword = '';
    form.value.nickname = '';
    form.value.phone = '';
  }
};

const switchToRegister = () => {
  if (isLoginMode.value) {
    isLoginMode.value = false;
    errorMessage.value = '';
    form.value.rememberMe = false;
  }
};

const toggleMode = () => {
  isLoginMode.value = !isLoginMode.value;
  errorMessage.value = '';
  form.value.confirmPassword = '';
  form.value.nickname = '';
  form.value.phone = '';
  form.value.rememberMe = false;
};

const handleSubmit = async () => {
  isLoading.value = true;
  errorMessage.value = '';
  try {
    if (isLoginMode.value) {
      await authStore.login({
        email: form.value.email,
        password: form.value.password,
        rememberMe: form.value.rememberMe
      });
    } else {
      if (form.value.password !== form.value.confirmPassword) {
        throw new Error('两次输入的密码不一致');
      }
      const phoneRegex = /^1[3-9]\d{9}$/;
      if (form.value.phone && !phoneRegex.test(form.value.phone)) {
        throw new Error('请输入有效的11位手机号码');
      }

      await authStore.register({
        email: form.value.email,
        password: form.value.password,
        confirmPassword: form.value.confirmPassword,
        nickname: form.value.nickname,
        phone: form.value.phone,
      });
    }
  } catch (error) {
    errorMessage.value = error.message || '操作失败，请稍后重试';
  } finally {
    isLoading.value = false;
  }
};

const handleGitHubLogin = () => {
  window.location.href = 'http://localhost:8081/oauth2/authorization/github?client=user';
};

const goToRandomVideo = () => {
  window.open('/random-video', '_blank');
};
</script>

<style scoped>
/* ===== CSS Variables - Ocean Blue Theme ===== */
:root {
  --background: oklch(0.985 0.003 200);
  --foreground: oklch(0.2 0.02 220);
  --card: oklch(1 0 0);
  --card-foreground: oklch(0.2 0.02 220);
  --primary: #0891b2;
  --primary-foreground: oklch(0.99 0 0);
  --secondary: oklch(0.95 0.01 220);
  --muted: oklch(0.97 0.003 200);
  --muted-foreground: oklch(0.5 0.02 220);
  --accent: oklch(0.7 0.15 195);
  --border: oklch(0.93 0.005 200);
  --input: oklch(0.975 0.003 200);
  --radius: 0.75rem;
}

/* ===== 容器 ===== */
.auth-container {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--background);
  overflow: hidden;
}

/* ===== 背景装饰 ===== */
.background-decorations {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
}

.glow-orb-1 {
  width: 500px;
  height: 500px;
  top: -150px;
  right: -100px;
  background: radial-gradient(circle, var(--primary) 0%, transparent 70%);
  opacity: 0.15;
  animation: float 8s ease-in-out infinite;
}

.glow-orb-2 {
  width: 400px;
  height: 400px;
  bottom: -100px;
  left: -80px;
  background: radial-gradient(circle, var(--accent) 0%, transparent 70%);
  opacity: 0.12;
  animation: float 10s ease-in-out infinite 2s;
}

.glow-orb-3 {
  width: 350px;
  height: 350px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, var(--primary) 0%, transparent 70%);
  opacity: 0.08;
  animation: float 12s ease-in-out infinite 4s;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image: 
    linear-gradient(oklch(0.2 0.02 220) 1px, transparent 1px),
    linear-gradient(90deg, oklch(0.2 0.02 220) 1px, transparent 1px);
  background-size: 48px 48px;
  opacity: 0.03;
}

/* ===== 视频入口按钮 ===== */
.video-entrance-btn {
  position: fixed;
  top: 1.5rem;
  right: 1.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background: linear-gradient(135deg, #0891b2, #06b6d4);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(6, 182, 212, 0.5);
  border-radius: 0.75rem;
  padding: 0.75rem 1.25rem;
  color: white;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(8, 145, 178, 0.3);
}

.video-entrance-btn:hover {
  background: linear-gradient(135deg, #0e7490, #0891b2);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(8, 145, 178, 0.5);
}

.video-icon {
  font-size: 1.125rem;
}

/* ===== 卡片包装器 ===== */
.card-wrapper {
  position: relative;
  width: 100%;
  max-width: 28rem;
  z-index: 1;
  animation: fadeSlideUp 0.5s ease-out both;
}

@keyframes fadeSlideUp {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 发光边框 ===== */
.card-glow-border {
  position: absolute;
  inset: -1px;
  border-radius: 1rem;
  opacity: 0.6;
  background: linear-gradient(
    135deg,
    oklch(0.6 0.18 220 / 0.4),
    oklch(0.7 0.15 195 / 0.2),
    oklch(0.6 0.18 220 / 0.4)
  );
  pointer-events: none;
}

/* ===== 主卡片 ===== */
.auth-card {
  position: relative;
  background: oklch(1 0 0 / 0.9);
  backdrop-filter: blur(24px);
  border: 1px solid oklch(0.92 0.01 220 / 0.6);
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 25px 50px oklch(0.6 0.18 220 / 0.05);
  overflow: visible;
}

/* ===== Logo 区域 ===== */
.logo-section {
  text-align: center;
  margin-bottom: 2rem;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.logo-icon-badge {
  margin-bottom: 0.75rem;
}

.brand-name {
  font-size: 1.875rem;
  font-weight: 700;
  color: var(--foreground);
  letter-spacing: -0.02em;
  margin: 0 0 0.5rem 0;
}

.brand-subtitle {
  font-size: 0.875rem;
  color: var(--muted-foreground);
  margin: 0;
}

/* ===== 模式切换 Pill ===== */
.mode-toggle-pill {
  display: flex;
  background: var(--muted);
  border-radius: 2rem;
  padding: 0.25rem;
  margin-bottom: 2rem;
}

.mode-btn {
  flex: 1;
  padding: 0.5rem 1rem;
  border: none;
  background: transparent;
  color: var(--muted-foreground);
  font-size: 0.875rem;
  font-weight: 500;
  border-radius: 2rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.mode-btn.active {
  background: var(--card);
  color: var(--foreground);
  box-shadow: 0 1px 3px oklch(0.2 0.02 220 / 0.1);
}

/* ===== 表单 ===== */
.auth-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.field-wrapper {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  transition: max-height 0.4s cubic-bezier(0.4, 0, 0.2, 1), 
              opacity 0.4s cubic-bezier(0.4, 0, 0.2, 1),
              margin 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.field-enter {
  max-height: 8rem;
  opacity: 1;
}

.field-label {
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--foreground);
}

/* ===== 输入框 ===== */
.input-wrapper {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.625rem 1rem;
  border-radius: 0.75rem;
  border: 1px solid var(--border);
  background: var(--input);
  transition: all 0.2s ease;
}

.input-wrapper:focus-within {
  border: 2px solid #22d3ee;
  background: white;
  padding: calc(0.625rem - 1px) calc(1rem - 1px);
}

.input-icon {
  flex-shrink: 0;
  color: var(--muted-foreground);
  transition: color 0.2s ease;
}

.input-icon.focused {
  color: #22d3ee;
}

.input-wrapper input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 0.875rem;
  color: var(--foreground);
}

.input-wrapper input::placeholder {
  color: oklch(0.5 0.02 220 / 0.6);
}

.toggle-password {
  flex-shrink: 0;
  background: none;
  border: none;
  color: var(--muted-foreground);
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
  transition: color 0.2s ease;
}

.toggle-password:hover {
  color: var(--primary);
}

/* ===== 记住我 + 忘记密码 ===== */
.remember-forgot-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.875rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--foreground);
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: 1rem;
  height: 1rem;
  cursor: pointer;
}

.forgot-link {
  color: var(--primary);
  text-decoration: none;
  font-weight: 500;
  transition: opacity 0.2s ease;
}

.forgot-link:hover {
  opacity: 0.8;
}

/* ===== 错误信息 ===== */
.error-message {
  color: oklch(0.55 0.22 25);
  font-size: 0.875rem;
  text-align: center;
  margin: 0;
  padding: 0.75rem;
  background: oklch(0.55 0.22 25 / 0.1);
  border-radius: 0.5rem;
}

/* ===== 提交按钮 ===== */
.submit-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  height: 2.75rem;
  width: 100%;
  border-radius: 2rem;
  background: linear-gradient(135deg, #0891b2, #06b6d4);
  color: white;
  font-size: 0.875rem;
  font-weight: 600;
  border: none;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(8, 145, 178, 0.35);
  transition: all 0.3s ease;
  overflow: hidden;
}

.submit-btn:hover:not(:disabled) {
  box-shadow: 0 8px 28px rgba(8, 145, 178, 0.45);
  transform: translateY(-2px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-btn::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transform: translateX(-100%);
  transition: transform 0.7s ease;
}

.submit-btn:hover:not(:disabled)::after {
  transform: translateX(100%);
}

.loading-content {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.spinner {
  width: 1rem;
  height: 1rem;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: white;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.arrow-icon {
  flex-shrink: 0;
}

/* ===== 分割线 ===== */
.divider {
  position: relative;
  text-align: center;
  margin: 1.5rem 0;
}

.divider::before,
.divider::after {
  content: '';
  position: absolute;
  top: 50%;
  width: 42%;
  height: 1px;
  background: var(--border);
}

.divider::before {
  left: 0;
}

.divider::after {
  right: 0;
}

.divider span {
  background: var(--card);
  padding: 0 1rem;
  color: var(--muted-foreground);
  font-size: 0.75rem;
  position: relative;
}

/* ===== GitHub 按钮 ===== */
.github-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  width: 100%;
  padding: 0.625rem 1rem;
  border: 1px solid var(--border);
  background: oklch(0.95 0.01 220 / 0.5);
  border-radius: 2rem;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--foreground);
  cursor: pointer;
  transition: all 0.2s ease;
}

.github-btn:hover {
  border-color: oklch(0.6 0.18 220 / 0.3);
  background: var(--secondary);
  box-shadow: 0 4px 12px oklch(0.6 0.18 220 / 0.05);
  transform: translateY(-2px);
}

.github-icon {
  flex-shrink: 0;
}

/* ===== 底部切换文本 ===== */
.toggle-text {
  text-align: center;
  font-size: 0.875rem;
  color: var(--muted-foreground);
  margin: 1.5rem 0 0 0;
}

.toggle-link {
  color: var(--primary);
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.toggle-link:hover {
  opacity: 0.8;
  text-decoration: underline;
}

/* ===== 响应式 ===== */
@media (max-width: 640px) {
  .auth-card {
    padding: 1.5rem;
  }
  
  .video-entrance-btn {
    top: 1rem;
    right: 1rem;
    padding: 0.625rem 1rem;
    font-size: 0.8125rem;
  }
}
</style>

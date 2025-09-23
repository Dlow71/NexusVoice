<template>
  <div class="login-container">
    <div class="form-wrapper">
      <h1 class="title">Nexus Voice</h1>
      <h2 class="subtitle">{{ isRegistering ? '创建新账户' : '欢迎回来' }}</h2>

      <!-- 登录/注册表单 -->
      <form @submit.prevent="handleSubmit">
        <div class="input-group">
          <label for="email">邮箱</label>
          <input type="email" id="email" v-model="formData.email" required>
        </div>
        <div class="input-group">
          <label for="password">密码</label>
          <input type="password" id="password" v-model="formData.password" required>
        </div>

        <!-- 此输入框只在注册模式下显示 -->
        <div v-if="isRegistering" class="input-group">
          <label for="confirmPassword">确认密码</label>
          <input type="password" id="confirmPassword" v-model="formData.confirmPassword" required>
        </div>

        <button type="submit" class="submit-button">
          {{ isRegistering ? '注册' : '登录' }}
        </button>
      </form>

      <p class="toggle-text">
        {{ isRegistering ? '已经有账户了？' : '还没有账户？' }}
        <a href="#" @click.prevent="toggleMode">
          {{ isRegistering ? '立即登录' : '立即注册' }}
        </a>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
// 引入 auth store 用于调用登录和注册方法
import { useAuthStore } from '../stores/auth';

const authStore = useAuthStore();

// 定义一个变量来控制当前是登录模式还是注册模式
const isRegistering = ref(false);
// 定义一个响应式对象来存储表单数据
const formData = ref({
  email: '',
  password: '',
  confirmPassword: ''
});

// 切换登录/注册模式的函数
const toggleMode = () => {
  isRegistering.value = !isRegistering.value;
};

// 处理表单提交的函数
const handleSubmit = () => {
  if (isRegistering.value) {
    // 处理注册逻辑
    if (formData.value.password !== formData.value.confirmPassword) {
      alert('两次输入的密码不一致！');
      return;
    }
    authStore.register(formData.value);
  } else {
    // 处理登录逻辑
    authStore.login(formData.value);
  }
};
</script>

<style scoped>
/* 登录页面的整体布局 */
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.form-wrapper {
  width: 100%;
  max-width: 400px;
  padding: 2.5rem;
  background-color: #2f2f2f;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.5);
}
.title {
  text-align: center;
  font-size: 2.5rem;
  font-weight: bold;
  margin-bottom: 0.5rem;
}
.subtitle {
  text-align: center;
  font-size: 1.2rem;
  color: #aaa;
  margin-bottom: 2rem;
}

/* 表单输入组的样式 */
.input-group {
  margin-bottom: 1.5rem;
}
.input-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #ccc;
}
.input-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #555;
  background-color: #1e1e1e;
  color: white;
  border-radius: 8px;
  font-size: 1rem;
  box-sizing: border-box;
}

/* 提交按钮 */
.submit-button {
  width: 100%;
  padding: 0.8rem;
  border: none;
  background-color: #3b82f6;
  color: white;
  border-radius: 8px;
  font-size: 1.1rem;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}
.submit-button:hover {
  background-color: #2563eb;
}

/* 切换模式的文字 */
.toggle-text {
  text-align: center;
  margin-top: 1.5rem;
  color: #888;
}
.toggle-text a {
  color: #60a5fa;
  text-decoration: none;
  font-weight: bold;
}
.toggle-text a:hover {
  text-decoration: underline;
}
</style>


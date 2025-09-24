<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <span>Nexus Voice 管理后台</span>
        </div>
      </template>

      <el-form :model="form" label-width="80px" @submit.prevent="handleLogin">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input type="password" v-model="form.password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit" class="login-button">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue';
import { useUserStore } from '../stores/user';

const userStore = useUserStore();

const form = reactive({
  username: '',
  password: ''
});

const handleLogin = () => {
  if (form.username && form.password) {
    userStore.login(form.username, form.password);
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.login-card {
  width: 450px;
}

.card-header {
  text-align: center;
  font-size: 1.5rem;
  font-weight: bold;
}

.login-button {
  width: 100%;
}
</style>

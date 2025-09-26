<template>
  <el-container class="main-layout">
    <!-- 侧边栏菜单 -->
    <el-aside width="200px" class="sidebar">
      <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical-demo"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
      >
        <div class="logo-container">
          Nexus Voice Admin
        </div>
        <el-menu-item index="/">
          <el-icon><DataLine /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/characters">
          <el-icon><User /></el-icon>
          <span>角色管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="top-header">
        <!-- 面包屑导航 -->
        <el-breadcrumb separator="/">
          <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="{ path: item.path }">
            {{ item.title }}
          </el-breadcrumb-item>
        </el-breadcrumb>

        <!-- 用户信息下拉菜单 -->
        <el-dropdown @command="handleCommand">
          <span class="el-dropdown-link">
            <el-avatar size="small" class="user-avatar"> A </el-avatar>
            <span class="user-name">{{ userStore.username || 'Admin' }}</span>
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="content-area">
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, watch, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useUserStore } from '../stores/user';
import { DataLine, User, ArrowDown } from '@element-plus/icons-vue';

const route = useRoute();
const userStore = useUserStore();

// 计算属性，用于高亮当前激活的菜单项
const activeMenu = computed(() => route.path);

// 面包屑数据
const breadcrumbs = ref([]);

// 监听路由变化，动态更新面包屑
watch(
    () => route.matched,
    (newRoute) => {
      // 过滤掉没有名字的路由记录，并格式化
      breadcrumbs.value = newRoute
          .filter(item => item.meta && item.meta.title)
          .map(item => ({
            title: item.meta.title, // 使用路由的 name 属性作为面包屑的文本
            path: item.path,
          }));
    },
    { immediate: true } // 立即执行一次，确保初始加载时也有面包屑
);

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'logout') {
    userStore.logout();
  }
};
</script>

<style scoped>
.main-layout {
  height: 100vh;
}
.sidebar {
  background-color: #304156;
  transition: width 0.28s;
}
.logo-container {
  padding: 20px;
  text-align: center;
  color: white;
  font-weight: bold;
  font-size: 1.2rem;
  background-color: #263445;
}
.el-menu {
  border-right: none !important;
}

/* 顶部导航栏样式 */
.top-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);
  padding: 0 20px;
}

/* 用户信息下拉菜单样式 */
.el-dropdown-link {
  cursor: pointer;
  display: flex;
  align-items: center;
}
.user-avatar {
  margin-right: 8px;
  background-color: #409EFF;
}
.user-name {
  color: #333;
}

.content-area {
  padding: 20px;
  background-color: #f0f2f5;
}
</style>


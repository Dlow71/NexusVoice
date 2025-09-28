<template>
  <div class="character-card" @click="selectCharacter">
    <img :src="character.avatarUrl || 'placeholder.svg'" @error="onImageError" alt="avatar" class="avatar">

    <div class="info">
      <h3>{{ character.name }}</h3>
      <p>{{ character.description }}</p>
    </div>

    <div v-if="isPrivate" class="actions">
      <button @click.stop="$emit('edit', character)" class="action-btn edit-btn">编辑</button>
      <button @click.stop="$emit('delete', character.id)" class="action-btn delete-btn">删除</button>
    </div>
  </div>
</template>

<script setup>
import {useRouter} from 'vue-router';
import characterApi from '../services/character';
// 定义从父组件接收的 props
// isPrivate 用来判断是否为私有角色，以决定是否显示操作按钮
const props = defineProps({
  character: {
    type: Object,
    required: true,
  },
  isPrivate: {
    type: Boolean,
    default: false,
  },
});

// 定义 emit 事件，用于通知父组件进行编辑或删除操作
const emit = defineEmits(['edit', 'delete']);

const router = useRouter();

// 点击卡片跳转到对应的聊天页面
const selectCharacter = async () => {
  try {
    // 1. 调用创建会话的接口，并传入角色ID
    const response = await characterApi.createConversation({
      roleId: props.character.id,
    });

    if (response.data.success) {
      // 2. 从响应中获取新创建的 conversationId
      const conversationId = response.data.data.conversationId;

      // 3. 使用 conversationId 跳转到聊天页面
      router.push(`/chat/${conversationId}`);
    } else {
      throw new Error(response.data.message || '创建会话失败');
    }
  } catch (error) {
    console.error("创建会话失败:", error);
    ElMessage.error(error.message || '无法开始新对话，请稍后再试。');
  }
};

// 当图片加载失败时，显示一个备用图片
const onImageError = (event) => {
  event.target.src = new URL('../assets/placeholder.svg', import.meta.url).href;
};
</script>

<style scoped>
/* 卡片主容器样式 */
.character-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  background-color: #2d3748; /* 卡片背景色 */
  border-radius: 12px;
  padding: 1.5rem;
  box-sizing: border-box;
  cursor: pointer;
  overflow: hidden; /* 隐藏溢出的 .actions 层 */
  transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
  border: 1px solid #4a5568;
  height: 100%; /* 确保网格中卡片等高 */
}

/* 卡片鼠标悬浮效果 */
.character-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.3);
}

/* 角色头像样式 */
.avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 1rem;
  border: 2px solid #4a5568;
}

/* 角色信息区域样式 */
.info {
  flex-grow: 1;
}

.info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.2rem;
  font-weight: 600;
  color: #e2e8f0;
}

.info p {
  margin: 0;
  font-size: 0.9rem;
  color: #a0aec0;
  line-height: 1.4;
}

/* 操作按钮层样式 */
.actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  transform: translateY(100%); /* 默认隐藏在卡片下方 */
  transition: transform 0.2s ease-in-out;
}

/* 鼠标悬浮在卡片上时，显示操作按钮 */
.character-card:hover .actions {
  transform: translateY(0);
}

.action-btn {
  flex: 1;
  padding: 0.75rem;
  background: none;
  border: none;
  color: white;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.2s;
}

.edit-btn {
  /* 用右边框作为按钮分隔线 */
  border-right: 1px solid rgba(255, 255, 255, 0.2);
}

.edit-btn:hover {
  background-color: rgba(59, 130, 246, 0.5); /* 蓝色 */
}

.delete-btn:hover {
  background-color: rgba(239, 68, 68, 0.5); /* 红色 */
}
</style>
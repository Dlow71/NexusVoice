<template>
  <div class="character-card" @click="selectCharacter">
    <img :src="character.avatarUrl || defaultAvatar" @error="onImageError" alt="avatar" class="avatar">

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
import { STAR_IMAGE_FALLBACK, replaceImageWithStarFallback } from '../utils/starFallback';
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
const defaultAvatar = STAR_IMAGE_FALLBACK;

// 点击卡片跳转到对应的聊天页面
const selectCharacter = async () => {
  try {
    // 方案1：默认使用WebSocket流式聊天（推荐）
    router.push(`/stream/${props.character.id}`);
    
    // 方案2：如果需要兼容HTTP版本，可以通过配置选择
    // const useStream = localStorage.getItem('chat-mode') !== 'http';
    // if (useStream) {
    //   router.push(`/stream/${props.character.id}`);
    // } else {
    //   // 原有的HTTP版本逻辑
    //   const response = await characterApi.createConversation({
    //     roleId: props.character.id,
    //   });
    //   if (response.data.success) {
    //     const conversationId = response.data.data.conversationId;
    //     router.push(`/chat/${conversationId}`);
    //   } else {
    //     throw new Error(response.data.message || '创建会话失败');
    //   }
    // }
  } catch (error) {
    console.error("跳转失败:", error);
    ElMessage.error(error.message || '无法开始对话，请稍后再试。');
  }
};

// 当图片加载失败时，显示一个备用图片
const onImageError = (event) => {
  replaceImageWithStarFallback(event.target);
};
</script>

<style scoped>
/* 卡片主容器样式 - 浅色主题 */
.character-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  background-color: white;
  border-radius: 16px;
  padding: 1.5rem;
  box-sizing: border-box;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  height: 100%;
}

/* 卡片鼠标悬浮效果 */
.character-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(59, 130, 246, 0.15);
  border-color: #3b82f6;
}

/* 角色头像样式 */
.avatar {
  width: 80px;
  height: 80px;
  display: block;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 1rem;
  border: 3px solid #e2e8f0;
  background: #e0f2fe;
  transition: all 0.3s ease;
}

.character-card:hover .avatar {
  border-color: #3b82f6;
  transform: scale(1.05);
}

/* 角色信息区域样式 */
.info {
  flex-grow: 1;
}

.info h3 {
  margin: 0 0 0.5rem 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #1e293b;
}

.info p {
  margin: 0;
  font-size: 0.85rem;
  color: #64748b;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 操作按钮层样式 */
.actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  background: linear-gradient(to top, rgba(255, 255, 255, 0.98), rgba(255, 255, 255, 0.95));
  backdrop-filter: blur(8px);
  border-top: 1px solid #e2e8f0;
  transform: translateY(100%);
  transition: transform 0.3s ease;
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
  color: #64748b;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-btn {
  border-right: 1px solid #e2e8f0;
}

.edit-btn:hover {
  background-color: #3b82f6;
  color: white;
}

.delete-btn:hover {
  background-color: #ef4444;
  color: white;
}
</style>

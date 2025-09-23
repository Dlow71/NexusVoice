<template>

  <router-link :to="`/chat/${character.id}`" class="card-link">
    <div class="card">
      <!-- 角色的头像 -->
      <img :src="getAvatarUrl(character.avatar)" alt="avatar" class="avatar">

      <!-- 右侧的文字信息区域 -->
      <div class="info">
        <h2 class="name">{{ character.name }}</h2>
        <p class="description">{{ character.description }}</p>
      </div>
    </div>
  </router-link>
</template>

<script setup>
defineProps({
  character: {
    type: Object,
    required: true
  }
});


const getAvatarUrl = (name) => {
  return new URL(`../assets/${name}`, import.meta.url).href;
};
</script>

<style scoped>



.card-link {
  text-decoration: none;
  color: inherit;
}

/* 卡片本身的主要样式 */
.card {
  background-color: #2f2f2f;
  border-radius: 12px;
  padding: 1.5rem;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

/* 要给卡片加一个鼠标悬浮效果 */
.card:hover {
  transform: translateY(-8px);
  box-shadow: 0 8px 30px rgba(0, 255, 255, 0.2);
  border-color: cyan; /* 边框也变成青色 */
}

/* 头像的样式 */
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%; /* 圆形头像 */
  margin-right: 1.5rem; /* 和右边的文字拉开距离 */
  background-color: #444;
  padding: 4px;
  flex-shrink: 0; /* 防止头像在 flex 布局中被意外压缩变形 */
}

/* 文字信息区域的容器 */
.info {
  text-align: left;
}

/* 角色名字的样式 */
.name {
  font-size: 1.5rem;
  font-weight: bold;
  margin: 0; /* 去掉 h2 标签的默认 margin */
  color: #ffffff;
}

/* 角色描述的样式 */
.description {
  margin: 0.25rem 0 0; /* 和上面的名字之间留一点点距离 */
  color: #aaa; /* 描述文字颜色浅一些 */
}
</style>


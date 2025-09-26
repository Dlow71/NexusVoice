<template>
  <!-- 这是角色选择页面的主容器 -->
  <div class="selection-container">
    <!-- 页面的头部，包含标题和副标题 -->
    <header class="header">
      <h1 class="title">Nexus Voice</h1>
      <p class="subtitle">选择一位角色，开启对话</p>
    </header>

    <!-- 主内容区域，在这里用网格布局来展示角色卡片 -->
    <main class="card-grid">
      <CharacterCard
          v-for="character in characters"
          :key="character.id"
          :character="character"
      />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import CharacterCard from '../components/CharacterCard.vue';

//虚拟数据
const characters = ref([
  { id: 'harry_potter', name: '哈利·波特', description: '来自霍格沃茨的年轻巫师。', avatar: 'placeholder.svg' },
  { id: 'socrates', name: '苏格拉底', description: '古希腊的哲学家。', avatar: 'placeholder.svg' },
  { id: 'eva_explorer', name: '宇航员伊娃', description: '一位星际探险家。', avatar: 'placeholder.svg' },
]);
</script>

<style scoped>
.selection-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh; /* 确保容器至少和屏幕一样高 */
  padding: 2rem;
  box-sizing: border-box; /* 这样 padding 就不会撑开容器，导致出现滚动条 */
}

/* 头部区域的样式 */
.header {
  text-align: center;
  margin-bottom: 3rem; /* 和下面的卡片网格拉开距离 */
}

/* 主标题的样式 */
.title {
  font-size: 3.2rem;
  font-weight: bold;
}

/* 副标题的样式 */
.subtitle {
  font-size: 1.2rem;
  color: #888; /* 颜色浅一点 */
  margin-top: 0.5rem;
}

/* 角色卡片网格的样式 */
.card-grid {
  display: grid;
  /* 默认情况下，显示3列，每列的宽度平均分配 */
  grid-template-columns: repeat(3, 1fr);
  gap: 2rem; /* 设置网格项之间的间距 */
  width: 100%;
  max-width: 1200px; /* 给网格一个最大宽度，避免在大屏幕上拉得太开 */
}

/* 不同屏幕尺寸设置响应式布局 */

/* 当屏幕宽度小于 1024px 时 (比如平板电脑)，让网格变为2列 */
@media (max-width: 1024px) {
  .card-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 当屏幕宽度小于 768px 时 (比如手机)，网格变为1列，并且缩小标题字号 */
@media (max-width: 768px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
  .title {
    font-size: 2.5rem;
  }
}
</style>


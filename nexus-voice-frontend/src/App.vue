<script setup>
import { onMounted } from 'vue';
import { useThemeStore } from './stores/theme';

const themeStore = useThemeStore();

onMounted(() => {
  themeStore.init();
});
</script>

<template>
  <router-view />
</template>

<style scoped>
</style>

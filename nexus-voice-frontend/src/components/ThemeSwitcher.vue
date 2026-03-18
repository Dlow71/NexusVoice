<template>
  <div class="theme-switcher">
    <button class="theme-trigger" @click="isOpen = !isOpen" title="切换主题">
      <span class="theme-trigger-icon">🎨</span>
    </button>

    <Transition name="theme-popup">
      <div v-if="isOpen" class="theme-popup" @click.stop>
        <div class="theme-popup-header">主题切换</div>
        <div class="theme-options">
          <button
            v-for="theme in themes"
            :key="theme.key"
            class="theme-option"
            :class="{ active: currentTheme === theme.key }"
            @click="selectTheme(theme.key)"
          >
            <div class="theme-preview">
              <span
                v-for="(color, i) in theme.preview"
                :key="i"
                class="preview-dot"
                :style="{ backgroundColor: color }"
              />
            </div>
            <div class="theme-info">
              <span class="theme-name">{{ theme.name }}</span>
              <span class="theme-desc">{{ theme.description }}</span>
            </div>
            <span v-if="currentTheme === theme.key" class="theme-check">✓</span>
          </button>
        </div>
      </div>
    </Transition>

    <!-- 遮罩 -->
    <div v-if="isOpen" class="theme-backdrop" @click="isOpen = false" />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import { useThemeStore } from '../stores/theme';

const themeStore = useThemeStore();
const isOpen = ref(false);

const themes = computed(() => themeStore.themeList);
const currentTheme = computed(() => themeStore.currentTheme);

const selectTheme = (key) => {
  themeStore.setTheme(key);
  isOpen.value = false;
};
</script>

<style scoped>
.theme-switcher {
  position: relative;
}

.theme-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--surface-glass);
  border: 1px solid var(--border-default);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  font-size: 1rem;
}

.theme-trigger:hover {
  background: var(--surface-glass-hover);
  border-color: var(--border-strong);
  transform: scale(1.05);
}

.theme-popup {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 240px;
  background: var(--surface-overlay);
  border: 1px solid var(--border-default);
  border-radius: 12px;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(255, 255, 255, 0.04);
  z-index: 1000;
  overflow: hidden;
}

.theme-popup-header {
  padding: 0.65rem 1rem;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--text-dim);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  border-bottom: 1px solid var(--border-subtle);
}

.theme-options {
  padding: 0.35rem;
}

.theme-option {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  width: 100%;
  padding: 0.55rem 0.65rem;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  text-align: left;
}

.theme-option:hover {
  background: var(--surface-glass-hover);
  border-color: var(--border-subtle);
}

.theme-option.active {
  background: var(--primary-soft-bg);
  border-color: var(--primary-soft-border);
}

.theme-preview {
  display: flex;
  gap: 3px;
  flex-shrink: 0;
}

.preview-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.theme-info {
  display: flex;
  flex-direction: column;
  gap: 1px;
  flex: 1;
  min-width: 0;
}

.theme-name {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-default);
}

.theme-desc {
  font-size: 0.7rem;
  color: var(--text-dim);
}

.theme-check {
  color: var(--text-accent-strong);
  font-weight: bold;
  font-size: 0.9rem;
  flex-shrink: 0;
}

.theme-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 999;
}

/* Transitions */
.theme-popup-enter-active,
.theme-popup-leave-active {
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.theme-popup-enter-from,
.theme-popup-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.96);
}
</style>

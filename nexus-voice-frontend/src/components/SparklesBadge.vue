<template>
  <div class="sparkles-badge" :class="{ hoverable }" :style="badgeStyle">
    <svg class="sparkles-icon" viewBox="0 0 24 24" aria-hidden="true" :style="iconStyle">
      <path d="M9.94 15.5A2 2 0 0 0 8.5 14.06l-6.14-1.58a.5.5 0 0 1 0-.96L8.5 9.94A2 2 0 0 0 9.94 8.5l1.58-6.14a.5.5 0 0 1 .96 0L14.06 8.5A2 2 0 0 0 15.5 9.94l6.14 1.58a.5.5 0 0 1 0 .96L15.5 14.06a2 2 0 0 0-1.44 1.44l-1.58 6.14a.5.5 0 0 1-.96 0z" />
      <path d="M20 3v4" />
      <path d="M22 5h-4" />
      <path d="M4 17v2" />
      <path d="M5 18H3" />
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  size: {
    type: Number,
    default: 48,
  },
  iconSize: {
    type: Number,
    default: 28,
  },
  rounded: {
    type: Number,
    default: 16,
  },
  hoverable: {
    type: Boolean,
    default: false,
  },
});

const badgeStyle = computed(() => ({
  '--badge-size': `${props.size}px`,
  '--badge-radius': `${props.rounded}px`,
}));

const iconStyle = computed(() => ({
  '--icon-size': `${props.iconSize}px`,
}));
</script>

<style scoped>
.sparkles-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: var(--badge-size);
  height: var(--badge-size);
  border-radius: var(--badge-radius);
  background: rgba(8, 145, 178, 0.1);
  color: #0891b2;
  border: 1px solid rgba(8, 145, 178, 0.08);
  transition: transform 0.3s ease, background-color 0.3s ease, color 0.3s ease, border-color 0.3s ease;
}

.sparkles-badge.hoverable:hover {
  background: rgba(8, 145, 178, 0.2);
  border-color: rgba(8, 145, 178, 0.14);
  transform: scale(1.1);
}

.sparkles-icon {
  display: block;
  width: var(--icon-size);
  height: var(--icon-size);
  fill: none;
  stroke: currentColor;
  stroke-width: 2.25;
  stroke-linecap: round;
  stroke-linejoin: round;
  shape-rendering: geometricPrecision;
  vector-effect: non-scaling-stroke;
  transition: transform 0.3s ease;
}

.sparkles-badge.hoverable:hover .sparkles-icon {
  transform: rotate(12deg);
}
</style>

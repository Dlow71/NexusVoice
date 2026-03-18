import { defineStore } from 'pinia';

export const THEMES = [
  {
    key: 'ocean-blue',
    name: 'Ocean Blue',
    description: '极简白 · 经典海洋',
    preview: ['#3b82f6', '#06b6d4', '#f8fafc'],
  },
  {
    key: 'glass-premium',
    name: 'Glass Premium',
    description: '靛蓝紫 · 经典琉璃',
    preview: ['#6366f1', '#8b5cf6', '#050a18'],
  },
  {
    key: 'cyberpunk-neon',
    name: 'Cyberpunk Neon',
    description: '青品红 · 赛博朋克',
    preview: ['#00f0ff', '#ff007f', '#040510'],
  },
  {
    key: 'sakura-anime',
    name: 'Sakura Anime',
    description: '樱粉紫 · 二次元',
    preview: ['#ff7eb3', '#ff758c', '#1a101f'],
  },
];

export const useThemeStore = defineStore('theme', {
  state: () => ({
    currentTheme: 'ocean-blue',
  }),

  getters: {
    themeList: () => THEMES,
    current: (state) => THEMES.find((t) => t.key === state.currentTheme) || THEMES[0],
  },

  actions: {
    setTheme(themeKey) {
      const valid = THEMES.find((t) => t.key === themeKey);
      if (!valid) return;
      this.currentTheme = themeKey;
      this._applyTheme(themeKey);
    },

    init() {
      this._applyTheme(this.currentTheme);
    },

    _applyTheme(themeKey) {
      document.documentElement.setAttribute('data-theme', themeKey);
    },
  },

  persist: true,
});

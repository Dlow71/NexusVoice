import { createApp } from 'vue'
import { createPinia } from "pinia";
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate';
import App from './App.vue'
// 1. 引入 Element Plus 的所有组件和样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'

import './style.css'
import './assets/element-overrides.css'
import './assets/themes/ocean-blue.css'
import './assets/themes/cyberpunk-neon.css'
import './assets/themes/sakura-anime.css'

const app = createApp(App)

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);
app.use(ElementPlus);
app.use(pinia);
app.use(router);
app.mount('#app')

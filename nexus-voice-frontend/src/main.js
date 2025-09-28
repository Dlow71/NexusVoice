import { createApp } from 'vue'
import {createPinia} from "pinia";
import App from './App.vue'
// 1. 引入 Element Plus 的所有组件和样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router'

import './style.css'

const app = createApp(App)

app.use(ElementPlus);
app.use(createPinia());
app.use(router);
app.mount('#app')


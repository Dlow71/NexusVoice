import { createApp } from 'vue'
import ElementPlus from 'element-plus' //引入elementplus库
import 'element-plus/dist/index.css'
import {createPinia} from "pinia";
import router from './router'
import App from './App.vue'

createApp(App)
    .use(createPinia())
    .use(ElementPlus)
    .use(router)
    .mount('#app')

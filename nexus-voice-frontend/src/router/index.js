import { createRouter, createWebHistory } from 'vue-router'
import {useAuthStore} from "../stores/auth.js";

// 使用路由懒加载，按需加载组件，减少首屏加载时间

// 定义应用的路由规则
const routes = [
    {
        path:'/login',
        name:'Login',
        component: () => import('../views/LoginView.vue'),
    },
    {
        path: '/', // 网站的根路径，重定向到 /characters
        redirect: '/characters',
    },
    {
        path: '/characters', // 角色选择页面
        name: 'CharacterSelection',
        component: () => import('../views/CharacterSelectionView.vue'),
        meta:{requiresAuth: true},
    },
    {
        // 定义一个动态路径
        path: '/chat/:id',
        name: 'Chat',
        // 使用路由懒加载，只有当用户访问这个页面时，才会去加载 ChatView.vue 文件
        component: () => import('../views/ChatView.vue'),
        meta:{requiresAuth: true},
    },
    {
        path: '/stream/:roleId',
        name: 'StreamChat',
        component: () => import('../views/ChatStreamView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/lab',
        name: 'Lab',
        component: () => import('../views/LabView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/random-video',
        name: 'RandomVideo',
        component: () => import('../views/RandomVideoView.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/oauth/callback',
        name: 'OAuthCallback',
        component: () => import('../views/OAuthCallbackView.vue')
    },
    {
        path: '/developer/api-keys',
        name: 'ApiKeyManagement',
        component: () => import('../views/ApiKeyManagementView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/rag',
        name: 'RagKnowledgeBase',
        component: () => import('../views/RagKnowledgeBaseView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/rtc-voice',
        name: 'RtcVoice',
        component: () => import('../views/RtcVoiceView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/voice-call',
        name: 'VoiceCall',
        component: () => import('../views/VoiceCallView.vue'),
        meta: { requiresAuth: true }
    }
]

// 创建路由实例
const router = createRouter({
    history: createWebHistory(),
    routes: routes
})
//添加全局路由守卫
router.beforeEach((to, from, next) => {
    const authStore=useAuthStore();
    //检查目标路由是否需要登陆验证
    if (to.meta.requiresAuth&&!authStore.isLoggedIn){
        //跳转到登陆页面
        next({name: 'Login'});
    }else {
        //放行
        next();
    }



})
// 导出路由实例，以便在 main.js 中使用
export default router

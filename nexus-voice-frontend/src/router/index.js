import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
// 角色选择页面
import CharacterSelectionView from '../views/CharacterSelectionView.vue'
import {useAuthStore} from "../stores/auth.js";

// 定义应用的路由规则
const routes = [
    {
        path:'/login',
        name:'Login',
        component: LoginView,
    },
    {
        path: '/', // 网站的根路径 (例如 http://localhost:5173/)
        name: 'CharacterSelection',
        component: CharacterSelectionView,
        meta:{requiresAuth: true},
    },
    {
        // 定义一个动态路径
        path: '/chat/:id',
        name: 'Chat',
        // 使用路由懒加载，只有当用户访问这个页面时，才会去加载 ChatView.vue 文件
        component: () => import('../views/ChatView.vue'),
        meta:{requiresAuth: true},
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


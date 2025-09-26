import { createRouter, createWebHistory } from 'vue-router';
// 确保引入了所有需要的组件，包括用户 stores
import { useUserStore } from '../stores/user';
import DashboardView from '../views/DashboardView.vue';
import CharacterManagementView from '../views/CharacterManagementView.vue';
import LoginView from '../views/LoginView.vue';
import AdminLayout from "../layouts/AdminLayout.vue";

const routes = [
    {
        // 登录页路由
        path: '/login',
        name: 'Login',
        component: LoginView,
        meta: {title: '登录'}
    },
    {
        // 配置一级路由
        path: '/',
        name: 'AdminLayout',
        component: AdminLayout,
        meta: { requiresAuth: true ,title: '首页'},
        children: [
            {
                // /默认路径展示面板信息
                path: '',
                name: 'Dashboard',
                component: DashboardView,
                meta: {title: '数据看板' },
            },
            {
                path: 'characters',
                name: 'CharacterManagement',
                component: CharacterManagementView,
                meta: { title: '角色管理' }
            }
        ]
    },
];

const router = createRouter({
    history: createWebHistory(),
    routes
});


// 路由守卫
// router.beforeEach() 会在每一次路由跳转之前被触发
router.beforeEach((to, from, next) => {
    // 在守卫内部，可以访问 Pinia 的 stores
    const userStore = useUserStore();
    // 检查用户是否已登录 (通过判断 token 是否存在)
    const isLoggedIn = !!userStore.token;

    // 检查目标页面 (to) 是否需要认证
    if (to.meta.requiresAuth) {
        // 如果页面需要认证，但用户未登录
        if (!isLoggedIn) {
            // 将用户重定向到登录页
            next('/login');
        } else {
            // 如果用户已登录，则允许访问
            next();
        }
    } else {
        // 如果页面不需要认证，则直接允许访问
        next();
    }
});

export default router;


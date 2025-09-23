import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import router from '../router';

//用户状态管理
// 'defineStore' 的第一个参数是这个 store 的唯一 ID
export const useAuthStore = defineStore('auth', () => {
    //存储用户信息
    const user = ref(JSON.parse(localStorage.getItem('user')));

    // --- Getters ---
    // 通过计算属性来判断用户是否已登录
    const isLoggedIn = computed(() => !!user.value);

    // 这是处理登录逻辑的函数
    function login(userData) {
        //模拟数据
        const fakeUser = { name: userData.email, email: userData.email };

        // 登录成功后，把用户信息保存到 state 和 localStorage 里
        user.value = fakeUser;
        localStorage.setItem('user', JSON.stringify(fakeUser));

        // 登录成功后，需要跳转到主页（角色选择页）
        router.push('/');
    }

    // 这是处理注册逻辑的函数
    function register(userData) {
        // 同样，这里也是模拟
        console.log('正在注册:', userData);
        login(userData);
    }

    // 这是处理登出逻辑的函数
    function logout() {
        // 我需要清空 state 和 localStorage
        user.value = null;
        localStorage.removeItem('user');

        // 登出后，我需要跳转回登录页面
        router.push('/login');
    }

    // 最后，我需要把这些 state, getters, 和 actions return 出去，这样组件才能使用它们
    return { user, isLoggedIn, login, register, logout };
});

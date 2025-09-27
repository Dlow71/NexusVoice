import { defineStore } from 'pinia';
// 导入 API 调用函数
import userApi from '../services/user';
import router from '../router';

export const useAuthStore = defineStore('auth', {
    state: () => ({
        token: localStorage.getItem('user-token') || null,
        user: JSON.parse(localStorage.getItem('user-info')) || null,
        refreshToken: localStorage.getItem('user-refresh-token') || null,
    }),

    getters: {
        isLoggedIn: (state) => !!state.token,
    },

    actions: {
        // 注册方法
        async register(userData) {
            const response = await userApi.register(userData);


            if (!response.data.success) {
                // 如果 success 为 false，说明业务上出错了，主动抛出错误
                throw new Error(response.data.message || '注册失败');
            }

            // 只有在 success 为 true 时，才执行成功逻辑
            this.setAuthData(response.data.data);
            router.push('/');
        },

        // 登录方法
        async login(credentials) {
            const response = await userApi.login(credentials);

            if (!response.data.success) {
                // 如果 success 为 false，说明业务上出错了，主动抛出错误
                throw new Error(response.data.message || '登录失败');
            }

            // 只有在 success 为 true 时，才执行成功逻辑
            this.setAuthData(response.data.data);
            router.push('/');
        },
        //用户登出
        async logout() {
            try {
                // 首先尝试调用后端的登出接口
                await userApi.logout();
            } catch (error) {
                // 即使后端接口调用失败 (例如网络错误或token已失效)，
                // 仍然要继续执行前端的登出流程，以确保用户界面恢复到未登录状态。
                console.error("调用登出接口失败:", error);
            } finally {
                // 无论后端调用是否成功，都必须执行以下清理操作
                this.token = null;
                this.refreshToken = null;
                this.user = null;
                localStorage.removeItem('user-token');
                localStorage.removeItem('user-refresh-token');
                localStorage.removeItem('user-info');
                // 跳转回登录页
                router.push('/login');
            }
        },

        setAuthData(data) {
            this.token = data.accessToken;
            this.user = data.userInfo;
            this.refreshToken = data.refreshToken;
            localStorage.setItem('user-refresh-token', data.refreshToken);
            localStorage.setItem('user-token', data.accessToken);
            localStorage.setItem('user-info', JSON.stringify(data.userInfo));
        }
    },
});


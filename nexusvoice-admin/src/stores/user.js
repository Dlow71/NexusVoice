import { defineStore } from 'pinia';

import router from '../router';

export const useUserStore = defineStore('user', {
    // state 负责存储应用的核心数据
    state: () => ({
        //获取初始化数据
        token: localStorage.getItem('admin-token') || null,
        username: localStorage.getItem('admin-username') || null,
    }),

    // actions 负责定义可以修改 state 的方法
    actions: {
        // 登录方法 (目前是模拟的)
        login(username, password) {
            // 返回一个 Promise，模拟异步的 API 请求
            return new Promise((resolve) => {
                setTimeout(() => {
                    // 模拟从后端成功获取到的 token
                    const mockToken = 'mock-jwt-token-' + Date.now();

                    // 1. 更新 Pinia state 中的数据
                    this.token = mockToken;
                    this.username = username;

                    // 2. 将 token 和 username 存入 localStorage，用于持久化
                    localStorage.setItem('admin-token', mockToken);
                    localStorage.setItem('admin-username', username);

                    // Promise 完成，表示登录成功
                    resolve(true);
                }, 500); // 模拟500毫秒的网络延迟
            });
        },

        // 登出方法
        logout() {
            // 1. 清空 Pinia state 中的用户数据
            this.token = null;
            this.username = null;

            // 2. 清空 localStorage 中的持久化数据
            localStorage.removeItem('admin-token');
            localStorage.removeItem('admin-username');

            // 3。使用 router 实例，将用户强制跳转回登录页面
            router.push('/login');
        },
    },
});


// 1. 导入 Axios 实例
import apiClient from './api';

// 2. 导出一个包含了所有用户相关接口函数的对象
export default {
    // 注册接口
    register(userData) {
        return apiClient.post('/auth/register', userData);
    },
    
    // 刷新令牌
    refreshToken(refreshToken) {
        return apiClient.post(
            '/auth/refresh',
            { refreshToken },
            {
                headers: {
                    'X-Client-Type': 'user'
                }
            }
        );
    },

    // 登录接口
    login(credentials) {
        return apiClient.post(
            '/auth/login',
            {
                username: credentials.email, // 使用 email 的值作为 username
                password: credentials.password,
                rememberMe: credentials.rememberMe || false
            },
            {
                headers: {
                    'X-Client-Type': 'user'
                }
            }
        );
    },
    //用户登出
    logout() {
        return apiClient.post('/auth/logout');
    }
};

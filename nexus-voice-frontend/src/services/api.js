import axios from 'axios';
import { useAuthStore } from '../stores/auth';

// 1. 创建一个 Axios 实例
const apiClient = axios.create({
    //从配置文件中读取文件
    baseURL:"http://localhost:8081/api",
    timeout: 300000,
});

// 2. 添加请求拦截器 (Request Interceptor)
apiClient.interceptors.request.use(
    (config) => {
        // 在发送请求之前，从 Pinia store 中获取 token
        const authStore = useAuthStore();
        const token = authStore.token;

        // 如果 token 存在，则将其添加到请求的 Authorization header 中
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        // 标识客户端类型，便于后端按端隔离会话（OAuth回调后的后续请求、刷新请求等也会带上）
        if (!config.headers['X-Client-Type']) {
            config.headers['X-Client-Type'] = 'user';
        }
        return config;
    },
    (error) => {
        // 对请求错误做些什么
        return Promise.reject(error);
    }
);


// 可以在这里统一处理错误，例如 token 过期等
apiClient.interceptors.response.use(
    (response) => {
        // 2xx 范围内的状态码都会触发该函数
        // 对响应数据做点什么
        return response;
    },
    (error) => {
        const authStore = useAuthStore();
        const originalRequest = error.config || {};

        // 仅处理 401 未授权
        if (error.response && error.response.status === 401) {
            // 避免重复刷新
            if (originalRequest._retry) {
                // 已重试过，仍然 401，执行登出
                authStore.logout();
                return Promise.reject(error);
            }

            // 若存在 refreshToken，则尝试刷新并重试原请求
            const storedRefreshToken = authStore.refreshToken;
            if (storedRefreshToken) {
                originalRequest._retry = true;

                // 使用裸 axios 调用刷新接口，避免拦截器相互影响
                const baseURL = apiClient.defaults.baseURL || '';
                const refreshUrl = (baseURL.endsWith('/'))
                    ? `${baseURL}auth/refresh`
                    : `${baseURL}/auth/refresh`;

                return axios.post(
                    refreshUrl,
                    { refreshToken: storedRefreshToken },
                    { headers: { 'X-Client-Type': 'user' } }
                ).then((res) => {
                    // 兼容后端统一返回结构：{ success, data: { accessToken, refreshToken, ... } }
                    const body = res.data || {};
                    if (!body.success || !body.data || !body.data.accessToken) {
                        throw new Error(body.message || '刷新令牌失败');
                    }

                    const newAccessToken = body.data.accessToken;
                    const newRefreshToken = body.data.refreshToken || storedRefreshToken;

                    // 更新 store 和本地缓存
                    authStore.token = newAccessToken;
                    authStore.refreshToken = newRefreshToken;
                    localStorage.setItem('user-token', newAccessToken);
                    localStorage.setItem('user-refresh-token', newRefreshToken);

                    // 更新并重试原请求
                    originalRequest.headers = originalRequest.headers || {};
                    originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;
                    originalRequest.headers['X-Client-Type'] = originalRequest.headers['X-Client-Type'] || 'user';
                    return apiClient(originalRequest);
                }).catch((refreshErr) => {
                    // 刷新失败则登出
                    authStore.logout();
                    return Promise.reject(refreshErr);
                });
            }

            // 无 refreshToken，直接登出
            authStore.logout();
        }

        return Promise.reject(error);
    }
);


// 4. 导出配置好的 Axios 实例
export default apiClient;

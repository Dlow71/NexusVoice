import axios from 'axios';
import { useAuthStore } from '../stores/auth';

// 1. 创建一个 Axios 实例
const apiClient = axios.create({
    //从配置文件中读取文件
    baseURL:"http://localhost:8081/api",
    timeout: 10000,
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
        // 超出 2xx 范围的状态码都会触发该函数
        if (error.response && error.response.status === 401) {
            // 如果收到 401 (未授权) 错误，自动登出用户
            const authStore = useAuthStore();
            authStore.logout();
        }
        // 对响应错误做点什么
        return Promise.reject(error);
    }
);


// 4. 导出配置好的 Axios 实例
export default apiClient;


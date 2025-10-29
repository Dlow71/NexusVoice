import apiClient from './api';

/**
 * 开发者API密钥管理服务
 */
export default {
    /**
     * 查询当前用户的所有API密钥
     */
    listApiKeys() {
        return apiClient.get('/v1/developer/api-keys');
    },

    /**
     * 查询API密钥详情
     * @param {string} id - 密钥ID
     */
    getApiKey(id) {
        return apiClient.get(`/v1/developer/api-keys/${id}`);
    },

    /**
     * 创建新的API密钥
     * @param {Object} data - 创建请求数据
     */
    createApiKey(data) {
        return apiClient.post('/v1/developer/api-keys', data);
    },

    /**
     * 更新API密钥
     * @param {Object} data - 更新请求数据（包含id字段）
     */
    updateApiKey(data) {
        return apiClient.put('/v1/developer/api-keys', data);
    },

    /**
     * 删除API密钥
     * @param {string} id - 密钥ID
     */
    deleteApiKey(id) {
        return apiClient.delete(`/v1/developer/api-keys/${id}`);
    },

    /**
     * 批量删除API密钥
     * @param {Array<string>} ids - 密钥ID列表
     */
    batchDeleteApiKeys(ids) {
        return apiClient.delete('/v1/developer/api-keys/batch', { data: ids });
    },

    /**
     * 启用API密钥
     * @param {string} id - 密钥ID
     */
    enableApiKey(id) {
        return apiClient.post(`/v1/developer/api-keys/${id}/enable`);
    },

    /**
     * 禁用API密钥
     * @param {string} id - 密钥ID
     */
    disableApiKey(id) {
        return apiClient.post(`/v1/developer/api-keys/${id}/disable`);
    },

    /**
     * 查询API密钥统计信息
     */
    getStatistics() {
        return apiClient.get('/v1/developer/api-keys/statistics');
    }
};

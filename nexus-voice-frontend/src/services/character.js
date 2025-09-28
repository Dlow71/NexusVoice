// 1. 导入配置好的 Axios 实例
import apiClient from './api';
import axios from "axios";

// 2. 导出一个包含了所有聊天相关接口函数的对象
export default {
    /**
     * 创建会话
     * @param data
     * @returns {Promise<axios.AxiosResponse<any>>}
     */
    createConversation(data) {
        return apiClient.post('/v1/conversations', data);
    },
    /**
     * 分页浏览公共角色
     * @param {object} params - 包含 page, size, keyword 等分页参数
     */
    getPublicCharacters(params) {
        return apiClient.get('/roles/public', { params });
    },
    /**
     * 获取我的私有角色列表
     * @param {object} params - 包含 page, size, keyword 等分页参数
     */
    getMyCharacters(params) {
        return apiClient.get('/roles/private', { params });
    },

    /**
     * 创建私有角色
     * @param {object} data - 角色数据，包含 name, description等
     */
    createCharacter(data) {
        return apiClient.post('/roles/private', data);
    },
    /**
     * 编辑私有角色
     * @param id
     * @param data
     * @returns {Promise<axios.AxiosResponse<any>>}
     */
    updateCharacter(id, data) {
        return apiClient.put(`/roles/private/${id}`, data);
    },
    /**
     * 删除私有角色
     * @param id
     * @returns {Promise<axios.AxiosResponse<any>>}
     */
    deleteCharacter(id) {
        return apiClient.delete(`/roles/private/${id}`);
    },
    /**
     * 上传头像
     * @param file
     * @returns {Promise<axios.AxiosResponse<any>>}
     */
    uploadImage(file) {
        // 1. 创建一个 FormData 对象来包装文件
        const formData = new FormData();
        // 2. 'file' 是传给后端的 key
        formData.append('file', file);

        // 3. 发送 POST 请求，并设置正确的 Content-Type
        return apiClient.post('/file/upload/image', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
    },
    /**
     * 直接获取七牛云音频（不走后端服务器）
     * @returns {Promise<axios.AxiosResponse<any>>}
     */
    getVoiceList() {
        //访问七牛云音频地址
        const qiniuUrl = "https://openai.qiniu.com/v1/voice/list";

        // 发送一个不带任何多余配置的纯净 GET 请求
        // 这个请求能否成功，完全取决于七牛云服务器的CORS策略
        return axios.get(qiniuUrl);
    },
    /**
     * 获取大语言模型的文本回复
     * @param {object} payload - 包含 text, characterId, history 等的对象
     */
    getLLMReply(payload) {
        // 假设后端 LLM 接口的地址是 /api/llm/chat
        return apiClient.post('/v1/conversations/chat', payload);
    },

    /**
     * 将文本转换为语音
     * @param {object} payload - 包含 text, voiceType 等的对象
     */
    textToSpeech(payload) {
        return apiClient.post('/tts/text-to-speech', payload);
    },
    /**
     * 获取当前用户的会话历史列表
     */
    getConversationHistory() {
        return apiClient.get('/v1/conversations/list');
    },

    /**
     * 根据会话ID获取该会话的所有历史消息
     * @param {string} conversationId - 会话的ID
     */
    getMessagesByConversationId(conversationId) {
        return apiClient.get(`/v1/conversations/${conversationId}/history`);
    },
    /**
     * 除指定ID的对话
     * @param {string} conversationId -
     */
    deleteConversation(conversationId) {
        return apiClient.delete(`/v1/conversations/${conversationId}`);
    },
};


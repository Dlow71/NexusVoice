// 1. 导入配置好的 Axios 实例
import apiClient from './api';

// 2. 导出一个包含了所有聊天相关接口函数的对象
export default {
    /**
     * 分页浏览公共角色
     * @param {object} params - 包含 page, size 等分页参数
     */
    getPublicCharacters(params) {
        return apiClient.get('/characters/public', { params });
    },

    /**
     * 获取我的私人角色列表
     */
    getMyCharacters() {
        return apiClient.get('/characters/my-private');
    },

    /**
     * 创建一个新的私人角色
     * @param {object} characterData - 包含角色信息的对象
     */
    createCharacter(characterData) {
        return apiClient.post('/characters/private', characterData);
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
};


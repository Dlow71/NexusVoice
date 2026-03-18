/**
 * ASR (Automatic Speech Recognition) 语音识别服务
 * 提供语音转文字的API接口封装
 */
import apiClient from './api';

/**
 * ASR API 基础路径
 */
const ASR_BASE_URL = '/v1/asr';

/**
 * ASR识别请求参数接口
 * @typedef {Object} TranscribeOptions
 * @property {string} [modelKey] - 使用的ASR模型key，格式：provider:model，默认：siliconflow:telespeech-asr
 * @property {string} [language] - 语言代码，如：zh（中文）、en（英文）、auto（自动识别）
 * @property {boolean} [enablePunctuation] - 是否启用标点符号，默认true
 * @property {boolean} [enableItn] - 是否启用逆文本正则化（数字、日期等），默认true
 */

/**
 * ASR识别结果接口
 * @typedef {Object} TranscribeResult
 * @property {string} text - 识别的文本内容
 * @property {number} transcriptionTimeMs - 识别耗时（毫秒）
 * @property {string} modelName - 使用的模型名称
 * @property {number} [audioDurationMs] - 音频时长（毫秒）
 * @property {Object} [rawResponse] - 原始API响应
 */

/**
 * 语音识别API服务类
 */
class AsrService {
  /**
   * 语音转文字
   * @param {File|Blob} audioFile - 音频文件对象
   * @param {TranscribeOptions} [options={}] - 可选配置参数
   * @returns {Promise<TranscribeResult>} 识别结果
   * @throws {Error} 当识别失败时抛出错误
   */
  async transcribe(audioFile, options = {}) {
    // 参数验证
    if (!audioFile) {
      throw new Error('音频文件不能为空');
    }

    // 文件大小验证（100MB限制）
    const maxSizeBytes = 100 * 1024 * 1024;
    if (audioFile.size > maxSizeBytes) {
      throw new Error(`音频文件过大，最大支持100MB，当前：${(audioFile.size / 1024 / 1024).toFixed(2)}MB`);
    }

    // 构建FormData
    const formData = new FormData();
    
    // 添加音频文件
    const filename = audioFile.name || `recording_${Date.now()}.webm`;
    formData.append('file', audioFile, filename);
    
    // 添加可选参数
    if (options.modelKey) {
      formData.append('modelKey', options.modelKey);
    }
    if (options.language) {
      formData.append('language', options.language);
    }
    if (typeof options.enablePunctuation === 'boolean') {
      formData.append('enablePunctuation', options.enablePunctuation.toString());
    }
    if (typeof options.enableItn === 'boolean') {
      formData.append('enableItn', options.enableItn.toString());
    }

    try {
      // 发送请求
      const response = await apiClient.post(
        `${ASR_BASE_URL}/transcribe`,
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
          // 设置较长的超时时间（60秒）
          timeout: 60000,
        }
      );

      // 响应验证
      if (!response.data || !response.data.success) {
        throw new Error(response.data?.message || '识别失败，服务器未返回有效数据');
      }

      const result = response.data.data;
      
      // 结果验证
      if (!result || !result.text) {
        throw new Error('识别结果为空');
      }

      return result;
    } catch (error) {
      // 错误处理和包装
      if (error.response) {
        // 服务器返回错误响应
        const message = error.response.data?.message || error.response.statusText || '识别失败';
        throw new Error(`ASR识别失败: ${message}`);
      } else if (error.request) {
        // 请求已发出但没有收到响应
        throw new Error('ASR服务无响应，请检查网络连接');
      } else {
        // 其他错误
        throw new Error(error.message || 'ASR识别失败');
      }
    }
  }

  /**
   * 获取可用的ASR模型列表
   * @returns {Promise<Array>} 模型列表
   */
  async getAvailableModels() {
    try {
      const response = await apiClient.get(`${ASR_BASE_URL}/models`);
      
      if (!response.data || !response.data.success) {
        throw new Error(response.data?.message || '获取模型列表失败');
      }

      return response.data.data || [];
    } catch (error) {
      console.error('获取ASR模型列表失败:', error);
      throw error;
    }
  }

  /**
   * 健康检查
   * @returns {Promise<Object>} 健康状态
   */
  async healthCheck(modelKey) {
    try {
      const response = await apiClient.get(`${ASR_BASE_URL}/health`, {
        params: { modelKey }
      });
      return response.data;
    } catch (error) {
      console.error('ASR健康检查失败:', error);
      throw error;
    }
  }
}

// 导出单例实例
export const asrService = new AsrService();

// 默认导出
export default asrService;

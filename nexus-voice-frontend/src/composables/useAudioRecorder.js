/**
 * 音频录音 Composable
 * 封装浏览器 MediaRecorder API，提供录音、停止、取消等功能
 * 支持浏览器兼容性检测、音频格式自动选择、时长限制等特性
 */
import { ref, onUnmounted } from 'vue';

/**
 * 录音状态枚举
 */
export const RecordingState = {
  IDLE: 'idle',           // 空闲状态
  RECORDING: 'recording', // 录音中
  PROCESSING: 'processing' // 处理中（识别/上传）
};

/**
 * 默认配置
 */
const DEFAULT_CONFIG = {
  maxDuration: 60,        // 最大录音时长（秒）
  channelCount: 1,        // 单声道
  sampleRate: 16000,      // 16kHz采样率
  echoCancellation: true, // 回声消除
  noiseSuppression: true, // 降噪
  autoGainControl: true,  // 自动增益
  timeslice: 100          // 数据收集间隔（毫秒）
};

/**
 * 音频录音 Composable
 * @param {Object} config - 配置选项
 * @param {number} [config.maxDuration=60] - 最大录音时长（秒）
 * @param {Function} [config.onRecordComplete] - 录音完成回调
 * @param {Function} [config.onError] - 错误回调
 * @returns {Object} 录音相关的响应式状态和方法
 */
export function useAudioRecorder(config = {}) {
  // 合并配置
  const options = { ...DEFAULT_CONFIG, ...config };

  // ==================== 响应式状态 ====================
  
  /**
   * 当前录音状态
   */
  const recordingState = ref(RecordingState.IDLE);

  /**
   * 当前录音时长（秒）
   */
  const recordingDuration = ref(0);

  /**
   * MediaRecorder 实例
   */
  const mediaRecorder = ref(null);

  /**
   * 音频数据块数组
   */
  const audioChunks = ref([]);

  /**
   * MediaStream 实例
   */
  const mediaStream = ref(null);

  /**
   * 录音定时器
   */
  let recordingTimer = null;

  // ==================== 浏览器兼容性检测 ====================

  /**
   * 检测浏览器是否支持录音
   * @returns {boolean} 是否支持
   */
  const isRecordingSupported = () => {
    return !!(
      navigator.mediaDevices &&
      navigator.mediaDevices.getUserMedia &&
      window.MediaRecorder
    );
  };

  /**
   * 获取浏览器支持的音频格式
   * 按优先级返回第一个支持的格式
   * @returns {string} MIME类型
   */
  const getSupportedAudioFormat = () => {
    const formats = [
      'audio/webm;codecs=opus',  // Chrome/Edge首选，文件小音质好
      'audio/webm',              // Firefox
      'audio/ogg;codecs=opus',   // 备选1
      'audio/mp4',               // Safari
      'audio/wav'                // 万能格式，但文件大
    ];

    for (const format of formats) {
      if (MediaRecorder.isTypeSupported(format)) {
        console.log(`[录音] 使用音频格式: ${format}`);
        return format;
      }
    }

    // 默认返回webm
    console.warn('[录音] 未找到明确支持的格式，使用默认 audio/webm');
    return 'audio/webm';
  };

  // ==================== 核心录音方法 ====================

  /**
   * 开始录音
   * @throws {Error} 当浏览器不支持或权限被拒绝时抛出错误
   */
  const startRecording = async () => {
    // 兼容性检查
    if (!isRecordingSupported()) {
      const error = new Error('您的浏览器不支持录音功能，请使用Chrome、Firefox或Edge浏览器');
      if (options.onError) {
        options.onError(error);
      }
      throw error;
    }

    // 防止重复启动
    if (recordingState.value === RecordingState.RECORDING) {
      console.warn('[录音] 已在录音中，忽略重复启动');
      return;
    }

    try {
      console.log('[录音] 开始请求麦克风权限...');

      // 请求麦克风权限
      const stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: options.channelCount,
          sampleRate: options.sampleRate,
          echoCancellation: options.echoCancellation,
          noiseSuppression: options.noiseSuppression,
          autoGainControl: options.autoGainControl
        }
      });

      mediaStream.value = stream;

      // 创建 MediaRecorder
      const mimeType = getSupportedAudioFormat();
      mediaRecorder.value = new MediaRecorder(stream, { mimeType });
      audioChunks.value = [];
      recordingDuration.value = 0;

      // 监听数据可用事件
      mediaRecorder.value.ondataavailable = (event) => {
        if (event.data && event.data.size > 0) {
          audioChunks.value.push(event.data);
        }
      };

      // 监听停止事件
      mediaRecorder.value.onstop = async () => {
        console.log(`[录音] 录音已停止，共收集 ${audioChunks.value.length} 个数据块`);

        // 创建音频Blob
        const audioBlob = new Blob(audioChunks.value, { type: mimeType });
        console.log(`[录音] 音频大小: ${(audioBlob.size / 1024).toFixed(2)} KB`);

        // 停止所有音轨
        stopMediaStream();

        // 触发完成回调
        if (options.onRecordComplete) {
          try {
            recordingState.value = RecordingState.PROCESSING;
            await options.onRecordComplete(audioBlob);
          } catch (error) {
            console.error('[录音] 处理录音失败:', error);
            if (options.onError) {
              options.onError(error);
            }
          } finally {
            recordingState.value = RecordingState.IDLE;
            recordingDuration.value = 0;
          }
        } else {
          recordingState.value = RecordingState.IDLE;
          recordingDuration.value = 0;
        }
      };

      // 监听错误事件
      mediaRecorder.value.onerror = (event) => {
        console.error('[录音] MediaRecorder错误:', event.error);
        const error = new Error(`录音失败: ${event.error?.message || '未知错误'}`);
        if (options.onError) {
          options.onError(error);
        }
        stopRecording();
      };

      // 开始录音
      mediaRecorder.value.start(options.timeslice);
      recordingState.value = RecordingState.RECORDING;
      console.log('[录音] 录音已开始');

      // 启动计时器
      startTimer();

    } catch (error) {
      console.error('[录音] 启动失败:', error);

      // 清理资源
      stopMediaStream();
      recordingState.value = RecordingState.IDLE;

      // 友好的错误提示
      let errorMessage = '录音启动失败';
      if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
        errorMessage = '麦克风权限被拒绝，请在浏览器设置中允许访问麦克风';
      } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
        errorMessage = '未找到麦克风设备，请检查设备连接';
      } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
        errorMessage = '麦克风被其他应用占用，请关闭其他正在使用麦克风的应用';
      } else if (error.name === 'OverconstrainedError' || error.name === 'ConstraintNotSatisfiedError') {
        errorMessage = '设备不满足录音要求';
      } else if (error.message) {
        errorMessage = error.message;
      }

      const wrappedError = new Error(errorMessage);
      if (options.onError) {
        options.onError(wrappedError);
      }
      throw wrappedError;
    }
  };

  /**
   * 停止录音
   */
  const stopRecording = () => {
    if (mediaRecorder.value && recordingState.value === RecordingState.RECORDING) {
      console.log('[录音] 停止录音...');
      mediaRecorder.value.stop();
      stopTimer();
    }
  };

  /**
   * 取消录音（不触发完成回调）
   */
  const cancelRecording = () => {
    if (recordingState.value === RecordingState.RECORDING) {
      console.log('[录音] 取消录音');
      stopTimer();
      stopMediaStream();
      
      // 重置状态
      audioChunks.value = [];
      recordingDuration.value = 0;
      recordingState.value = RecordingState.IDLE;
      mediaRecorder.value = null;
    }
  };

  // ==================== 辅助方法 ====================

  /**
   * 启动计时器
   */
  const startTimer = () => {
    stopTimer(); // 先清除旧的定时器
    recordingTimer = setInterval(() => {
      recordingDuration.value++;

      // 检查是否达到最大时长
      if (recordingDuration.value >= options.maxDuration) {
        console.warn(`[录音] 已达到最大时长 ${options.maxDuration} 秒，自动停止`);
        stopRecording();
      }
    }, 1000);
  };

  /**
   * 停止计时器
   */
  const stopTimer = () => {
    if (recordingTimer) {
      clearInterval(recordingTimer);
      recordingTimer = null;
    }
  };

  /**
   * 停止媒体流
   */
  const stopMediaStream = () => {
    if (mediaStream.value) {
      mediaStream.value.getTracks().forEach(track => {
        track.stop();
        console.log('[录音] 已停止音轨:', track.kind);
      });
      mediaStream.value = null;
    }
  };

  /**
   * 格式化录音时长
   * @param {number} seconds - 秒数
   * @returns {string} 格式化字符串，如 "1:23"
   */
  const formatDuration = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  // ==================== 生命周期清理 ====================

  /**
   * 组件卸载时清理资源
   */
  onUnmounted(() => {
    console.log('[录音] 组件卸载，清理资源');
    stopTimer();
    stopMediaStream();
    if (mediaRecorder.value) {
      mediaRecorder.value = null;
    }
  });

  // ==================== 返回值 ====================

  return {
    // 状态
    recordingState,
    recordingDuration,
    
    // 方法
    startRecording,
    stopRecording,
    cancelRecording,
    isRecordingSupported,
    formatDuration,
    
    // 常量
    RecordingState
  };
}

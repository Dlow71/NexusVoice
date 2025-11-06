/**
 * RTC服务 - WebRTC实时语音对话API
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */

import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

/**
 * 创建RTC会话
 */
export const createRtcSession = async (request = {}) => {
  const response = await axios.post(`${API_BASE_URL}/api/v1/rtc/session`, request)
  return response.data
}

/**
 * 获取会话信息
 */
export const getRtcSession = async (sessionId) => {
  const response = await axios.get(`${API_BASE_URL}/api/v1/rtc/session/${sessionId}`)
  return response.data
}

/**
 * 开始WebRTC连接
 */
export const startRtcConnection = async (sessionId) => {
  const response = await axios.post(`${API_BASE_URL}/api/v1/rtc/session/${sessionId}/connect`)
  return response.data
}

/**
 * 打断播放
 */
export const interruptRtc = async (sessionId, mode = 'SOFT', reason = 'user_barge_in') => {
  const response = await axios.post(`${API_BASE_URL}/api/v1/rtc/session/${sessionId}/interrupt`, {
    mode,
    reason
  })
  return response.data
}

/**
 * 结束会话
 */
export const endRtcSession = async (sessionId) => {
  const response = await axios.delete(`${API_BASE_URL}/api/v1/rtc/session/${sessionId}`)
  return response.data
}







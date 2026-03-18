import apiClient from './api'

export const startVoiceSession = async (payload = {}) => {
  const response = await apiClient.post('/v1/voice/sessions', payload)
  return response.data
}

export const getVoiceRuntimeConfig = async (voiceSessionId) => {
  const response = await apiClient.get(`/v1/voice/sessions/${voiceSessionId}/runtime-config`)
  return response.data
}

export const updateVoiceRuntimeConfig = async (voiceSessionId, payload = {}) => {
  const response = await apiClient.put(`/v1/voice/sessions/${voiceSessionId}/runtime-config`, payload)
  return response.data
}

export const interruptVoiceSession = async (voiceSessionId) => {
  const response = await apiClient.post(`/v1/voice/sessions/${voiceSessionId}/interrupt`)
  return response.data
}

export const endVoiceSession = async (voiceSessionId) => {
  const response = await apiClient.delete(`/v1/voice/sessions/${voiceSessionId}`)
  return response.data
}

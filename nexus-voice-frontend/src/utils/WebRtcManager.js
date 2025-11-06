/**
 * WebRTC连接管理器
 * 
 * 功能：
 * - 管理RTCPeerConnection
 * - 处理ICE候选者收集
 * - 麦克风采集与远端音频播放
 * - 连接状态监控
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */

export class WebRtcManager {
  constructor() {
    this.peerConnection = null
    this.localStream = null
    this.remoteAudio = null
    this.onIceCandidate = null
    this.onConnectionStateChange = null
    this.onTrack = null
    this.iceServers = [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' }
    ]
  }

  /**
   * 初始化WebRTC连接
   */
  async initialize() {
    console.log('[WebRTC] 初始化连接...')
    
    try {
      // 创建RTCPeerConnection
      this.peerConnection = new RTCPeerConnection({
        iceServers: this.iceServers,
        rtcpMuxPolicy: 'require', // 复用RTCP
        bundlePolicy: 'max-bundle' // 最大化bundle
      })

      // 监听ICE候选者
      this.peerConnection.onicecandidate = (event) => {
        if (event.candidate) {
          console.log('[WebRTC] ICE候选者:', event.candidate.candidate)
          if (this.onIceCandidate) {
            this.onIceCandidate(event.candidate)
          }
        } else {
          console.log('[WebRTC] ICE候选者收集完成')
        }
      }

      // 监听连接状态变化
      this.peerConnection.onconnectionstatechange = () => {
        const state = this.peerConnection.connectionState
        console.log('[WebRTC] 连接状态:', state)
        if (this.onConnectionStateChange) {
          this.onConnectionStateChange(state)
        }
      }

      // 监听ICE连接状态变化
      this.peerConnection.oniceconnectionstatechange = () => {
        const state = this.peerConnection.iceConnectionState
        console.log('[WebRTC] ICE连接状态:', state)
      }

      // 监听远端音频轨道
      this.peerConnection.ontrack = (event) => {
        console.log('[WebRTC] 收到远端音频轨道')
        if (this.remoteAudio) {
          this.remoteAudio.srcObject = event.streams[0]
          this.remoteAudio.play().catch(err => {
            console.error('[WebRTC] 播放远端音频失败:', err)
          })
        }
        if (this.onTrack) {
          this.onTrack(event)
        }
      }

      console.log('[WebRTC] 初始化完成')
      return true
    } catch (error) {
      console.error('[WebRTC] 初始化失败:', error)
      throw error
    }
  }

  /**
   * 获取本地音频流（麦克风）
   */
  async getLocalAudioStream() {
    console.log('[WebRTC] 请求麦克风权限...')
    
    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,      // 回声消除
          noiseSuppression: true,      // 降噪
          autoGainControl: false,      // AGC默认关闭（设备白名单策略）
          sampleRate: 48000,           // 优先48kHz
          channelCount: 1              // 单声道
        },
        video: false
      })

      console.log('[WebRTC] 麦克风已获取:', this.localStream.id)

      // 添加音频轨道到PeerConnection
      this.localStream.getTracks().forEach(track => {
        console.log('[WebRTC] 添加本地音频轨道:', track.label)
        this.peerConnection.addTrack(track, this.localStream)
      })

      return this.localStream
    } catch (error) {
      console.error('[WebRTC] 获取麦克风失败:', error)
      throw new Error('无法访问麦克风，请检查浏览器权限设置')
    }
  }

  /**
   * 设置远端音频元素
   */
  setRemoteAudioElement(audioElement) {
    this.remoteAudio = audioElement
    console.log('[WebRTC] 远端音频元素已设置')
  }

  /**
   * 创建SDP Offer
   */
  async createOffer() {
    console.log('[WebRTC] 创建SDP Offer...')
    
    try {
      const offer = await this.peerConnection.createOffer({
        offerToReceiveAudio: true, // 接收音频
        offerToReceiveVideo: false
      })

      await this.peerConnection.setLocalDescription(offer)
      console.log('[WebRTC] 本地SDP已设置')

      return offer.sdp
    } catch (error) {
      console.error('[WebRTC] 创建Offer失败:', error)
      throw error
    }
  }

  /**
   * 处理SDP Answer
   */
  async handleAnswer(sdp) {
    console.log('[WebRTC] 处理SDP Answer...')
    
    try {
      const answer = new RTCSessionDescription({
        type: 'answer',
        sdp: sdp
      })

      await this.peerConnection.setRemoteDescription(answer)
      console.log('[WebRTC] 远端SDP已设置')
      return true
    } catch (error) {
      console.error('[WebRTC] 设置Answer失败:', error)
      throw error
    }
  }

  /**
   * 添加ICE候选者
   */
  async addIceCandidate(candidate) {
    try {
      await this.peerConnection.addIceCandidate(new RTCIceCandidate(candidate))
      console.log('[WebRTC] ICE候选者已添加')
    } catch (error) {
      console.error('[WebRTC] 添加ICE候选者失败:', error)
    }
  }

  /**
   * 获取连接统计信息
   */
  async getStats() {
    if (!this.peerConnection) {
      return null
    }

    try {
      const stats = await this.peerConnection.getStats()
      const result = {
        rtt: 0,
        jitter: 0,
        packetLoss: 0,
        bitrate: 0
      }

      stats.forEach(report => {
        if (report.type === 'inbound-rtp' && report.kind === 'audio') {
          result.jitter = report.jitter || 0
          result.packetLoss = report.packetsLost || 0
        }
        if (report.type === 'candidate-pair' && report.state === 'succeeded') {
          result.rtt = report.currentRoundTripTime ? report.currentRoundTripTime * 1000 : 0
        }
      })

      return result
    } catch (error) {
      console.error('[WebRTC] 获取统计信息失败:', error)
      return null
    }
  }

  /**
   * 关闭连接
   */
  close() {
    console.log('[WebRTC] 关闭连接...')

    // 停止本地流
    if (this.localStream) {
      this.localStream.getTracks().forEach(track => {
        track.stop()
        console.log('[WebRTC] 停止音频轨道:', track.label)
      })
      this.localStream = null
    }

    // 关闭PeerConnection
    if (this.peerConnection) {
      this.peerConnection.close()
      this.peerConnection = null
      console.log('[WebRTC] PeerConnection已关闭')
    }

    // 停止远端音频
    if (this.remoteAudio) {
      this.remoteAudio.srcObject = null
      this.remoteAudio = null
    }
  }

  /**
   * 检查浏览器是否支持WebRTC
   */
  static isSupported() {
    return !!(
      navigator.mediaDevices &&
      navigator.mediaDevices.getUserMedia &&
      window.RTCPeerConnection
    )
  }
}







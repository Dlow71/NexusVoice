/**
 * RTC信令WebSocket客户端
 * 
 * 功能：
 * - 管理WebSocket连接
 * - 处理信令消息（OFFER/ANSWER/ICE_CANDIDATE/INTERRUPT等）
 * - 消息序列号管理
 * - 幂等性保护
 * - 心跳保活
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */

export class RtcSignalingClient {
  constructor(url, token) {
    this.url = url
    this.token = token
    this.ws = null
    this.sessionId = null
    this.sequence = 0
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.heartbeatInterval = null
    this.connected = false

    // 回调函数
    this.onConnected = null
    this.onAnswer = null
    this.onIceCandidateAck = null
    this.onInterruptAck = null
    this.onStateUpdate = null
    this.onError = null
    this.onDisconnected = null
  }

  /**
   * 连接信令WebSocket
   */
  connect() {
    return new Promise((resolve, reject) => {
      console.log('[Signaling] 连接WebSocket:', this.url)

      try {
        // 添加token到URL
        const wsUrl = `${this.url}?token=${this.token}`
        this.ws = new WebSocket(wsUrl)

        this.ws.onopen = () => {
          console.log('[Signaling] WebSocket连接成功')
          this.connected = true
          this.reconnectAttempts = 0
          this.startHeartbeat()
          resolve()
        }

        this.ws.onmessage = (event) => {
          try {
            const message = JSON.parse(event.data)
            this.handleMessage(message)
          } catch (error) {
            console.error('[Signaling] 解析消息失败:', error)
          }
        }

        this.ws.onerror = (error) => {
          console.error('[Signaling] WebSocket错误:', error)
          reject(error)
        }

        this.ws.onclose = (event) => {
          console.log('[Signaling] WebSocket连接关闭:', event.code, event.reason)
          this.connected = false
          this.stopHeartbeat()

          if (this.onDisconnected) {
            this.onDisconnected(event)
          }

          // 自动重连（非正常关闭）
          if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnect()
          }
        }

      } catch (error) {
        console.error('[Signaling] 创建WebSocket失败:', error)
        reject(error)
      }
    })
  }

  /**
   * 处理收到的信令消息
   */
  handleMessage(message) {
    console.log('[Signaling] 收到消息:', message.type, message)

    switch (message.type) {
      case 'CONNECTED':
        if (this.onConnected) {
          this.onConnected(message)
        }
        break

      case 'ANSWER':
        if (this.onAnswer) {
          this.onAnswer(message.sdp, message)
        }
        break

      case 'ICE_CANDIDATE_ACK':
        if (this.onIceCandidateAck) {
          this.onIceCandidateAck(message)
        }
        break

      case 'INTERRUPT_ACK':
        if (this.onInterruptAck) {
          this.onInterruptAck(message)
        }
        break

      case 'STATE_UPDATE':
        if (this.onStateUpdate) {
          this.onStateUpdate(message.state, message)
        }
        break

      case 'ERROR':
        console.error('[Signaling] 服务器错误:', message.message)
        if (this.onError) {
          this.onError(message.message, message)
        }
        break

      case 'HEARTBEAT_ACK':
        // 心跳应答，仅记录
        console.debug('[Signaling] 心跳应答')
        break

      default:
        console.warn('[Signaling] 未知消息类型:', message.type)
    }
  }

  /**
   * 发送SDP Offer
   */
  sendOffer(sessionId, sdp) {
    const message = {
      type: 'OFFER',
      sessionId: sessionId,
      seq: ++this.sequence,
      ts: Date.now(),
      idempotencyKey: `offer-${this.sequence}`,
      sdp: sdp
    }

    this.send(message)
    console.log('[Signaling] SDP Offer已发送, seq:', this.sequence)
  }

  /**
   * 发送ICE候选者
   */
  sendIceCandidate(sessionId, candidate) {
    const message = {
      type: 'ICE_CANDIDATE',
      sessionId: sessionId,
      seq: ++this.sequence,
      ts: Date.now(),
      idempotencyKey: `ice-${this.sequence}`,
      candidate: {
        candidate: candidate.candidate,
        sdpMid: candidate.sdpMid,
        sdpMLineIndex: candidate.sdpMLineIndex
      }
    }

    this.send(message)
    console.debug('[Signaling] ICE候选者已发送, seq:', this.sequence)
  }

  /**
   * 发送打断信号
   */
  sendInterrupt(sessionId, mode = 'SOFT', reason = 'user_click') {
    const message = {
      type: 'INTERRUPT',
      sessionId: sessionId,
      seq: ++this.sequence,
      ts: Date.now(),
      idempotencyKey: `interrupt-${this.sequence}`,
      mode: mode,
      reason: reason
    }

    this.send(message)
    console.log('[Signaling] 打断信号已发送, mode:', mode)
  }

  /**
   * 发送心跳
   */
  sendHeartbeat() {
    if (!this.connected) {
      return
    }

    const message = {
      type: 'HEARTBEAT',
      seq: ++this.sequence,
      ts: Date.now()
    }

    this.send(message)
  }

  /**
   * 发送消息到WebSocket
   */
  send(message) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.warn('[Signaling] WebSocket未连接，无法发送消息')
      return
    }

    try {
      this.ws.send(JSON.stringify(message))
    } catch (error) {
      console.error('[Signaling] 发送消息失败:', error)
    }
  }

  /**
   * 启动心跳定时器（每5秒）
   */
  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      this.sendHeartbeat()
    }, 5000)
    console.log('[Signaling] 心跳定时器已启动')
  }

  /**
   * 停止心跳定时器
   */
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
      console.log('[Signaling] 心跳定时器已停止')
    }
  }

  /**
   * 重连
   */
  reconnect() {
    this.reconnectAttempts++
    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 30000) // 指数退避，最大30秒

    console.log(`[Signaling] ${delay}ms后尝试第${this.reconnectAttempts}次重连...`)

    setTimeout(() => {
      this.connect().catch(err => {
        console.error('[Signaling] 重连失败:', err)
      })
    }, delay)
  }

  /**
   * 断开连接
   */
  disconnect() {
    console.log('[Signaling] 断开WebSocket连接...')

    this.stopHeartbeat()

    if (this.ws) {
      this.ws.close(1000, '用户主动断开')
      this.ws = null
    }

    this.connected = false
    this.sequence = 0
  }

  /**
   * 获取连接状态
   */
  isConnected() {
    return this.connected && this.ws && this.ws.readyState === WebSocket.OPEN
  }
}







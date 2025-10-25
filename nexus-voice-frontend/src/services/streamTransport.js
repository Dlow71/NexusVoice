/**
 * 流式传输协议抽象层
 * 
 * 设计目标：
 * - 提供统一的流式通信接口
 * - 支持WebSocket和SSE两种协议
 * - 易于扩展新协议（如gRPC Stream）
 * - 完整的生命周期管理
 * - 详细的日志追踪
 * 
 * @author NexusVoice
 * @since 2025-10-25
 */

/**
 * 流式传输协议抽象基类
 * 定义了所有传输协议必须实现的接口
 */
export class StreamTransport {
  constructor() {
    if (new.target === StreamTransport) {
      throw new Error('StreamTransport是抽象类，不能直接实例化');
    }
    
    // 状态管理
    this.status = 'disconnected'; // disconnected, connecting, connected
    
    // 事件回调（由使用者设置）
    this.onStatusChange = null;  // (status: string) => void
    this.onMessage = null;       // (data: object) => void
    this.onError = null;         // (error: Error) => void
  }
  
  /**
   * 连接到服务器
   * @param {string} token - JWT认证令牌
   * @returns {Promise<void>}
   */
  async connect(token) {
    throw new Error('子类必须实现connect方法');
  }
  
  /**
   * 发送消息
   * @param {object} data - 消息数据
   * @returns {Promise<void>}
   */
  async send(data) {
    throw new Error('子类必须实现send方法');
  }
  
  /**
   * 关闭连接
   */
  close() {
    throw new Error('子类必须实现close方法');
  }
  
  /**
   * 检查是否已连接
   * @returns {boolean}
   */
  isConnected() {
    return this.status === 'connected';
  }
  
  /**
   * 获取协议类型
   * @returns {string}
   */
  getProtocolType() {
    throw new Error('子类必须实现getProtocolType方法');
  }
  
  /**
   * 触发状态变更事件
   * @protected
   */
  _emitStatusChange(status) {
    this.status = status;
    if (this.onStatusChange) {
      this.onStatusChange(status);
    }
  }
  
  /**
   * 触发消息事件
   * @protected
   */
  _emitMessage(data) {
    if (this.onMessage) {
      this.onMessage(data);
    }
  }
  
  /**
   * 触发错误事件
   * @protected
   */
  _emitError(error) {
    if (this.onError) {
      this.onError(error);
    }
  }
}

/**
 * WebSocket传输实现
 * 双向实时通信，适合需要客户端主动发送消息的场景
 */
export class WebSocketTransport extends StreamTransport {
  constructor() {
    super();
    this.ws = null;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 3;
    this.reconnectTimeouts = [1000, 2000, 4000]; // 指数退避
    this.token = null;
  }
  
  getProtocolType() {
    return 'websocket';
  }
  
  async connect(token) {
    console.log('[WebSocket] 🔌 开始连接...');
    
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('[WebSocket] ✅ 已连接，跳过重复连接');
      return;
    }
    
    this.token = token;
    this._emitStatusChange('connecting');
    
    try {
      // 使用子协议传递token（更安全）
      const wsUrl = `ws://localhost:8081/ws/chat/stream`;
      const protocol = `Bearer.${token}`;
      
      this.ws = new WebSocket(wsUrl, protocol);
      
      // 连接成功
      this.ws.onopen = () => {
        console.log('[WebSocket] ✅ 连接已建立');
        this._emitStatusChange('connected');
        this.reconnectAttempts = 0;
      };
      
      // 接收消息
      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log(`[WebSocket] 📨 收到消息: ${data.type}`);
          this._emitMessage(data);
        } catch (error) {
          console.error('[WebSocket] ❌ 解析消息失败:', error);
          this._emitError(new Error('消息解析失败'));
        }
      };
      
      // 连接错误
      this.ws.onerror = (error) => {
        console.error('[WebSocket] ❌ 连接错误:', error);
        this._emitError(new Error('WebSocket连接错误'));
      };
      
      // 连接关闭
      this.ws.onclose = (event) => {
        console.log(`[WebSocket] 🔌 连接已关闭, code: ${event.code}`);
        this._emitStatusChange('disconnected');
        
        // 认证失败，不重连
        if (event.code === 1008 || event.code === 1003) {
          this._emitError(new Error('Token认证失败，请重新登录'));
          return;
        }
        
        // 自动重连
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
          const timeout = this.reconnectTimeouts[this.reconnectAttempts] || 5000;
          console.log(`[WebSocket] 🔄 ${timeout/1000}秒后尝试第${this.reconnectAttempts + 1}次重连...`);
          
          setTimeout(() => {
            this.reconnectAttempts++;
            this.connect(this.token);
          }, timeout);
        } else {
          console.error('[WebSocket] ❌ 达到最大重连次数，停止重连');
          this._emitError(new Error('连接失败，请检查网络'));
        }
      };
    } catch (error) {
      console.error('[WebSocket] ❌ 创建连接失败:', error);
      this._emitStatusChange('disconnected');
      this._emitError(error);
    }
  }
  
  async send(data) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      throw new Error('WebSocket未连接');
    }
    
    console.log('[WebSocket] 📤 发送消息', {
      conversationId: data.conversationId,
      hasImages: !!data.imageUrls,
      imageCount: data.imageUrls?.length || 0
    });
    
    this.ws.send(JSON.stringify(data));
  }
  
  close() {
    console.log('[WebSocket] 🔌 关闭连接');
    
    if (this.ws) {
      // 停止自动重连
      this.maxReconnectAttempts = 0;
      
      if (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING) {
        this.ws.close();
      }
      this.ws = null;
    }
    
    this._emitStatusChange('disconnected');
  }
}

/**
 * SSE (Server-Sent Events) 传输实现
 * 单向流式推送，轻量级，适合纯流式输出场景
 */
export class SseTransport extends StreamTransport {
  constructor() {
    super();
    this.controller = null; // AbortController用于取消请求
    this.token = null;
    this.currentReader = null; // 当前的流读取器
  }
  
  getProtocolType() {
    return 'sse';
  }
  
  async connect(token) {
    console.log('[SSE] 🔌 准备连接（SSE按需建立连接）');
    this.token = token;
    this._emitStatusChange('connected');
  }
  
  async send(data) {
    console.log('[SSE] 📤 发送POST请求', {
      conversationId: data.conversationId,
      hasImages: !!data.imageUrls,
      imageCount: data.imageUrls?.length || 0
    });
    
    // 创建AbortController用于取消请求
    this.controller = new AbortController();
    
    try {
      const response = await fetch('http://localhost:8081/api/v1/conversations/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.token}`
        },
        body: JSON.stringify(data),
        signal: this.controller.signal
      });
      
      if (!response.ok) {
        throw new Error(`HTTP错误: ${response.status} ${response.statusText}`);
      }
      
      console.log('[SSE] ✅ 连接已建立，开始读取流');
      
      // 读取SSE流
      await this._readSseStream(response.body);
      
      console.log('[SSE] ✅ 流读取完成');
    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('[SSE] 🛑 请求已取消');
      } else {
        console.error('[SSE] ❌ 请求失败:', error);
        this._emitError(error);
      }
    } finally {
      this.controller = null;
      this.currentReader = null;
    }
  }
  
  /**
   * 读取SSE流
   * @private
   */
  async _readSseStream(body) {
    const reader = body.getReader();
    this.currentReader = reader;
    const decoder = new TextDecoder();
    
    let buffer = ''; // 用于累积不完整的消息
    let currentEvent = null; // 当前事件名称
    let chunkCount = 0; // 接收到的数据块计数
    
    try {
      while (true) {
        const { done, value } = await reader.read();
        
        if (done) {
          console.log('[SSE] 📭 流已结束，共接收', chunkCount, '个数据块');
          break;
        }
        
        chunkCount++;
        
        // 解码数据
        const chunk = decoder.decode(value, { stream: true });
        console.log(`[SSE] 📦 接收数据块 #${chunkCount}:`, chunk.substring(0, 200) + (chunk.length > 200 ? '...' : ''));
        buffer += chunk;
        
        // 按双换行符分割消息（SSE标准格式）
        const messages = buffer.split('\n\n');
        
        // 最后一个可能是不完整的，保留到buffer中
        buffer = messages.pop() || '';
        
        // 处理完整的消息
        for (const message of messages) {
          if (!message.trim()) continue;
          
          // 解析SSE消息（支持event和data字段）
          const lines = message.split('\n');
          let eventData = null;
          
          for (const line of lines) {
            // 支持 "event:" 和 "event: " 两种格式
            if (line.startsWith('event:')) {
              currentEvent = line.slice(6).trim(); // 移除 "event:" 前缀并去除空格
              console.log('[SSE] 📋 事件类型:', currentEvent);
            } 
            // 支持 "data:" 和 "data: " 两种格式
            else if (line.startsWith('data:')) {
              const jsonStr = line.slice(5).trim(); // 移除 "data:" 前缀并去除空格
              
              // 跳过空data行
              if (!jsonStr) continue;
              
              // 解析JSON
              try {
                eventData = JSON.parse(jsonStr);
              } catch (error) {
                console.error('[SSE] ❌ 解析JSON失败:', error);
                console.error('[SSE] 问题行:', line);
              }
            }
          }
          
          // 如果有数据，触发消息事件
          if (eventData) {
            console.log(`[SSE] 📨 收到消息: ${eventData.type}, delta: ${eventData.delta?.substring(0, 50) || 'null'}`);
            this._emitMessage(eventData);
          }
        }
      }
    } catch (error) {
      if (error.name === 'AbortError') {
        console.log('[SSE] 🛑 流读取已取消');
      } else {
        console.error('[SSE] ❌ 读取流失败:', error);
        throw error;
      }
    } finally {
      reader.releaseLock();
    }
  }
  
  close() {
    console.log('[SSE] 🔌 关闭连接');
    
    // 取消当前请求
    if (this.controller) {
      this.controller.abort();
      this.controller = null;
    }
    
    // 释放流读取器
    if (this.currentReader) {
      try {
        this.currentReader.cancel();
      } catch (error) {
        console.warn('[SSE] 释放读取器失败:', error);
      }
      this.currentReader = null;
    }
    
    this._emitStatusChange('disconnected');
  }
}

/**
 * 创建传输实例的工厂函数
 * @param {string} protocol - 协议类型 ('websocket' | 'sse')
 * @returns {StreamTransport}
 */
export function createTransport(protocol) {
  console.log(`[TransportFactory] 🏭 创建 ${protocol} 传输实例`);
  
  switch (protocol) {
    case 'websocket':
      return new WebSocketTransport();
    case 'sse':
      return new SseTransport();
    default:
      throw new Error(`不支持的协议类型: ${protocol}`);
  }
}

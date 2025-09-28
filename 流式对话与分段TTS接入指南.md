# NexusVoice WebSocket 流式对话与分段TTS接入指南

本文面向前后端同学，用大白话介绍当前 WebSocket 流式对话与“分段TTS（文本转语音）”的整体实现思路、前端接入方式、消息协议与参数配置，帮助你快速对接并获得更好的首帧与播放体验。

---

## 1. 整体思路（大白话）

- 连接与鉴权：前端通过 `ws://<host>/ws/chat/stream` 建立 WebSocket 连接，握手时带上 JWT（请求头 Authorization: Bearer，或 URL 上 `?token=...`/`?access_token=...`）。握手拦截器会校验并把 `userId/username/roles` 塞进会话。

- 模型开始：连接建立会有一条“欢迎 START”，模型真正开始生成时会再发一条“模型 START”。你可以把第一个当作服务器状态，第二个当作模型开始信号。

- 分段TTS的核心：
  - 目标是“文字和音频同时出现”，先攒一段“够完整”的文本作为首段，立刻并发生成该段音频；首段必须“音频就绪”或“等待超时”后才一起发出去，这样首屏直接是“文本+音频”的同步首帧。
  - 后续继续按句子边界+长度阈值切段，每段文本生成时就并行跑TTS，等音频到位就按序发“文本+音频段”。
  - 如果某段音频慢了，我们先发文本，音频来了再补一条 `TTS_SEGMENT_UPDATE`（只带音频不带文本），前端更新当前段的音频即可。

- 统一收尾：整段文本累计完毕后，写入数据库，再统一发一条 `END`，里面有 `conversationId/messageId/model/responseTimeMs` 等收尾信息。对前端来说，`END` 就是“这一轮完事了”。

- 与 HTTP 的一致性：
  - 历史裁剪（避免上下文过长）与 HTTP 一致：按“约 2500 tokens 预算 + 最多 20 条历史”策略。
  - 联网搜索开关：只有 `enableWebSearch=true` 才联网，默认 false。
  - TTS：只有 `enableAudio=true` 才启用分段TTS，默认 false（不开就走纯文字逐 token）。

---

## 2. 前端快速对接

### 2.1 连接地址与鉴权

- 连接地址：`ws://<host>:<port>/ws/chat/stream`
- 鉴权方式（任选其一）：
  - 请求头：`Authorization: Bearer <jwt>`
  - 查询参数：`?token=<jwt>` 或 `?access_token=<jwt>`

可通过接口获取测试信息：`GET /api/v1/test/websocket/info`

### 2.2 发送消息（请求体）

发送 JSON，字段与 HTTP 的 `ChatRequestDto` 一致：

```json
{
  "conversationId": null,
  "message": "你好，能用中文介绍一下你自己吗？",
  "modelName": "gpt-4o-mini",
  "temperature": 0.7,
  "maxTokens": 2000,
  "title": "新对话",
  "systemPrompt": "你是一个有用的AI助手",
  "roleId": null,
  "enableWebSearch": false,
  "enableAudio": true
}
```

- 说明：把 `enableAudio` 设为 `true` 就会走“分段TTS模式”。否则会收到传统的逐 token 文本 `CONTENT`。

### 2.3 接收消息（事件流）

可能收到的事件类型：

- `START`：
  - 两种：连接层欢迎 START（连上就来一条）和模型层 START（模型开始生成时）。
- `TTS_SEGMENT`（分段TTS主力）：
  - 同时带“该段文本（delta）+ 该段音频（audioUrl）+ 分段组ID（ttsGroupId）+ 段序号（index）”。
- `TTS_SEGMENT_UPDATE`（迟到音频补发）：
  - 只带“该段音频（audioUrl）+ 组ID + 段序号”，文本不重复发送。
- `CONTENT`（纯文字增量片段）：
  - 仅在 `enableAudio=false` 时出现。
- `HEARTBEAT`：
  - 首段等待音频期间，按间隔发心跳，让前端知道连接正常。
- `ERROR`：
  - 异常时返回错误说明。
- `END`：
  - 本轮完成，包含 `conversationId/messageId/model/responseTimeMs`，在分段模式下还会带 `ttsGroupId` 与 `ttsChunked=true`。

### 2.4 前端播放队列（推荐思路）

按 `ttsGroupId` 把同一轮回答聚合，按 `index` 顺序播放：

伪码：

```ts
type Segment = { index: number; text?: string; audioUrl?: string };

const groups = new Map<string, Map<number, Segment>>();

function onEvent(evt) {
  switch (evt.type) {
    case 'TTS_SEGMENT': {
      const g = getOrCreate(evt.ttsGroupId);
      g.set(evt.index, { index: evt.index, text: evt.delta, audioUrl: evt.audioUrl });
      appendText(evt.delta); // 把文本拼到聊天窗口
      tryStartOrContinuePlayback(evt.ttsGroupId);
      break;
    }
    case 'TTS_SEGMENT_UPDATE': {
      const g = getOrCreate(evt.ttsGroupId);
      const seg = g.get(evt.index) || { index: evt.index };
      seg.audioUrl = evt.audioUrl;
      g.set(evt.index, seg);
      tryStartOrContinuePlayback(evt.ttsGroupId);
      break;
    }
    case 'CONTENT': {
      // 仅在 enableAudio=false 情况下，直接拼文本到聊天窗口即可
      appendText(evt.delta);
      break;
    }
    case 'END': {
      // 一轮完成，收尾
      finishConversation(evt.conversationId);
      break;
    }
  }
}

function tryStartOrContinuePlayback(groupId: string) {
  const g = groups.get(groupId)!; // Map<index, Segment>
  // 维护一个当前播放指针 currentIndex
  while (g.has(currentIndex)) {
    const seg = g.get(currentIndex)!;
    if (!seg.audioUrl) break; // 没有音频就先不播（或播静音）
    enqueueAndPlay(seg.audioUrl); // 入队播放；建议做极短淡入/淡出衔接
    currentIndex++;
  }
}
```

建议：

- 做极短淡入淡出（5–20ms）衔接段间音频，消除缝隙。
- 可以预加载下一个 `audioUrl`，降低段间等待。
- 若 `audioUrl` 迟到，会收到 `TTS_SEGMENT_UPDATE`，更新对应段后重新尝试 `tryStartOrContinuePlayback`。

---

## 3. 消息协议（事件定义）

以下为常见事件的字段示例（实际可能还有其他字段，根据实现演进有所扩展）：

### 3.1 START

```json
{
  "type": "START",
  "id": "stream_172...",
  "model": "gpt-4o-mini",
  "isEnd": false
}
```

### 3.2 TTS_SEGMENT（分段文本+音频）

```json
{
  "type": "TTS_SEGMENT",
  "ttsGroupId": "a2c7-...-9f1",
  "index": 0,
  "delta": "这是首段文本……",
  "audioUrl": "https://cdn.example.com/tts/seg0.mp3",
  "model": "gpt-4o-mini",
  "ttsChunked": true,
  "isEnd": false
}
```

### 3.3 TTS_SEGMENT_UPDATE（迟到音频补发）

```json
{
  "type": "TTS_SEGMENT_UPDATE",
  "ttsGroupId": "a2c7-...-9f1",
  "index": 0,
  "audioUrl": "https://cdn.example.com/tts/seg0.mp3",
  "model": "gpt-4o-mini",
  "ttsChunked": true,
  "isEnd": false
}
```

### 3.4 CONTENT（纯文字增量）

```json
{
  "type": "CONTENT",
  "delta": "逐token文本...",
  "index": 12,
  "isEnd": false
}
```

> 仅在 `enableAudio=false` 情况下会收到 CONTENT。开启音频后，就以 TTS_SEGMENT 为主。

### 3.5 HEARTBEAT

```json
{ "type": "HEARTBEAT", "isEnd": false }
```

### 3.6 ERROR

```json
{
  "type": "ERROR",
  "errorMessage": "AI响应出错：xxx",
  "isEnd": true
}
```

### 3.7 END（统一收尾）

```json
{
  "type": "END",
  "finishReason": "stop",
  "conversationId": 123,
  "messageId": 456,
  "model": "gpt-4o-mini",
  "responseTimeMs": 1420,
  "ttsGroupId": "a2c7-...-9f1",
  "ttsChunked": true,
  "isEnd": true
}
```

---

## 4. 请求体字段（ChatRequestDto）

- `conversationId`：继续已有对话，否则为空创建新对话。
- `message`：用户输入（必填）。
- `modelName`：模型名，默认 `gpt-4o-mini`。
- `temperature`：随机度，默认 `0.7`。
- `maxTokens`：最大回复token，默认 `2000`。
- `systemPrompt`：系统提示词（可选）。
- `title`：新对话标题（可选，默认“新对话”）。
- `roleId`：角色扮演ID（可选）。
- `enableWebSearch`：是否启用联网搜索，默认 `false`。
- `enableAudio`：是否启用分段TTS，默认 `false`（设为 `true` 才会走分段音频）。

---

## 5. 可配置项（SystemConfig）

以下通过数据库 `system_config` 表读取，读不到走默认值：

- `websocket.tts.segment.first_min_chars`（int，默认 300）
  - 首段最小字符数（首段更长，保证首帧“文字+音频”同步开播）。
- `websocket.tts.segment.min_chars`（int，默认 160）
  - 后续段的最小字符数。
- `websocket.tts.segment.max_chars`（int，默认 220）
  - 后续段的最大字符数（超过会硬切）。
- `websocket.tts.first_gate_ms`（int，默认 1500）
  - 首段同步门时长，超时则先发文本，音频稍后通过 `TTS_SEGMENT_UPDATE` 补发。
- `websocket.tts.max_concurrency`（int，默认 2）
  - TTS最大并发，控制外部服务压力与成本。
- `websocket.stream.heartbeat.ms`（int，默认 5000）
  - 首段等待期间的心跳间隔。
- `websocket.tts.update_on_late_audio`（bool，默认 true）
  - 是否启用“迟到音频补发”。

> 注：分段参数只在 `enableAudio=true` 时生效；HTTP 接口不受影响。

---

## 6. 典型时序

1) 连接 → 欢迎 `START`

2) 发送 ChatRequestDto（`enableAudio=true`）

3) 模型 `START`（可视为“模型开始生成”）

4) 首段等待期间：收到若干 `HEARTBEAT`

5) 收到首个 `TTS_SEGMENT(index=0)`，内含“首段文本+音频”（若音频超时，也可能先收到首段文本段，随后 `TTS_SEGMENT_UPDATE` 补发音频）

6) 后续 `TTS_SEGMENT(index=1,2,...)` 按序到达；偶有 `TTS_SEGMENT_UPDATE` 用于补发迟到音频

7) `END` 收尾

---

## 7. 测试与排查

- 获取测试说明：`GET /api/v1/test/websocket/info`
- 工具：Postman（支持WS）、wscat、浏览器开发者工具（WS面板）。
- 观察日志：检查是否有 `分段TTS失败`、`发送TTS_SEGMENT失败`、`AI响应出错` 等字样。
- 常见问题：
  - 只看到 `CONTENT` 而没有 `TTS_SEGMENT`：检查请求里 `enableAudio` 是否为 `true`。
  - 首屏太慢：可适当降低 `first_min_chars` 或增大 `first_gate_ms`；也可在前端先展示加载动画，等待首段。
  - 段间有缝：前端做极短淡入淡出，或预加载下一段音频。

---

## 8. 兼容性与回退

- 不改前端也能用：`enableAudio=false` 时仍是传统逐 token 文本流（`CONTENT`），行为与原来一致。
- 新事件向后兼容：不了解 `TTS_SEGMENT(_UPDATE)` 的老前端可以忽略它们，仍可依赖 `END` 收尾。

---

## 9. 参考文件（后端实现关键位置）

- WebSocket 处理器（分段TTS主逻辑）：
  - `nexusvoice-backend/src/main/java/com/nexusvoice/interfaces/websocket/ChatStreamHandler.java`
- 流式事件模型（新增 `TTS_SEGMENT`、`TTS_SEGMENT_UPDATE`）：
  - `nexusvoice-backend/src/main/java/com/nexusvoice/infrastructure/ai/model/StreamChatResponse.java`
- 测试说明接口（含响应示例）：
  - `nexusvoice-backend/src/main/java/com/nexusvoice/interfaces/api/test/WebSocketTestController.java`

---

如需我再补一份“前端播放队列的更详细代码示例（含音频预加载与淡入淡出）”，可以直接告诉我使用的框架（React/Vue/Vanilla）和播放器方案（Audio/MSE/第三方库），我会给出针对性的最佳实践示例。


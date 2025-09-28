# 角色助手（Role Assistant）API 与前后端对接指南

本文档汇总“生成角色草稿 → 预览深研任务清单 → 确认创建角色”的整体流程、接口定义、业务语义与前端对接说明。

## 一、总体流程

1) 生成角色草稿（快速、不创建）
- 从指定对话读取最近的上下文消息（做数量与长度裁剪），调用模型产出“角色草稿”JSON（只存结论，不存思维链）。
- 草稿会写回该对话作为一条系统消息，`metadata.type=ROLE_BRIEF`，便于追溯。

2) 预览深研任务清单（不搜索、不创建）
- 基于最近一次 `ROLE_BRIEF` 草稿，给出建议的搜索查询任务列表（查询语句 + 任务理由），供前端删改。

3) 确认创建角色（可选深研）
- 当用户确认：
  - 快速模式：直接用最近草稿创建私人角色。
  - 深研模式（可选）：按用户编辑后的查询集合执行检索与优化，再创建。
- 创建成功后，如草稿含有 `greetingMessage`，后端将自动执行 TTS，上传至七牛云CDN，并将音频URL回写到 `greetingAudioUrl`。

可选扩展：应用深研并更新草稿（不创建）
- 允许前端传入编辑后的查询集合执行深研，仅更新草稿（写入新的 `ROLE_BRIEF`），用户可继续迭代后再创建。

---

## 二、接口定义

所有接口均需要用户登录（@RequireUser），请在请求头携带有效的 JWT：

```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### 1) 生成角色草稿
- 方法与路径：`POST /api/roles/assistant/brief?conversationId={id}&enableWebSearch=false`
- 说明：从对话读取最近上下文，生成结构化“角色草稿”并写回对话（系统消息 `ROLE_BRIEF`）。
- 请求参数：
  - `conversationId` Long 必填：对话ID
  - `enableWebSearch` Boolean 选填：是否允许联网搜索（默认 false）
- 响应体：`RoleBriefDto`

示例响应（节选）：
```json
{
  "name": "霍格沃茨年轻魔法研究员",
  "description": "聪明好奇，擅长将复杂知识讲得通俗易懂",
  "personaPrompt": "说话温和、条理清晰，喜欢用类比解释概念，避免剧透",
  "greetingMessage": "你好，我是你的魔法学习伙伴，我们从基础开始",
  "avatarUrl": null,
  "voiceType": "qiniu_zh_female_dmytwz",
  "sources": [
    {"title": "…", "url": "…", "snippet": "…"}
  ],
  "disclaimers": ["本角色仅为原创风格设定，不复刻具体IP"]
}
```

相关代码：
- 控制器：`nexusvoice-backend/src/main/java/com/nexusvoice/interfaces/api/role/RoleAssistantController.java:1`
- 服务：`nexusvoice-backend/src/main/java/com/nexusvoice/application/role/service/RoleAssistantService.java:1`

### 2) 预览深研任务清单
- 方法与路径：`GET /api/roles/assistant/research/tasks?conversationId={id}`
- 说明：基于最近一次草稿，生成建议的搜索任务列表（前端可删改）。
- 请求参数：
  - `conversationId` Long 必填：对话ID
- 响应体：`RoleResearchTaskPreviewDto`

示例响应：
```json
{
  "tasks": [
    {"id": "task-1", "query": "<风格 特点 写作 口吻 示例>", "rationale": "补充风格与口吻示例", "enabled": true},
    {"id": "task-2", "query": "<领域 知识 点 概要>", "rationale": "补充领域知识点", "enabled": true}
  ],
  "defaultLimit": 12,
  "maxLimit": 20
}
```

相关代码：
- 控制器：`nexusvoice-backend/src/main/java/com/nexusvoice/interfaces/api/role/RoleAssistantController.java:1`
- 服务：`nexusvoice-backend/src/main/java/com/nexusvoice/application/role/service/RoleAssistantService.java:1`

### 3) 确认创建角色
- 方法与路径：`POST /api/roles/assistant/confirm`
- 说明：使用最近一次 `ROLE_BRIEF` 草稿创建私人角色；可选开启深研并传自定义查询集合。创建成功后自动处理 greeting 的TTS并上传CDN。
- 请求体：
```json
{
  "conversationId": 123,
  "deepResearch": false,
  "researchLimit": 12,
  "voiceType": "qiniu_zh_female_dmytwz",         // 前端直传音色（优先级最高，可选）
  "overrideVoiceType": null,                      // 兼容旧字段（将废弃，不推荐）
  "overrideName": null,                           // 可选改名
  "researchQueries": ["自定义查询1", "自定义查询2"] // 深研模式下可替换默认任务
}
```
- 音色优先级：`voiceType（前端直传） > overrideVoiceType（兼容旧） > brief.voiceType > 默认 qiniu_zh_female_dmytwz`
- 响应体：`RoleDTO`（包含 `greetingAudioUrl`、`voiceType` 等字段）

成功后端行为：
- 创建私人角色（`isPublic=false`，`userId=当前用户`）。
- 若草稿含 `greetingMessage`：
  1. 用 `MarkdownTextUtils.cleanForTTS` 清理文本；
  2. 使用 `TTSService.textToSpeech()` 生成音频（默认 mp3, 1.0 语速）；
  3. `FileUploadService` 上传CDN（七牛云），得到URL；
  4. 调用 `RoleApplicationService.updatePrivateRole()` 更新 `greetingAudioUrl`；
  5. 返回的 `RoleDTO` 也包含 `greetingAudioUrl` 与 `voiceType`。
- 在对话中写入系统消息 `metadata.type=ROLE_CREATED`，记录创建信息。

相关代码：
- 控制器：`nexusvoice-backend/src/main/java/com/nexusvoice/interfaces/api/role/RoleAssistantController.java:1`
- 服务：`nexusvoice-backend/src/main/java/com/nexusvoice/application/role/service/RoleAssistantService.java:1`
- TTS链路：
  - `nexusvoice-backend/src/main/java/com/nexusvoice/application/tts/service/TTSService.java:1`
  - `nexusvoice-backend/src/main/java/com/nexusvoice/utils/MarkdownTextUtils.java:1`

### 可选扩展）应用深研并仅更新草稿（不创建）
- 方法与路径：`POST /api/roles/assistant/research/apply`
- 说明：根据用户编辑的查询集合执行深研，仅更新草稿为新的 `ROLE_BRIEF`，供继续迭代后再创建。
- 请求体：
```json
{
  "conversationId": 123,
  "researchLimit": 12,
  "researchQueries": ["编辑后的查询1", "编辑后的查询2"]
}
```
- 响应体：`RoleBriefDto`（同时写入系统消息 `ROLE_BRIEF`）

---

## 三、业务语义与状态

- 草稿（Brief）：结构化的人设信息，用于创建角色前的预览与编辑。持久化在对话的系统消息 `metadata.type=ROLE_BRIEF`。
- 深研（Research）：对草稿进行资料拓展、来源汇总与风格优化。可预览任务→可编辑任务→执行深研。
- 角色（Role）：最终用于对话扮演的人设实体。私人角色仅创建者可见与可用；公共角色由管理员发布。
- 合规：默认强调“原创不复刻具体IP”，sources 为可追溯来源；外部内容只存可公开的摘要与链接。

---

## 四、前端对接建议

页面流（建议）：
1. 聊天页提供“生成角色草稿”按钮 → 调用 brief 接口，右侧展示 `RoleBriefDto` 卡片。
2. “预览深研任务”按钮 → 展示 `RoleResearchTaskPreviewDto` 列表，允许增删改查询词；可勾选“执行深研仅更新草稿”。
3. “确认创建角色”按钮 → 表单项：
   - 模式选择：快速/深研；
   - 语音音色：`voiceType` 下拉（默认 `qiniu_zh_female_dmytwz`）；
   - 任务列表（仅深研时显示，可传 `researchQueries` 与 `researchLimit`）。
4. 成功后返回 `RoleDTO`，可在聊天页切换为该角色继续对话。

前端参数要点：
- `voiceType`：让用户自行选择音色，后端将用于 greeting 的TTS以及角色 `voiceType` 字段。
- 若无需深研，`deepResearch=false` 即可；需要深研时可传 `researchQueries` 覆盖默认任务。
- 如果只想反复优化草稿，使用 `/research/apply` 多轮迭代，满意后再 `confirm` 创建。

---

## 五、默认与限额

- 深研默认条目：12（系统最大允许20，超出将被截断）。
- TTS：默认 `mp3` 格式、`speedRatio=1.0`。
- 默认音色：未指定时为 `qiniu_zh_female_dmytwz`。
- 历史读取：生成草稿时读取对话最近 20 条消息，且单条文本做长度裁剪以控成本。

---

## 六、错误与容错

- 若未找到草稿：接口会返回“未找到角色草稿，请先生成草稿”。
- 深研失败：会降级为使用原草稿创建；不会导致整体失败。
- TTS/上传失败：仅记录日志并返回无音频的角色（`greetingAudioUrl` 为空），不影响角色创建成功。

---

## 七、代码参照

- 控制器：`nexusvoice-backend/src/main/java/com/nexusvoice/interfaces/api/role/RoleAssistantController.java:1`
- 服务：`nexusvoice-backend/src/main/java/com/nexusvoice/application/role/service/RoleAssistantService.java:1`
- 角色应用服务：`nexusvoice-backend/src/main/java/com/nexusvoice/application/role/service/RoleApplicationService.java:1`
- TTS服务：`nexusvoice-backend/src/main/java/com/nexusvoice/application/tts/service/TTSService.java:1`
- Markdown清理：`nexusvoice-backend/src/main/java/com/nexusvoice/utils/MarkdownTextUtils.java:1`


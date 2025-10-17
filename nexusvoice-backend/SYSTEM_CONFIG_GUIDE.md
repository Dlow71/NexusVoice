# 系统配置管理指南

## 📋 概述

本文档说明NexusVoice项目如何使用**系统配置表（system_config）**来集中管理所有可配置项，彻底消除代码中的硬编码默认值。

## 🎯 核心目标

将所有硬编码的默认值（如默认AI模型、默认温度参数等）改为从数据库配置表动态获取，实现：

1. **配置热更新**：修改配置无需重启服务（5分钟缓存过期后自动生效）
2. **统一管理**：所有配置集中在数据库，便于运维管理
3. **易于维护**：避免代码中散落的魔法数字

## 🏗️ 架构设计

### 数据库层

- **表名**：`system_config`
- **迁移脚本**：`V5__create_system_config_table_mysql.sql`
- **字段**：
  - `config_key`：配置键（唯一）
  - `config_value`：配置值
  - `config_group`：配置分组（ai、conversation、tts等）
  - `enabled`：是否启用
  - `readonly`：是否只读

### 领域层

#### SystemConfigKey（常量类）
位置：`/domain/config/constant/SystemConfigKey.java`

集中定义所有配置键常量，例如：
```java
public static final String AI_MODEL_DEFAULT = "ai.model.default";
public static final String AI_TEMPERATURE_DEFAULT = "ai.temperature.default";
```

#### SystemConfigService（领域服务）
位置：`/domain/config/service/SystemConfigService.java`

提供便捷的配置获取方法，带本地缓存（5分钟过期）：

```java
// 基础方法
String getString(String key, String defaultValue)
Integer getInt(String key, Integer defaultValue)
Double getDouble(String key, Double defaultValue)
Boolean getBoolean(String key, Boolean defaultValue)

// 便捷方法
String getDefaultAiModel()
Double getDefaultTemperature()
Integer getDefaultMaxTokens()
String getDefaultSystemPrompt()
```

## 📝 核心配置项

### AI模型配置

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| ai.model.default | openai:gpt-oss-20b | 默认AI模型（完整格式） |
| ai.model.default.provider | openai | 默认模型厂商 |
| ai.model.default.code | gpt-oss-20b | 默认模型代码 |
| ai.temperature.default | 0.7 | AI默认温度参数 |
| ai.max_tokens.default | 2000 | AI默认最大令牌数 |
| ai.system_prompt.default | 你是一个有用的AI助手 | 默认系统提示词 |

### 对话配置

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| conversation.title.default | 新对话 | 默认对话标题 |
| conversation.max_history | 20 | 对话历史最大条数 |
| conversation.max_messages | 100 | 单个对话最大消息数 |
| conversation.max_tokens | 50000 | 单个对话最大令牌数 |

### TTS配置

| 配置键 | 默认值 | 说明 |
|--------|--------|------|
| tts.voice.default | qiniu_zh_female_wwxkjx | 默认语音类型 |
| tts.speed.default | 1.0 | 默认语速 |
| tts.encoding.default | mp3 | 默认音频编码 |

## 🔧 已修改的代码

### 1. ConversationApplicationService

**修改内容**：
- 注入 `SystemConfigService`
- 所有硬编码的默认值改为从配置获取：
  - 默认模型：`gpt-4o-mini` → `systemConfigService.getDefaultAiModel()`
  - 默认标题：`"新对话"` → `systemConfigService.getDefaultConversationTitle()`
  - 默认系统提示词：`"你是一个有用的AI助手"` → `systemConfigService.getDefaultSystemPrompt()`
  - 最大消息数：`100` → `systemConfigService.getConversationMaxMessages()`
  - 最大令牌数：`50000` → `systemConfigService.getConversationMaxTokens()`

**关键方法**：
- `getOrCreateConversation()` - 创建对话时使用配置默认值
- `createConversation()` - 创建对话时使用配置默认值
- `getAiChatService()` - 自动添加厂商前缀，使用配置的默认模型
- `buildSystemPrompt()` - 使用配置的默认系统提示词

### 2. ChatStreamHandler

**修改内容**：
- 将 `SystemConfigRepository` 替换为 `SystemConfigService`
- 修改 `getIntConfig()` 和 `getBooleanConfig()` 方法使用 `SystemConfigService`
- 所有默认值改为从配置获取（与ConversationApplicationService一致）

**关键方法**：
- `getOrCreateConversation()` - 使用配置默认值
- `getAiChatService()` - 使用配置默认模型

### 3. ChatRequest（基础设施层）

**修改内容**：
- 将静态工厂方法 `defaultRequest()` 和 `streamRequest()` 标记为 `@Deprecated`
- 更新默认模型为 `openai:gpt-oss-20b`
- 添加注释说明应从 `SystemConfigService` 获取配置

## 📊 默认模型变更

### 变更前
- HTTP接口：`gpt-4o-mini`（多处硬编码）
- WebSocket：`openai:gpt-oss-20b`（硬编码）
- 不一致，容易混淆

### 变更后
- **统一默认模型**：`openai:gpt-oss-20b`（可通过配置修改）
- **配置键**：`ai.model.default`
- **修改方式**：更新数据库配置表即可，5分钟内生效

## 🚀 使用方式

### 修改默认AI模型

```sql
-- 方式1：修改完整模型名称
UPDATE system_config 
SET config_value = 'openai:gpt-4o' 
WHERE config_key = 'ai.model.default';

-- 方式2：分别修改厂商和模型代码
UPDATE system_config 
SET config_value = 'openai' 
WHERE config_key = 'ai.model.default.provider';

UPDATE system_config 
SET config_value = 'gpt-4o' 
WHERE config_key = 'ai.model.default.code';
```

### 修改默认系统提示词

```sql
UPDATE system_config 
SET config_value = '你是一个专业的AI编程助手' 
WHERE config_key = 'ai.system_prompt.default';
```

### 修改对话限制

```sql
-- 修改单个对话最大消息数
UPDATE system_config 
SET config_value = '200' 
WHERE config_key = 'conversation.max_messages';

-- 修改单个对话最大令牌数
UPDATE system_config 
SET config_value = '100000' 
WHERE config_key = 'conversation.max_tokens';
```

## 💡 最佳实践

### 1. 优先级顺序

系统在获取配置时遵循以下优先级（从高到低）：

1. **请求参数**：用户在API请求中明确指定的值
2. **对话配置**：对话创建时保存的配置
3. **系统配置**：数据库 system_config 表中的配置
4. **代码默认值**：代码中的fallback值（极少使用）

### 2. 配置管理建议

- **分组管理**：使用 `config_group` 字段对配置分组
- **启用/禁用**：使用 `enabled` 字段控制配置是否生效
- **只读保护**：重要配置设置 `readonly=1` 防止误修改
- **定期备份**：定期导出 system_config 表数据

### 3. 缓存刷新

- **自动刷新**：配置缓存5分钟后自动过期
- **手动刷新**：调用 `systemConfigService.refreshCache()` 立即刷新
- **清空缓存**：调用 `systemConfigService.clearCache()` 清空缓存

### 4. 开发建议

- **使用常量**：引用配置键时使用 `SystemConfigKey` 常量，避免字符串拼写错误
- **提供默认值**：所有获取配置的地方都应提供合理的默认值
- **添加日志**：关键配置的使用添加 DEBUG 级别日志

## 🔍 故障排查

### 配置未生效

1. 检查配置是否启用：`enabled = 1`
2. 检查缓存是否过期（等待5分钟或手动刷新）
3. 检查配置键是否正确（使用 SystemConfigKey 常量）

### 查询当前生效配置

```sql
-- 查看所有AI相关配置
SELECT config_key, config_value, enabled, description 
FROM system_config 
WHERE config_group = 'ai' 
ORDER BY sort_order;

-- 查看特定配置
SELECT config_key, config_value, enabled 
FROM system_config 
WHERE config_key = 'ai.model.default';
```

## 📌 注意事项

1. **兼容性**：代码支持旧格式模型名称（无厂商前缀），会自动添加默认厂商前缀
2. **数据迁移**：执行 Flyway 迁移脚本 V5 后配置表会自动创建并填充默认数据
3. **环境隔离**：不同环境（开发、测试、生产）可配置不同的默认模型
4. **监控告警**：建议监控配置变更，避免误操作影响生产环境

## 🎉 总结

通过系统配置表的集中管理，我们实现了：

✅ **消除硬编码**：所有默认值从数据库动态获取  
✅ **配置统一**：HTTP和WebSocket使用相同的默认模型  
✅ **易于维护**：修改配置无需修改代码和重启服务  
✅ **便于运维**：通过SQL即可调整系统行为  
✅ **提高灵活性**：不同环境、不同场景可使用不同配置  

---

**更新时间**：2025-10-17  
**作者**：NexusVoice Team

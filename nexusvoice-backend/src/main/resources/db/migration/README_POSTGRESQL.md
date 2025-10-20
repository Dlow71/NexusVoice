# NexusVoice PostgreSQL 数据库迁移指南

## 📋 概述

本项目使用 **Flyway** 进行数据库版本管理，已全面迁移至 **PostgreSQL**。

## 🗂️ 迁移脚本说明

### V1 - 数据库初始化
**文件**: `V1__init_database_postgresql.sql`

创建所有核心表结构：
- ✅ `users` - 用户表
- ✅ `roles` - AI角色表
- ✅ `conversations` - 对话会话表
- ✅ `conversation_messages` - 对话消息表
- ✅ `ai_models` - AI模型配置表
- ✅ `ai_api_keys` - API密钥池表
- ✅ `ai_api_call_logs` - API调用日志表
- ✅ `system_config` - 系统配置表

**特性**:
- 使用 `BIGINT` 作为主键（支持雪花ID）
- 完整的外键约束
- 优化的索引设计
- 自动 `updated_at` 触发器
- 详细的字段注释（COMMENT）

### V2 - AI模型初始数据
**文件**: `V2__init_ai_models_postgresql.sql`

预置10个常用AI模型：
1. OpenAI GPT-4o-mini ⭐ （推荐）
2. OpenAI GPT-4o
3. OpenAI GPT-4 Turbo
4. Claude 3.5 Sonnet
5. Grok Beta
6. DeepSeek Chat
7. DeepSeek V3.1 🆕 （支持思考链）
8. 通义千问 Max
9. GPT OSS 20B
10. Grok 4 Fast

**特性**:
- 包含详细的价格信息（输入/输出token单价）
- 配置上下文窗口大小
- 默认参数设置
- `ON CONFLICT DO NOTHING` 避免重复插入

### V3 - 系统配置初始数据
**文件**: `V3__init_system_config_postgresql.sql`

初始化系统配置项，包含10个配置分组：
- 🤖 **ai** - AI相关配置（模型、温度等）
- 💬 **conversation** - 对话配置（历史消息数、标题生成等）
- 🎤 **tts** - TTS语音合成配置（分段、并发等）
- 💾 **storage** - 文件存储配置（七牛云/MinIO）
- 🔌 **websocket** - WebSocket配置（心跳、单flight保护等）
- 🔍 **search** - 搜索配置（DuckDuckGo）
- 🎨 **image** - 图像生成配置
- 👤 **role** - 角色助手配置
- 📦 **cache** - 缓存配置（Caffeine + Redis）
- ⚙️ **system** - 系统配置

**配置热更新**:
- 支持运行时修改配置
- 三级缓存自动刷新
- Redis Pub/Sub 多实例同步

### V4 - 示例数据（可选）
**文件**: `V4__init_sample_data_postgresql.sql`

创建示例数据：
- 👤 2个测试用户（admin、test）
- 🤖 5个公共AI角色（助手、编程、写作、学习、生活）

⚠️ **生产环境建议**：删除或修改此脚本中的测试数据。

## 🚀 使用方法

### 1. 配置数据库连接

编辑 `application-local.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nexusvoice
    username: your_username
    password: your_password
    driver-class-name: org.postgresql.Driver
  
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    encoding: UTF-8
```

### 2. 创建数据库

```bash
# 方法1：使用psql命令
createdb nexusvoice

# 方法2：使用psql客户端
psql -U postgres
CREATE DATABASE nexusvoice;
\q
```

### 3. 启动应用

Flyway会自动执行迁移脚本：

```bash
./mvnw spring-boot:run
```

### 4. 验证迁移状态

```sql
-- 查看Flyway迁移历史
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 查看表结构
\dt

-- 查看AI模型数据
SELECT id, provider_code, model_code, model_name, status FROM ai_models;

-- 查看系统配置
SELECT config_key, config_value, description FROM system_config ORDER BY config_group, sort_order;
```

## 🔧 手动执行迁移（可选）

如果需要手动执行迁移：

```bash
# 进入PostgreSQL
psql -U postgres -d nexusvoice

# 依次执行迁移脚本
\i /path/to/V1__init_database_postgresql.sql
\i /path/to/V2__init_ai_models_postgresql.sql
\i /path/to/V3__init_system_config_postgresql.sql
\i /path/to/V4__init_sample_data_postgresql.sql
```

## 📊 数据库设计亮点

### 1. 雪花ID主键
```sql
id BIGINT PRIMARY KEY  -- 支持分布式ID生成
```

### 2. 自动时间戳
```sql
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

-- 自动更新触发器
CREATE TRIGGER update_xxx_updated_at BEFORE UPDATE ON xxx
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### 3. 逻辑删除
```sql
deleted SMALLINT NOT NULL DEFAULT 0  -- 0-未删除, 1-已删除
```

### 4. 优化索引
- 单列索引：提升查询性能
- 复合索引：支持多条件查询
- 外键索引：加速关联查询

### 5. 完整约束
- `UNIQUE` 约束：防止重复数据
- `FOREIGN KEY` 约束：保证数据完整性
- `CHECK` 约束：业务规则验证

## 🆚 与MySQL的差异

| 特性 | MySQL | PostgreSQL |
|------|-------|------------|
| 自增主键 | `AUTO_INCREMENT` | 使用雪花ID（应用层生成） |
| 时间类型 | `DATETIME` | `TIMESTAMP` |
| 布尔类型 | `TINYINT(1)` | `BOOLEAN` |
| JSON类型 | `JSON` / `TEXT` | `JSONB` / `TEXT` |
| 触发器 | `BEFORE UPDATE` | `BEFORE UPDATE` + Function |
| 注释 | `COMMENT` | `COMMENT ON` |
| 事务 | 支持 | 更强大的MVCC |
| 并发 | 表锁/行锁 | 无锁读（MVCC） |

## 🔍 常见问题

### Q1: Flyway报错 "Found non-empty schema"
**解决方案**:
```yaml
spring:
  flyway:
    baseline-on-migrate: true  # 允许在非空数据库上迁移
```

### Q2: 如何重置数据库？
```sql
-- 删除所有表
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;

-- 重新执行迁移
-- 重启应用即可
```

### Q3: 如何查看迁移状态？
```sql
SELECT 
    installed_rank, 
    version, 
    description, 
    type, 
    script, 
    checksum, 
    installed_on, 
    execution_time, 
    success 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

### Q4: 如何跳过某个迁移脚本？
**不推荐跳过迁移！** 如果必须跳过：
```sql
-- 手动插入迁移记录（仅用于特殊情况）
INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, 
    script, checksum, installed_by, installed_on, 
    execution_time, success
) VALUES (
    nextval('flyway_schema_history_installed_rank_seq'),
    'X', 'Skip migration', 'SQL',
    'VX__skip.sql', 0, 'admin', CURRENT_TIMESTAMP,
    0, true
);
```

## 📚 参考资料

- [Flyway官方文档](https://flywaydb.org/documentation/)
- [PostgreSQL官方文档](https://www.postgresql.org/docs/)
- [MyBatis-Plus文档](https://baomidou.com/)
- [NexusVoice架构设计文档](../../../架构设计文档.md)

## 🎯 下一步

1. ✅ 完成数据库迁移
2. 🔧 配置AI模型API密钥（`ai_api_keys`表）
3. 🔧 配置存储服务（七牛云/MinIO）
4. 🚀 启动后端服务
5. 🎨 启动前端应用

---

**NexusVoice** - 企业级多模态AI对话平台

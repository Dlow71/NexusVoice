# NexusVoice 🎙️

> 🚀 企业级多模态AI对话平台，基于DDD架构 + Java 21虚拟线程 + 动态模型管理，打造极致的AI交互体验

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue.js-3.5.21-4FC08D.svg)](https://vuejs.org/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35.0-blue.svg)](https://github.com/langchain4j/langchain4j)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)

## ✨ 项目介绍

NexusVoice 是一个**生产级的多模态AI对话平台**，采用严格的DDD架构设计，集成了智能对话、图像生成、语音合成、实时搜索等AI能力。项目核心亮点在于**完全动态化的模型管理**、**Java 21虚拟线程并发优化**、**责任链增强系统**和**三级缓存架构**，为企业级AI应用提供坚实的技术基础。

### 🎯 为什么选择 NexusVoice？

#### 🤖 多模态AI能力
- **智能对话** - 支持OpenAI、Grok、DeepSeek等多种模型，动态热切换
- **实时联网搜索** - MCP搜索系统，AI智能判断何时搜索，DuckDuckGo集成
- **AI图像生成** - 硅基流动API，支持Qwen/Kolors 4种模型，自动CDN上传
- **语音合成(TTS)** - 智能文本分段 + 虚拟线程并发上传 + 实时音频播放
- **流式对话** - WebSocket实时响应，支持分段TTS流式输出

#### 🏗️ 核心技术亮点

##### 1️⃣ **完全动态化的AI模型管理系统** ⭐⭐⭐
- **数据库驱动配置**：`ai_models`、`ai_api_keys`、`ai_api_call_logs`三张表管理模型
- **API密钥池**：支持加权轮询、健康检查、自动熔断恢复、配额管理
- **热更新支持**：修改配置无需重启服务，实时生效
- **费用追踪**：精确统计每次调用的token使用量和费用
- **多模型适配**：抽象适配器模式，轻松接入新模型（OpenAI/Grok/DeepSeek）

##### 2️⃣ **TTS智能分段并发处理（Java 21虚拟线程）** ⭐⭐⭐
- **智能文本切分**：`TextChunker`按句子边界切分，最大300字/段
- **虚拟线程并发**：`Executors.newVirtualThreadPerTaskExecutor()`实现轻量级并发
- **Semaphore控制**：限制最大并发数（可配置，默认4），避免API限流
- **实时上传CDN**：每段TTS完成后立即上传七牛云/MinIO
- **性能提升**：相比单线程处理，性能提升10倍以上

##### 3️⃣ **存储策略管理（模板+策略模式）** ⭐⭐
- **模板方法模式**：`AbstractStorageRepository`定义上传流程骨架
- **策略模式**：`StorageStrategyManager`根据`system_config`动态切换存储
- **支持多提供商**：七牛云、MinIO，无缝切换
- **健康检查**：定期检查存储服务可用性，自动切换到备用存储
- **文件迁移**：提供完整的跨存储迁移工具

##### 4️⃣ **SystemConfig三级缓存架构** ⭐⭐
- **三级缓存**：Caffeine本地缓存（30秒）+ Redis缓存（1小时）+ PostgreSQL
- **Redis Pub/Sub**：配置变更实时广播到所有实例
- **强一致性**：CUD操作立即失效缓存并通知其他实例
- **多实例支持**：每个实例有唯一ID，避免重复处理自己发布的事件
- **配置热更新**：修改数据库配置后立即生效，无需重启

##### 5️⃣ **责任链增强系统** ⭐
- **ChatEnhancementChain**：统一管理增强器链
- **SearchEnhancer**：智能联网搜索增强（AI判断是否需要搜索）
- **动态扩展**：支持添加新的增强器（RAG、多模态等）
- **可配置开关**：每个增强器可独立启用/禁用

##### 6️⃣ **WebSocket流式对话** ⭐
- **JWT握手鉴权**：支持Header和Query参数两种认证方式
- **实时流式输出**：逐字推送AI回复，打字机效果
- **分段TTS流式**：音频片段生成后立即推送，边说边听
- **单flight保护**：防止同一会话并发请求冲突
- **心跳保活**：5秒心跳机制，保持连接稳定

##### 7️⃣ **角色助手系统** ⭐
- **从对话生成角色**：分析历史对话，AI自动生成角色设定
- **联网深度研究**：可选启用联网搜索，增强角色背景知识
- **自动TTS开场白**：角色创建后自动生成专属语音
- **完整工作流**：草稿生成 → 深研预览 → 应用深研 → 确认创建

#### 🎨 用户体验创新
- **实时音频队列**：智能管理分段音频播放，错误自动跳过
- **智能自动滚动**：检测用户手动滚动，智能决定是否自动滚动
- **Markdown渲染**：Marked + DOMPurify，支持代码高亮和XSS防护
- **语音识别**：浏览器Web Speech API，支持语音输入
- **会话持久化**：sessionStorage保存会话状态，刷新页面无损

#### 🏛️ 架构设计
- **严格的DDD分层**：Interfaces → Application → Domain → Infrastructure
- **微服务就绪**：模块化设计，清晰的边界上下文
- **企业级安全**：JWT + Spring Security + WebSocket鉴权 + 配置分离
- **高性能优化**：三级缓存 + 虚拟线程 + 连接池 + 异步处理

## 🚀 快速开始

### 环境要求
- **后端**: Java 21+, PostgreSQL 15+, Redis 7+
- **前端**: Node.js 18+
- **外部服务**: AI模型API Key（OpenAI/Grok/DeepSeek）, 存储服务（七牛云/MinIO）, 硅基流动API Key

### 快速部署

#### 1. **克隆项目**
```bash
git clone https://github.com/your-org/NexusVoice.git
cd NexusVoice
```

#### 2. **数据库准备**
```bash
# 创建PostgreSQL数据库
createdb nexusvoice

# Flyway会自动执行数据库迁移脚本
# 位置: nexusvoice-backend/src/main/resources/db/migration/
```

#### 3. **后端配置**
```bash
cd nexusvoice-backend

# 复制配置模板
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml

# 编辑配置文件，填入以下关键配置：
# - PostgreSQL连接信息
# - Redis连接信息
# - JWT密钥
# - AI模型API密钥（在数据库ai_api_keys表中配置）
# - 存储服务配置（七牛云/MinIO）

# 启动后端服务
./mvnw spring-boot:run
```

#### 4. **前端启动**
```bash
cd nexus-voice-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

#### 5. **访问应用**
- **前端界面**: http://localhost:5173
- **后端API**: http://localhost:8081
- **API文档**: http://localhost:8081/swagger-ui.html
- **健康检查**: http://localhost:8081/actuator/health

#### 6. **配置AI模型（重要）**
```sql
-- 在ai_models表中已预置多个模型配置
-- 需要在ai_api_keys表中添加你的API密钥
INSERT INTO ai_api_keys (id, provider_code, model_code, api_key, base_url, weight, status)
VALUES (雪花ID, 'openai', 'gpt-4o-mini', 'sk-your-api-key', 'https://api.openai.com/v1', 1, 1);

-- 或通过SystemConfig配置默认模型
UPDATE system_config SET config_value = 'openai:gpt-4o-mini' WHERE config_key = 'ai.default.model';
```

## 📁 项目结构

```
NexusVoice/
├── nexusvoice-backend/                    # Spring Boot 后端服务
│   ├── src/main/java/com/nexusvoice/
│   │   ├── interfaces/                    # 接口层 - REST API + WebSocket处理器
│   │   │   ├── api/                      # REST控制器
│   │   │   └── websocket/                # WebSocket处理器
│   │   ├── application/                   # 应用层 - 业务编排
│   │   │   ├── conversation/             # 对话应用服务
│   │   │   ├── role/                     # 角色应用服务
│   │   │   ├── tts/                      # TTS应用服务
│   │   │   └── file/                     # 文件上传服务
│   │   ├── domain/                        # 领域层 - 核心业务逻辑
│   │   │   ├── conversation/             # 对话领域模型
│   │   │   ├── role/                     # 角色领域模型
│   │   │   ├── ai/                       # AI领域模型
│   │   │   ├── config/                   # 配置领域模型
│   │   │   └── storage/                  # 存储领域模型
│   │   └── infrastructure/                # 基础设施层 - 技术实现
│   │       ├── ai/                       # AI服务实现
│   │       │   ├── manager/              # DynamicAiModelBeanManager
│   │       │   ├── pool/                 # ApiKeyPoolManager
│   │       │   ├── model/                # 模型适配器
│   │       │   ├── chain/                # 责任链增强器
│   │       │   └── factory/              # LangChain4j工厂
│   │       ├── repository/               # 仓储实现
│   │       │   └── storage/              # StorageStrategyManager
│   │       └── config/                   # 配置类
│   └── src/main/resources/
│       ├── db/migration/                 # Flyway数据库迁移脚本
│       └── application.yml               # 配置文件
├── nexus-voice-frontend/                  # Vue 3 前端应用
│   ├── src/
│   │   ├── views/                        # 页面组件
│   │   │   ├── ChatStreamView.vue       # WebSocket流式聊天
│   │   │   └── CharacterSelectionView.vue
│   │   ├── components/                   # 业务组件
│   │   │   ├── ConversationSidebar.vue   # 对话历史侧边栏
│   │   │   └── CharacterCard.vue
│   │   ├── services/                     # API服务
│   │   └── stores/                       # Pinia状态管理
│   └── public/                           # 静态资源
└── 架构设计文档.md                        # 详细架构设计文档
```

## 🛠️ 技术栈

### 后端核心技术
| 技术 | 版本 | 用途说明 |
|------|------|----------|
| **Java** | 21 | 核心语言，支持虚拟线程 |
| **Spring Boot** | 3.3.5 | 应用框架 |
| **LangChain4j** | 0.35.0 | AI模型集成框架 |
| **PostgreSQL** | 15+ | 主数据库 |
| **Redis** | 7+ | 缓存 + Pub/Sub |
| **MyBatis-Plus** | 3.5.11 | ORM框架 |
| **Flyway** | - | 数据库版本管理 |
| **Spring Security** | - | 安全认证框架 |
| **JWT** | 0.12.6 | Token认证 |
| **WebSocket** | - | 实时通信 |
| **Caffeine** | 3.1.8 | 本地缓存 |
| **Redisson** | 3.35.0 | Redis分布式客户端 |
| **RocketMQ** | 2.3.0 | 消息队列（可选） |
| **Druid** | 1.2.23 | 数据库连接池 |

### 前端技术栈
| 技术 | 版本 | 用途说明 |
|------|------|----------|
| **Vue.js** | 3.5.21 | 前端框架 |
| **Vite** | 7.1.7 | 构建工具 |
| **Element Plus** | 2.11.4 | UI组件库 |
| **Pinia** | 3.0.3 | 状态管理 |
| **Axios** | 1.12.2 | HTTP客户端 |
| **Marked** | 16.3.0 | Markdown渲染 |
| **DOMPurify** | 3.2.7 | XSS防护 |

### 第三方服务集成
| 服务类型 | 提供商 | 说明 |
|---------|--------|------|
| **AI对话** | OpenAI / Grok / DeepSeek | 支持多模型动态切换 |
| **AI绘画** | 硅基流动 | Qwen/Kolors 4种模型 |
| **联网搜索** | DuckDuckGo | 免费搜索API |
| **对象存储** | 七牛云 / MinIO | 支持动态切换 |
| **TTS语音** | 七牛云 | 语音合成服务 |

## 📊 数据库设计

### 核心表结构
```sql
-- AI模型配置表
ai_models              # 模型配置（provider、model_code、费用等）
ai_api_keys            # API密钥池（加权轮询、健康检查、配额管理）
ai_api_call_logs       # 调用日志（token使用、费用统计）

-- 系统配置表
system_config          # 三级缓存配置表（支持热更新）

-- 对话相关表
conversations          # 对话会话（绑定模型、角色、用户）
conversation_messages  # 对话消息（支持流式存储）

-- 角色表
roles                  # AI角色配置（人设、语音、开场白）

-- 用户表
users                  # 用户信息（JWT认证）
```

### Flyway迁移脚本
项目使用Flyway进行数据库版本管理，迁移脚本位于：
```
nexusvoice-backend/src/main/resources/db/migration/
├── V1__init_database.sql
├── V2__create_users_table_mysql.sql
├── V3__create_conversation_tables_mysql.sql
├── V4__create_ai_model_tables_mysql.sql
├── V5__create_system_config_table_mysql.sql
├── V6__add_storage_config_mysql.sql
├── V7__add_deepseek_models_mysql.sql
└── ...
```

## 🎮 核心功能

### 💬 流式对话
- **WebSocket实时推送**：逐字显示AI回复，打字机效果
- **多模型支持**：OpenAI GPT-4/GPT-4o-mini、Grok、DeepSeek V3等
- **联网搜索**：AI智能判断何时需要搜索，自动获取最新信息
- **上下文管理**：自动管理对话历史，支持多轮对话
- **模型热切换**：数据库配置，无需重启服务

### 🎨 AI图像生成
- **4种模型**：Qwen/Kolors系列，支持不同风格
- **丰富参数**：尺寸、种子、CFG、引导比例等
- **批量生成**：Kolors模型支持批量生成
- **自动CDN**：生成后自动上传七牛云，返回永久URL

### 🎤 TTS语音合成
- **智能分段**：长文本按句子边界智能切分
- **虚拟线程并发**：Java 21虚拟线程，并发生成音频
- **实时推送**：WebSocket流式推送音频片段
- **音频队列**：前端智能管理播放顺序，错误自动跳过

### 🎭 AI角色助手
- **从对话生成**：分析历史对话，AI自动生成角色设定
- **深度研究**：可选联网搜索，增强角色背景知识
- **自动语音**：角色创建后自动生成专属TTS开场白
- **私有化**：每个用户的角色互不干扰

## 🔧 性能优化

### 缓存策略
- **三级缓存**：Caffeine（30秒）+ Redis（1小时）+ PostgreSQL
- **配置热更新**：Redis Pub/Sub实时同步到所有实例
- **缓存穿透保护**：空值缓存，避免恶意查询

### 并发优化
- **Java 21虚拟线程**：TTS并发处理、WebSocket异步任务
- **连接池**：Druid数据库连接池、Redis连接池
- **API密钥轮询**：加权轮询算法，负载均衡

### 数据库优化
- **索引优化**：全表索引覆盖，查询性能优化
- **雪花ID**：分布式ID生成，避免自增ID瓶颈
- **逻辑删除**：数据安全，支持恢复

## 🤝 贡献指南

欢迎所有形式的贡献！

### 参与方式
- 🐛 **报告Bug**：通过Issue描述问题和复现步骤
- 💡 **提出功能**：分享你的想法和需求
- 📖 **改进文档**：完善README、代码注释
- 💻 **提交代码**：实现新功能或修复Bug

### 提交流程
1. Fork本项目
2. 创建特性分支：`git checkout -b feature/amazing-feature`
3. 提交更改：`git commit -m '新增：XXX功能'`
4. 推送到分支：`git push origin feature/amazing-feature`
5. 提交Pull Request

### 开发规范
- 遵循DDD分层架构，代码放到正确的层
- 使用`BizException`和`ErrorCodeEnum`统一异常处理
- 日志使用中文，便于调试
- 编写单元测试覆盖核心逻辑

## 📚 文档资源

- 📖 **架构设计文档**：`架构设计文档.md`
- 📖 **运行说明**：`运行说明.md`
- 📖 **API文档**：http://localhost:8081/swagger-ui.html
- 📖 **数据库迁移脚本**：`nexusvoice-backend/src/main/resources/db/migration/`

## 🌟 Star History

如果这个项目对你有帮助，欢迎给个Star支持！⭐

## 📄 开源协议

本项目采用 MIT 协议开源，详见 [LICENSE](LICENSE) 文件。

---

**NexusVoice** - 企业级多模态AI对话平台，让AI交互更智能、更流畅、更有趣！

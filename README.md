# NexusVoice 🎙️

> 🚀 一个现代化的智能语音对话系统，让AI不仅能聊天，还能看世界、画图片、说话！

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue.js-3.5.21-4FC08D.svg)](https://vuejs.org/)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-0.35.0-blue.svg)](https://github.com/langchain4j/langchain4j)

## ✨ 什么是 NexusVoice？

NexusVoice 是一个功能强大的多模态AI对话平台，不仅支持传统的文本对话，还集成了图像生成、语音合成、实时搜索等AI能力，为用户提供沉浸式的AI交互体验。

### 🎯 核心特性

#### 🤖 多模态AI能力
- **智能对话** - 基于OpenAI GPT模型的高质量对话
- **实时联网搜索** - 集成DuckDuckGo，AI可获取最新信息
- **AI图像生成** - 支持硅基流动API，4种AI绘画模型
- **语音合成** - 文本转语音，让AI真的能"说话"
- **流式对话** - WebSocket实时响应，体验更流畅

#### 🎨 创新用户体验
- **角色助手** - 从对话历史自动生成个性化AI角色
- **分段TTS** - 长文本智能分段播放，边说边听
- **语音识别** - 浏览器Web Speech API，支持语音输入
- **音频队列** - 智能音频播放管理，文本音频完美同步

#### 🏗️ 现代化架构
- **DDD分层设计** - 严格的领域驱动设计四层架构
- **微服务就绪** - 模块化设计，支持服务拆分
- **企业级安全** - JWT + Spring Security + WebSocket鉴权
- **高性能优化** - 缓存策略 + 异步处理 + 连接池

## 🚀 快速开始

### 环境要求
- **后端**: Java 21+, MySQL 8.0+
- **前端**: Node.js 18+
- **外部服务**: OpenAI API Key, 七牛云账户, 硅基流动API Key

### 快速部署

1. **克隆项目**
   ```bash
   git clone https://github.com/your-org/NexusVoice.git
   cd NexusVoice
   ```

2. **后端配置**
   ```bash
   cd nexusvoice-backend
   cp application-local.yml.example src/main/resources/application-local.yml
   # 编辑配置文件，填入你的API密钥
   ./mvnw spring-boot:run
   ```

3. **前端启动**
   ```bash
   cd nexus-voice-frontend
   npm install
   npm run dev
   ```

4. **访问应用**
   - 前端: http://localhost:5173
   - 后端API: http://localhost:8081
   - API文档: http://localhost:8081/swagger-ui.html

## 📁 项目结构

```
NexusVoice/
├── nexusvoice-backend/           # Spring Boot 后端服务
│   ├── src/main/java/com/nexusvoice/
│   │   ├── interfaces/           # 接口层 (REST API + WebSocket)
│   │   ├── application/          # 应用层 (业务编排)
│   │   ├── domain/              # 领域层 (核心业务逻辑)
│   │   └── infrastructure/       # 基础设施层 (技术实现)
│   └── src/main/resources/      # 配置文件和迁移脚本
├── nexus-voice-frontend/         # Vue 3 前端应用
│   ├── src/
│   │   ├── views/               # 页面组件
│   │   ├── components/          # 业务组件
│   │   ├── services/            # API服务
│   │   └── stores/              # Pinia状态管理
│   └── public/                  # 静态资源
```

## 🛠️ 技术栈

### 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.5 | 核心框架 |
| LangChain4j | 0.35.0 | AI集成框架 |
| MyBatis-Plus | 3.5.11 | ORM框架 |
| MySQL | 8.0+ | 数据库 |
| Spring Security | - | 安全框架 |
| WebSocket | - | 实时通信 |

### 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue.js | 3.5.21 | 前端框架 |
| Vite | 7.1.7 | 构建工具 |
| Element Plus | 2.11.4 | UI组件库 |
| Pinia | 3.0.3 | 状态管理 |
| Axios | 1.12.2 | HTTP客户端 |

### AI服务集成
- **对话**: OpenAI GPT-4
- **图像**: 硅基流动 (Qwen/Kolors模型)
- **搜索**: DuckDuckGo API
- **存储**: 七牛云CDN
- **语音**: TTS语音合成

## 🎮 功能演示

### 💬 智能对话
支持多轮对话，AI会根据上下文智能回复，还可以开启联网搜索获取实时信息。

### 🎨 AI绘画
输入文本描述，AI自动生成精美图片，支持多种风格和参数调节。

### 🎤 语音交互
- **语音输入**: 点击麦克风，说话即可转为文字
- **语音输出**: AI回复自动转为语音播放
- **实时流式**: 边生成边播放，真正的实时对话体验

### 🎭 角色助手
AI会分析你的对话历史，自动生成专属的AI角色，每个角色都有独特的个性和语音。

## 🤝 贡献指南

我们欢迎所有形式的贡献！无论是：

- 🐛 报告Bug
- 💡 提出新功能
- 📖 改进文档
- 💻 提交代码

### 开发团队分工
- **前端开发** (1人): Vue.js界面开发、WebSocket集成、音视频处理
- **后端开发** (2人): 
  - 开发者1: 接口层 + 应用层 (API开发、业务编排)
  - 开发者2: 领域层 + 基础设施层 (核心逻辑、AI集成)

### 参与步骤
1. Fork 项目
2. 创建功能分支: `git checkout -b feature/amazing-feature`
3. 提交更改: `git commit -m 'Add amazing feature'`
4. 推送分支: `git push origin feature/amazing-feature`
5. 提交Pull Request

## 🌟 项目亮点

- 🎯 **多模态AI** - 对话+图像+语音+搜索，四大AI能力一站式
- 🚀 **实时体验** - WebSocket流式对话，毫秒级响应
- 🎨 **用户体验** - 语音识别、音频播放、角色助手等创新功能
- 🏗️ **企业架构** - DDD设计、微服务就绪、高可扩展
- 🔒 **安全可靠** - JWT认证、数据加密、配置分离
- 📱 **现代技术** - Spring Boot 3 + Vue 3 + 最新AI框架

⭐ 如果这个项目对你有帮助，请给我们一个星标支持！

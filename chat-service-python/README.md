# Chat Service - 基于 FastAPI + LangChain + PostgreSQL 的聊天服务

## 📋 项目简介

这是一个基于 Python FastAPI 框架开发的 AI 聊天服务，集成了 LangChain 和 OpenAI API，使用 PostgreSQL 数据库存储聊天历史。

### 技术栈

- **Web 框架**：FastAPI 0.115+
- **异步运行时**：Uvicorn (ASGI)
- **数据库**：PostgreSQL + SQLModel ORM
- **AI 框架**：LangChain + OpenAI API
- **日志**：Loguru
- **流式响应**：SSE (Server-Sent Events)

## 🏗️ 项目架构

遵循 DDD（领域驱动设计）分层架构：

```
chat-service-python/
├── server.py                    # 应用启动入口
├── pyproject.toml               # 项目依赖配置
├── .env                         # 环境变量（需自行创建）
├── .env.example                 # 环境变量示例
├── .python-version              # Python版本声明
├── README.md                    # 项目文档
│
└── chat_service/                # 源代码包
    ├── __init__.py
    │
    ├── api/                     # API层 - 路由定义
    │   ├── __init__.py
    │   └── chat.py              # 聊天接口
    │
    ├── model/                   # 数据模型层
    │   ├── __init__.py
    │   ├── protocol.py          # 请求响应协议（DTO）
    │   └── chat.py              # 聊天领域模型
    │
    ├── tool/                    # 业务逻辑层（领域服务）
    │   ├── __init__.py
    │   └── chat_service.py      # 聊天业务逻辑
    │
    ├── db/                      # 数据访问层
    │   ├── __init__.py
    │   ├── db_engine.py         # 数据库引擎配置
    │   ├── conversation.py      # 对话表模型
    │   ├── message.py           # 消息表模型
    │   ├── conversation_op.py   # 对话表操作
    │   └── message_op.py        # 消息表操作
    │
    └── util/                    # 工具层
        ├── __init__.py
        ├── llm_util.py          # LLM调用工具
        ├── config.py            # 配置管理
        └── log_util.py          # 日志工具
```

## 🚀 快速开始

### 1. 环境要求

- Python 3.11+
- PostgreSQL 14+

### 2. 安装依赖

```bash
# 创建虚拟环境
python -m venv .venv

# 激活虚拟环境
source .venv/bin/activate  # Mac/Linux
# 或
.venv\Scripts\activate     # Windows

# 安装依赖
pip install -e .
```

### 3. 配置环境变量

```bash
# 复制环境变量示例文件
cp .env.example .env

# 编辑 .env 文件，配置数据库和 OpenAI API Key
vim .env
```

必须配置的环境变量：
- `DB_URL`: PostgreSQL 数据库连接地址
- `OPENAI_API_KEY`: OpenAI API 密钥

### 4. 初始化数据库

```bash
# 创建数据库表
python -m chat_service.db.db_engine
```

### 5. 启动服务

```bash
# 方式1：使用 server.py 启动
python server.py

# 方式2：使用 uvicorn 命令启动（支持热重载）
uvicorn server:app --host 0.0.0.0 --port 8000 --reload
```

服务启动后访问：
- API 文档：http://localhost:8000/docs
- 健康检查：http://localhost:8000/api/v1/health

## 📚 API 接口说明

### 1. 创建对话

```http
POST /api/v1/chat/conversation
Content-Type: application/json

{
  "title": "测试对话"
}
```

### 2. 发送消息（普通模式）

```http
POST /api/v1/chat/message
Content-Type: application/json

{
  "conversationId": "1",
  "content": "你好，介绍一下自己"
}
```

### 3. 发送消息（流式模式）

```http
POST /api/v1/chat/stream
Content-Type: application/json

{
  "conversationId": "1",
  "content": "讲个笑话"
}
```

返回 SSE 流式数据。

### 4. 获取对话历史

```http
GET /api/v1/chat/conversation/{conversation_id}/messages
```

## 🔧 开发说明

### 异步编程规范

项目全链路使用异步编程（async/await）：

```python
# ✅ 正确：使用异步函数
async def fetch_data():
    async with async_session_local() as session:
        result = await session.exec(statement)
        return result

# ❌ 错误：在异步函数中使用同步IO
async def bad_example():
    time.sleep(1)  # 会阻塞事件循环！
```

### 数据库操作

使用 SQLModel + AsyncSession：

```python
from chat_service.db.db_engine import get_async_session
from fastapi import Depends

@router.get("/example")
async def example(session: AsyncSession = Depends(get_async_session)):
    # 使用注入的 session 进行数据库操作
    result = await session.exec(select(Message))
    return result.all()
```

### LangChain 集成

参考 `chat_service/util/llm_util.py` 中的实现：

```python
from chat_service.util.llm_util import call_llm, stream_llm

# 普通调用
response = await call_llm("你好")

# 流式调用
async for chunk in stream_llm("讲个故事"):
    print(chunk, end="")
```

## 📝 数据库设计

### conversations 表（对话）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| title | VARCHAR | 对话标题 |
| created_at | TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | 更新时间 |

### messages 表（消息）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| conversation_id | BIGINT | 所属对话ID（外键） |
| role | VARCHAR | 角色（user/assistant/system） |
| content | TEXT | 消息内容 |
| created_at | TIMESTAMP | 创建时间 |

## 🐛 常见问题

### 1. 数据库连接失败

检查 `.env` 中的 `DB_URL` 配置是否正确，确保 PostgreSQL 服务已启动。

### 2. OpenAI API 调用失败

- 检查 `OPENAI_API_KEY` 是否配置正确
- 如果在国内，可能需要配置代理或使用国内镜像地址

### 3. 依赖安装失败

建议使用 Python 3.11+ 版本，某些依赖可能不支持更老的 Python 版本。

## 📄 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

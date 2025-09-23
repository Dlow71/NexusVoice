# NexusVoice Backend

Java 后端骨架（Spring Boot 3 / Java 21）。包含：
- 基础 REST 健康检查接口（`/api/health`）
- WebSocket 端点占位（`/ws/chat`）
- 基础安全配置（放行健康检查与 Swagger，其它默认需鉴权）
- Actuator 健康检查

## 技术栈

- **Spring Boot 3.5.3** - 主框架
- **Java 21** - 编程语言
- **PostgreSQL + pgvector** - 主数据库和向量存储
- **MySQL 8.0** - 可选数据库支持
- **MyBatis-Plus 3.5.7** - ORM框架
- **Druid** - 数据库连接池
- **Spring Security** - 安全框架
- **JWT** - 身份认证
- **LangChain4j** - AI集成框架
- **OpenAI API** - 大语言模型和嵌入模型
- **WebSocket** - 实时通信
- **SpringDoc OpenAPI** - API文档

## 环境要求
- JDK 21+
- Maven 3.9+

## 启动
开发环境（默认）：

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

访问：
- REST: `http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- WS 测试: `ws://localhost:8080/ws/chat`

## 下一步
- 接入 JWT（access/refresh）与 Redis、MySQL、Flyway
- 落地 ASR/LLM/TTS 适配与流式编排
- 加固 CORS/CSRF、限流与审计

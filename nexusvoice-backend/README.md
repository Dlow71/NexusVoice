# NexusVoice Backend

Java 后端骨架（Spring Boot 3 / Java 21）。包含：
- 基础 REST 健康检查接口（`/api/health`）
- WebSocket 端点占位（`/ws/chat`）
- 基础安全配置（放行健康检查与 Swagger，其它默认需鉴权）
- Actuator 健康检查

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

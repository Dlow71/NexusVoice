# NexusVoice 升级指南

## 升级概述

本次升级将项目从 Spring Boot 3.3.2 + Spring AI 升级到 Spring Boot 3.5.3 + LangChain4j，并引入 PostgreSQL + pgvector 作为主要数据库和向量存储解决方案。

## 主要变更

### 1. 框架升级
- **Spring Boot**: 3.3.2 → 3.5.3
- **AI框架**: Spring AI → LangChain4j 0.35.0
- **数据库**: MySQL → PostgreSQL + pgvector

### 2. 新增依赖
```xml
<!-- LangChain4j核心依赖 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>0.35.0</version>
</dependency>

<!-- OpenAI集成 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    <version>0.35.0</version>
</dependency>

<!-- 向量存储 -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-pgvector</artifactId>
    <version>0.35.0</version>
</dependency>

<!-- PGVector支持 -->
<dependency>
    <groupId>com.pgvector</groupId>
    <artifactId>pgvector</artifactId>
    <version>0.1.6</version>
</dependency>
```

### 3. 配置变更

#### 数据库驱动
```yaml
# 旧配置 (MySQL)
driver-class-name: com.mysql.cj.jdbc.Driver

# 新配置 (PostgreSQL)
driver-class-name: org.postgresql.Driver
```

#### 新增LangChain4j配置
```yaml
langchain4j:
  open-ai:
    api-key: ${OPENAI_API_KEY}
    base-url: https://api.openai.com/v1
    chat-model:
      model-name: gpt-4o-mini
      temperature: 0.7
      max-tokens: 2000
    embedding-model:
      model-name: text-embedding-3-small
  pgvector:
    host: localhost
    port: 5432
    database: nexusvoice_dev
    user: ${PGVECTOR_USER}
    password: ${PGVECTOR_PASSWORD}
    dimension: 1536
    table: embeddings
```

## 升级步骤

### 1. 环境准备
```bash
# 停止现有应用
# 备份现有数据（如果需要）

# 启动PostgreSQL + pgvector
docker-compose up -d postgres
```

### 2. 配置更新
```bash
# 复制配置模板
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml

# 编辑配置文件，填入实际值
vim src/main/resources/application-local.yml
```

### 3. 依赖下载
```bash
# 清理并下载新依赖
mvn clean dependency:resolve
```

### 4. 数据库初始化
```bash
# 数据库会通过Docker自动初始化
# 或手动执行SQL脚本
psql -h localhost -U nexusvoice -d nexusvoice_dev -f src/main/resources/db/migration/V1__init_database.sql
```

### 5. 启用LangChain4j配置
编辑 `src/main/java/com/nexusvoice/config/LangChain4jConfig.java`，取消注释配置代码并添加 `@Configuration` 注解。

### 6. 启动应用
```bash
mvn spring-boot:run
```

## 验证升级

### 1. 检查应用启动
- 访问 http://localhost:8080/api/health
- 检查控制台日志无错误

### 2. 检查数据库连接
- 访问 http://localhost:8081 (pgAdmin)
- 验证表结构创建成功

### 3. 检查AI功能
- 验证OpenAI API连接
- 测试向量存储功能

## 回滚方案

如果升级遇到问题，可以：

1. **回滚代码**：
   ```bash
   git checkout <previous-commit>
   ```

2. **恢复数据库**：
   ```bash
   # 停止PostgreSQL
   docker-compose down
   # 恢复MySQL配置
   ```

## 注意事项

1. **API密钥安全**：确保OpenAI API密钥配置在 `application-local.yml` 中，不要提交到Git
2. **数据迁移**：如果有现有MySQL数据需要迁移到PostgreSQL，需要单独处理
3. **性能监控**：升级后注意监控应用性能和资源使用情况
4. **依赖冲突**：如遇到依赖冲突，检查Maven依赖树并解决

## 技术支持

如果升级过程中遇到问题，请：
1. 检查日志文件
2. 验证配置文件格式
3. 确认网络连接（OpenAI API、数据库等）
4. 参考项目文档和LangChain4j官方文档

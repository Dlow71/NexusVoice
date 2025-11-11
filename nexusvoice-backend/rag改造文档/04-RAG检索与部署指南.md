# RAG检索与部署指南

> **系列文档**: RAG知识库系统实现指南 (4/4)  
> **前置阅读**: 03-核心组件实现详解.md

---

## 📋 本文内容

1. RAG检索流程详解
2. 数据库设计与索引优化
3. 部署环境准备
4. 配置与启动
5. 性能优化建议
6. 监控与运维

---

## 🔍 RAG检索流程

### 整体流程

```
用户查询 "Spring Boot配置原理"
    ↓
【步骤1】HyDE - 生成假设文档
    ↓
【步骤2】并行检索
    ├─ 向量检索 (EmbeddingDomainService)
    └─ 关键词检索 (KeywordSearchDomainService)
    ↓
【步骤3】RRF融合算法
    ↓
【步骤4】Rerank重排序
    ↓
返回Top K文档片段
```

### 步骤1: HyDE (Hypothetical Document Embeddings)

**目的**: 将用户问题扩展为假设的答案文档，提升检索准确率

```java
@Service
public class HyDEDomainService {
    
    public String generateHypotheticalDocument(
        String question,
        ChatModelConfig config
    ) {
        if (config == null || !config.isValid()) {
            return question;  // 降级：直接用原问题
        }
        
        String prompt = String.format(
            "根据以下问题，生成一个详细的假设性答案文档：\n\n%s\n\n要求：\n" +
            "1. 假设你知道答案\n" +
            "2. 用专业术语描述\n" +
            "3. 包含关键概念\n" +
            "4. 200-300字", 
            question
        );
        
        try {
            ChatModel model = createChatModel(config);
            return model.generate(prompt);
        } catch (Exception e) {
            log.warn("HyDE生成失败，使用原问题: {}", e.getMessage());
            return question;
        }
    }
}
```

**示例**:

输入问题:
```
Spring Boot的自动配置原理是什么？
```

HyDE生成的假设文档:
```
Spring Boot的自动配置原理基于@EnableAutoConfiguration注解和spring.factories文件。
系统启动时，Spring Boot会扫描所有jar包中的META-INF/spring.factories文件，
加载其中定义的自动配置类。这些配置类通过@Conditional注解系列实现条件化装配，
只有满足特定条件（如存在某个类、某个Bean等）时才会生效...
```

### 步骤2: 并行检索

#### 向量检索

```java
// EmbeddingDomainService.vectorSearch()
public List<VectorStoreResult> vectorSearch(
    List<String> datasetIds,
    String question,
    int maxResults,
    double minScore
) {
    // 1. 生成查询向量
    Embedding queryEmbedding = embeddingModel.embed(question).content();
    
    // 2. 构建查询请求
    EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
        .filter(new IsIn("dataset_id", datasetIds))
        .maxResults(maxResults)
        .minScore(minScore)
        .queryEmbedding(queryEmbedding)
        .build();
    
    // 3. 执行向量检索
    EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
    
    // 4. 转换结果
    return result.matches().stream()
        .map(match -> new VectorStoreResult(
            match.embedded().metadata().getString("file_id"),
            match.embedded().text(),
            match.score(),
            SearchType.VECTOR
        ))
        .collect(Collectors.toList());
}
```

**SQL执行**（PGVector）:
```sql
SELECT id, embedding, text, metadata,
       1 - (embedding <=> query_vector) AS score
FROM langchain4j_pgvector_embedding
WHERE metadata->>'dataset_id' = ANY($1)
  AND 1 - (embedding <=> query_vector) >= $2
ORDER BY embedding <=> query_vector
LIMIT $3;
```

#### 关键词检索

```java
// KeywordSearchDomainService.keywordSearch()
public List<VectorStoreResult> keywordSearch(
    List<String> datasetIds,
    String question,
    int maxResults
) {
    // 1. 提取关键词
    List<String> keywords = extractKeywords(question);
    
    // 2. 构建全文检索查询
    List<DocumentUnitEntity> results = documentUnitRepository.selectList(
        Wrappers.<DocumentUnitEntity>lambdaQuery()
            .in(DocumentUnitEntity::getFileId, 
                getFileIdsByDatasetIds(datasetIds))
            .and(wrapper -> {
                for (String keyword : keywords) {
                    wrapper.or().like(DocumentUnitEntity::getContent, keyword);
                }
            })
            .orderByDesc(DocumentUnitEntity::getCreateTime)
            .last("LIMIT " + maxResults)
    );
    
    // 3. 转换结果
    return results.stream()
        .map(doc -> new VectorStoreResult(
            doc.getId(),
            doc.getContent(),
            calculateKeywordScore(doc.getContent(), keywords),
            SearchType.KEYWORD
        ))
        .collect(Collectors.toList());
}
```

### 步骤3: RRF融合算法

**RRF公式**: `RRF(d) = Σ(1/(k + rank_i(d)))`, 其中 k=60

```java
private List<VectorStoreResult> fusionWithRRF(
    List<VectorStoreResult> vectorResults,
    List<VectorStoreResult> keywordResults,
    int maxResults
) {
    Map<String, Double> rrfScores = new HashMap<>();
    Map<String, VectorStoreResult> resultMap = new HashMap<>();
    
    // 向量检索结果打分
    for (int i = 0; i < vectorResults.size(); i++) {
        VectorStoreResult result = vectorResults.get(i);
        String id = result.getId();
        double score = 1.0 / (60 + i + 1);
        rrfScores.put(id, score);
        resultMap.put(id, result);
    }
    
    // 关键词检索结果打分（累加）
    for (int i = 0; i < keywordResults.size(); i++) {
        VectorStoreResult result = keywordResults.get(i);
        String id = result.getId();
        double score = 1.0 / (60 + i + 1);
        rrfScores.merge(id, score, Double::sum);
        resultMap.putIfAbsent(id, result);
    }
    
    // 按RRF分数排序
    return rrfScores.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
        .limit(maxResults)
        .map(entry -> {
            VectorStoreResult result = resultMap.get(entry.getKey());
            result.setFusionScore(entry.getValue());
            return result;
        })
        .collect(Collectors.toList());
}
```

**示例**:

```
向量检索结果 (按相似度排序):
1. Doc A (score=0.95)
2. Doc B (score=0.92)
3. Doc C (score=0.88)
4. Doc D (score=0.85)

关键词检索结果 (按匹配度排序):
1. Doc C (score=0.90)
2. Doc A (score=0.85)
3. Doc E (score=0.80)
4. Doc F (score=0.75)

RRF融合:
Doc A: 1/(60+1) + 1/(60+2) = 0.0164 + 0.0161 = 0.0325
Doc C: 1/(60+3) + 1/(60+1) = 0.0159 + 0.0164 = 0.0323
Doc B: 1/(60+2) + 0          = 0.0161
Doc E: 0        + 1/(60+3)   = 0.0159
...

最终排序: Doc A > Doc C > Doc B > E > D > F
```

### 步骤4: Rerank重排序

```java
@Service
public class RerankDomainService {
    
    private final LLMDomainService llmService;
    
    public List<VectorStoreResult> rerank(
        List<VectorStoreResult> results,
        String question
    ) {
        if (results.size() <= 3) {
            return results;  // 结果太少，不需要rerank
        }
        
        // 构建提示词
        StringBuilder prompt = new StringBuilder();
        prompt.append("问题: ").append(question).append("\n\n");
        prompt.append("请对以下文档片段按相关性打分(0-100):\n\n");
        
        for (int i = 0; i < results.size(); i++) {
            prompt.append("【文档").append(i+1).append("】\n");
            prompt.append(results.get(i).getContent()).append("\n\n");
        }
        
        prompt.append("输出JSON格式: [{\"index\": 1, \"score\": 95}, ...]");
        
        // 调用LLM
        try {
            String response = llmService.generate(prompt.toString());
            List<RerankScore> scores = parseRerankScores(response);
            
            // 重新排序
            return reorderByScores(results, scores);
        } catch (Exception e) {
            log.warn("Rerank失败，返回原结果: {}", e.getMessage());
            return results;
        }
    }
}
```

---

## 🗄️ 数据库设计

### ER图

```
file_detail (文件元数据)
    ├─ id (PK)
    ├─ dataset_id (FK → dataset)
    ├─ user_id (FK → user)
    └─ status (状态机)
        ↓ 1:N
document_unit (文档段落)
    ├─ id (PK)
    ├─ file_id (FK)
    ├─ content (原文)
    └─ is_vector (是否已向量化)
        ↓ 1:N (逻辑关联)
langchain4j_pgvector_embedding (向量数据)
    ├─ id (PK)
    ├─ embedding (向量)
    └─ metadata (JSONB)
        ├─ file_id
        ├─ dataset_id
        └─ page
```

### 建表SQL

```sql
-- 1. 文件详情表
CREATE TABLE file_detail (
    id VARCHAR(64) PRIMARY KEY,
    url VARCHAR(512) NOT NULL,
    original_filename VARCHAR(255),
    ext VARCHAR(32),
    file_page_size INTEGER DEFAULT 0,
    dataset_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    
    -- 状态机字段
    status VARCHAR(32) DEFAULT 'PENDING',
    ocr_progress INTEGER DEFAULT 0,
    embedding_progress INTEGER DEFAULT 0,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_file_detail_dataset ON file_detail(dataset_id);
CREATE INDEX idx_file_detail_user ON file_detail(user_id);
CREATE INDEX idx_file_detail_status ON file_detail(status);

-- 2. 文档单元表
CREATE TABLE document_unit (
    id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64) NOT NULL,
    page INTEGER NOT NULL,
    content TEXT,
    is_ocr BOOLEAN DEFAULT FALSE,
    is_vector BOOLEAN DEFAULT FALSE,
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_unit_file ON document_unit(file_id);
CREATE INDEX idx_document_unit_page ON document_unit(file_id, page);
CREATE INDEX idx_document_unit_vector ON document_unit(file_id, is_vector);
CREATE INDEX idx_document_unit_ocr ON document_unit(file_id, is_ocr);

-- 3. PGVector扩展和向量表
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE langchain4j_pgvector_embedding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    embedding VECTOR(1536) NOT NULL,
    text TEXT,
    metadata JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- HNSW索引（推荐）
CREATE INDEX ON langchain4j_pgvector_embedding 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- GIN索引加速metadata查询
CREATE INDEX ON langchain4j_pgvector_embedding 
USING gin (metadata);

-- metadata字段索引
CREATE INDEX ON langchain4j_pgvector_embedding 
((metadata->>'dataset_id'));
CREATE INDEX ON langchain4j_pgvector_embedding 
((metadata->>'file_id'));
```

### 索引优化

#### HNSW vs IVFFlat

| 索引类型 | 构建时间 | 查询速度 | 准确率 | 推荐场景 |
|---------|---------|---------|--------|---------|
| **HNSW** | 慢 | 快 | 高 | 生产环境（推荐） |
| **IVFFlat** | 快 | 中 | 中 | 开发测试 |

**HNSW参数**:
- `m`: 每个节点的连接数，默认16，越大越准确但内存占用越多
- `ef_construction`: 构建时的搜索深度，默认64，越大构建越慢但质量越高

**查询时参数**:
```sql
SET hnsw.ef_search = 100;  -- 查询时的搜索深度
```

---

## 🚀 部署指南

### 1. 环境准备

#### 系统要求

- **操作系统**: Linux (Ubuntu 20.04+ / CentOS 7+)
- **Java**: OpenJDK 17+
- **PostgreSQL**: 15+ with PGVector
- **RabbitMQ**: 3.11+
- **内存**: 最低8GB，推荐16GB+
- **磁盘**: 根据文档量，建议100GB+

#### Docker Compose部署

```yaml
version: '3.8'

services:
  postgres:
    image: pgvector/pgvector:pg15
    environment:
      POSTGRES_DB: agentx
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: your_password
    ports:
      - "5432:5432"
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
    command: postgres -c shared_buffers=256MB -c max_connections=200
  
  rabbitmq:
    image: rabbitmq:3.11-management
    environment:
      RABBITMQ_DEFAULT_USER: guest
      RABBITMQ_DEFAULT_PASS: guest
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - ./data/rabbitmq:/var/lib/rabbitmq
  
  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - ./data/minio:/data
  
  agentx:
    image: agentx:latest
    depends_on:
      - postgres
      - rabbitmq
      - minio
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/agentx
      SPRING_RABBITMQ_HOST: rabbitmq
      MINIO_ENDPOINT: http://minio:9000
    volumes:
      - ./logs:/app/logs
```

启动:
```bash
docker-compose up -d
```

### 2. 应用配置

#### application-prod.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/agentx
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  
  rabbitmq:
    host: ${MQ_HOST:localhost}
    port: 5672
    username: ${MQ_USER:guest}
    password: ${MQ_PASSWORD:guest}
    listener:
      simple:
        concurrency: 5
        max-concurrency: 10
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000

rag:
  vector:
    max-length: 1800
    min-length: 200
    overlap-size: 100
  
  search:
    max-results: 15
    min-score: 0.7
    enable-rerank: true
    enable-hyde: true

logging:
  level:
    root: INFO
    org.xhy: DEBUG
  file:
    name: logs/agentx.log
    max-size: 100MB
    max-history: 30
```

### 3. 启动应用

```bash
# 1. 构建
./mvnw clean package -DskipTests

# 2. 运行
java -jar -Xms2g -Xmx4g \
  -Dspring.profiles.active=prod \
  target/agentx-1.0.0.jar

# 或使用systemd
sudo systemctl start agentx
```

### 4. 健康检查

```bash
# 健康检查接口
curl http://localhost:8080/actuator/health

# 查看metrics
curl http://localhost:8080/actuator/metrics
```

---

## ⚡ 性能优化

### 1. 数据库优化

#### PostgreSQL配置

```ini
# postgresql.conf
shared_buffers = 4GB              # 物理内存的25%
effective_cache_size = 12GB       # 物理内存的75%
maintenance_work_mem = 1GB        # 索引构建内存
work_mem = 64MB                   # 单个查询内存
max_connections = 200             # 最大连接数

# 向量检索优化
hnsw.ef_search = 100              # 查询精度
```

#### 索引优化

```sql
-- 定期VACUUM
VACUUM ANALYZE langchain4j_pgvector_embedding;

-- 重建索引（如果性能下降）
REINDEX INDEX CONCURRENTLY idx_embedding_hnsw;
```

### 2. 应用层优化

#### 连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

#### RabbitMQ优化

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 10          # 并发消费者数
        max-concurrency: 20      # 最大并发
        prefetch: 5              # 预取消息数
```

### 3. 批量处理

```java
// 批量向量化
public void batchVectorize(List<DocumentUnitEntity> units) {
    List<String> contents = units.stream()
        .map(DocumentUnitEntity::getContent)
        .collect(Collectors.toList());
    
    // 批量调用embedding API
    List<Embedding> embeddings = embeddingModel.embedAll(contents).content();
    
    // 批量存储
    for (int i = 0; i < units.size(); i++) {
        // ... 存储逻辑
    }
}
```

### 4. 缓存策略

```java
@Cacheable(value = "rag-search", key = "#question + #datasetIds")
public List<DocumentUnitEntity> hybridSearch(...) {
    // 检索逻辑
}
```

---

## 📊 监控与运维

### 1. 关键指标

| 指标 | 说明 | 告警阈值 |
|-----|------|---------|
| **文档处理延迟** | 上传到完成的时间 | >5分钟 |
| **向量化成功率** | 成功/总数 | <95% |
| **检索延迟** | P95 | >500ms |
| **MQ堆积** | 待处理消息数 | >1000 |
| **数据库连接池** | 活跃连接数 | >80% |

### 2. 日志监控

```bash
# 查看处理日志
tail -f logs/agentx.log | grep "阶段1完成"

# 查看错误日志
tail -f logs/agentx.log | grep ERROR

# 统计处理速度
tail -f logs/agentx.log | grep "向量化完成" | wc -l
```

### 3. 数据库监控

```sql
-- 查看慢查询
SELECT query, mean_exec_time, calls
FROM pg_stat_statements
WHERE mean_exec_time > 1000
ORDER BY mean_exec_time DESC
LIMIT 10;

-- 查看表大小
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

---

## 🔧 常见问题

### Q1: 向量化速度慢怎么办？

**A**: 
1. 增加RabbitMQ消费者并发度
2. 批量调用Embedding API
3. 使用多线程处理

### Q2: 检索准确率低怎么优化？

**A**:
1. 启用HyDE和Rerank
2. 调整minScore阈值
3. 增加候选结果数量再融合
4. 优化文档分割策略

### Q3: 如何备份和恢复？

**A**:
```bash
# 备份PostgreSQL
pg_dump agentx > backup.sql

# 恢复
psql agentx < backup.sql

# 备份PGVector数据
pg_dump -t langchain4j_pgvector_embedding agentx > vectors_backup.sql
```

---

## 📚 系列文档回顾

1. ✅ 01-RAG系统架构概览.md - 整体架构和设计理念
2. ✅ 02-Markdown文档处理流程.md - 两阶段处理详解
3. ✅ 03-核心组件实现详解.md - 代码实现细节
4. ✅ 04-RAG检索与部署指南.md - 检索算法和部署运维

---

**© 2025 AgentX RAG System | 祝你实现成功！**

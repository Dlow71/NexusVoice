# RAG知识库系统深度教学（下篇） - 工程实现与优化技巧

> 📚 **本文目标**：深入工程实现细节，掌握性能优化、故障处理、生产部署的最佳实践

---

## 🎯 第七章：向量化系统工程实现

### 7.1 Embedding模型选择策略

```java
/**
 * 为什么需要支持多种Embedding模型？
 * 1. 成本考虑：OpenAI很贵，本地模型免费
 * 2. 隐私考虑：敏感数据不能发到云端
 * 3. 性能考虑：本地模型延迟低
 * 4. 可用性：OpenAI可能被墙或限流
 */
@Component
public class EmbeddingModelFactory {
    
    // 模型性能对比（基于MTEB基准测试）
    private static final Map<String, ModelSpec> MODEL_SPECS = Map.of(
        "text-embedding-ada-002", new ModelSpec(
            1536,        // 维度
            8191,        // 最大token
            0.0001,      // 价格$/1K tokens
            0.95         // 准确率
        ),
        "text-embedding-3-small", new ModelSpec(
            512,         // 维度（可配置）
            8191,        // 最大token
            0.00002,     // 便宜5倍！
            0.92         // 准确率略低
        ),
        "bge-large-zh", new ModelSpec(
            1024,        // 维度
            512,         // 最大token
            0,           // 本地免费
            0.90         // 中文效果好
        )
    );
    
    /**
     * 创建Embedding模型（支持多种协议）
     */
    public OpenAiEmbeddingModel createEmbeddingModel(EmbeddingConfig config) {
        // 1. 优先使用用户配置
        if (config != null && config.getApiKey() != null) {
            return OpenAiEmbeddingModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .dimensions(selectOptimalDimensions(config.getModelName()))
                .timeout(Duration.ofSeconds(30))
                .maxRetries(3)  // 自动重试
                .build();
        }
        
        // 2. 回退到默认模型
        return createDefaultModel();
    }
    
    /**
     * 维度选择策略
     * 为什么要考虑维度？
     * - 高维度：准确但慢、占用空间大
     * - 低维度：快但准确率略低
     */
    private Integer selectOptimalDimensions(String modelName) {
        // text-embedding-3-small支持动态维度
        if (modelName.equals("text-embedding-3-small")) {
            // 根据数据量自动选择
            long vectorCount = getVectorCount();
            if (vectorCount > 1_000_000) {
                return 512;  // 大规模用低维度
            } else {
                return 1536; // 小规模用高维度
            }
        }
        
        return null; // 其他模型用默认维度
    }
}
```

### 7.2 批量向量化优化

```java
@Service
public class EmbeddingDomainService {
    
    /**
     * 批量向量化的重要性：
     * - API调用次数减少100倍
     * - 成本降低（按token计费，不是按请求）
     * - 速度提升10倍
     */
    public void batchEmbedDocuments(List<DocumentUnitEntity> documents) {
        // 1. 分批处理（OpenAI限制：每批最多2048个）
        int batchSize = 100;  // 经验值，平衡速度和稳定性
        
        for (int i = 0; i < documents.size(); i += batchSize) {
            List<DocumentUnitEntity> batch = documents.subList(
                i, 
                Math.min(i + batchSize, documents.size())
            );
            
            try {
                processBatch(batch);
            } catch (RateLimitException e) {
                // 限流处理：指数退避
                handleRateLimit(batch, e);
            } catch (Exception e) {
                // 失败处理：单个处理
                processSingleDocuments(batch);
            }
        }
    }
    
    /**
     * 限流处理策略（指数退避算法）
     */
    private void handleRateLimit(List<DocumentUnitEntity> batch, RateLimitException e) {
        int retryCount = 0;
        int maxRetries = 5;
        
        while (retryCount < maxRetries) {
            // 指数退避：1s, 2s, 4s, 8s, 16s
            int waitTime = (int) Math.pow(2, retryCount);
            
            log.info("限流触发，等待{}秒后重试", waitTime);
            Thread.sleep(waitTime * 1000);
            
            try {
                processBatch(batch);
                break;  // 成功则退出
            } catch (RateLimitException e2) {
                retryCount++;
            }
        }
        
        if (retryCount >= maxRetries) {
            // 多次重试失败，降级到单个处理
            log.warn("批量处理多次失败，降级到单个处理");
            processSingleDocuments(batch);
        }
    }
    
    /**
     * 向量存储优化
     */
    private void storeVectors(List<Embedding> embeddings, List<TextSegment> segments) {
        // 使用批量插入而不是循环插入
        String sql = """
            INSERT INTO embeddings (id, vector, metadata, created_at)
            VALUES (?, ?::vector, ?::jsonb, ?)
            ON CONFLICT (id) DO UPDATE SET 
                vector = EXCLUDED.vector,
                updated_at = now()
            """;
        
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) {
                Embedding embedding = embeddings.get(i);
                TextSegment segment = segments.get(i);
                
                ps.setString(1, embedding.id());
                ps.setArray(2, createSqlArray(embedding.vector()));
                ps.setObject(3, segment.metadata());
                ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            }
            
            @Override
            public int getBatchSize() {
                return embeddings.size();
            }
        });
    }
}
```

---

## 🔄 第八章：Rerank重排序系统

### 8.1 为什么需要Rerank？

```java
/**
 * Rerank解决的核心问题：
 * 向量相似度 ≠ 语义相关性
 * 
 * 例子：
 * 问题："如何修改密码？"
 * 
 * 向量检索结果：
 * 1. "系统安全性很重要" (0.85) - 语义相似但不相关
 * 2. "点击设置-安全-修改密码" (0.75) - 分数低但是答案
 * 
 * Rerank后：
 * 1. "点击设置-安全-修改密码" (0.95) ✅
 * 2. "系统安全性很重要" (0.30)
 */
@Service
public class RerankDomainService {
    
    private final RerankModelFactory modelFactory;
    
    /**
     * 重排序实现（支持多种模型）
     */
    public List<Integer> rerank(List<String> documents, String query) {
        // 1. 选择Rerank模型
        RerankModel model = selectRerankModel();
        
        // 2. 构建重排序请求
        RerankRequest request = RerankRequest.builder()
            .query(query)
            .documents(documents)
            .topN(Math.min(documents.size(), 20))  // 只返回Top 20
            .build();
        
        // 3. 执行重排序
        RerankResponse response = model.rerank(request);
        
        // 4. 返回重排序后的索引
        return response.getResults().stream()
            .filter(r -> r.getRelevanceScore() > 0.3)  // 过滤低分
            .map(RerankResult::getIndex)
            .collect(Collectors.toList());
    }
    
    /**
     * 模型选择策略
     */
    private RerankModel selectRerankModel() {
        // 优先级：
        // 1. Cohere Rerank (效果最好)
        // 2. BGE-Reranker (本地免费)
        // 3. Cross-Encoder (备选)
        
        if (isCohereAvailable()) {
            return new CohereRerankModel(apiKey);
        } else if (isLocalModelAvailable()) {
            return new BGERerankerModel();  // 本地模型
        } else {
            return new CrossEncoderModel();  // 轻量级备选
        }
    }
}
```

### 8.2 Rerank性能优化

```java
/**
 * Rerank是计算密集型操作，需要优化
 */
@Component
public class RerankOptimizer {
    
    // 缓存最近的重排序结果
    private final Cache<String, List<Integer>> rerankCache = 
        Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    
    /**
     * 带缓存的重排序
     */
    public List<Integer> rerankWithCache(
            List<String> documents, 
            String query) {
        
        // 1. 生成缓存键
        String cacheKey = generateCacheKey(documents, query);
        
        // 2. 检查缓存
        List<Integer> cached = rerankCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Rerank缓存命中");
            return cached;
        }
        
        // 3. 执行重排序
        List<Integer> result = doRerank(documents, query);
        
        // 4. 写入缓存
        rerankCache.put(cacheKey, result);
        
        return result;
    }
    
    /**
     * 分级重排序（性能优化）
     * 思路：先粗排，再精排
     */
    public List<DocumentResult> tieredRerank(
            List<DocumentResult> candidates, 
            String query) {
        
        // 第一级：快速粗排（基于关键词匹配）
        List<DocumentResult> tier1 = candidates.stream()
            .filter(doc -> containsKeywords(doc, query))
            .limit(50)  // 保留Top 50
            .collect(Collectors.toList());
        
        // 第二级：精细重排（使用Rerank模型）
        if (tier1.size() > 10) {
            List<String> texts = tier1.stream()
                .map(DocumentResult::getText)
                .collect(Collectors.toList());
            
            List<Integer> rerankedIndices = rerankModel.rerank(texts, query);
            
            // 重新排序
            return rerankedIndices.stream()
                .map(tier1::get)
                .collect(Collectors.toList());
        }
        
        return tier1;
    }
}
```

---

## ⚡ 第九章：性能优化最佳实践

### 9.1 向量检索性能优化

```java
/**
 * PGVector性能优化技巧
 */
@Configuration
public class VectorDBOptimization {
    
    /**
     * 1. 索引优化（HNSW vs IVFFlat）
     */
    @PostConstruct
    public void createOptimalIndex() {
        // HNSW索引：准确率高，构建慢，查询快
        jdbcTemplate.execute("""
            CREATE INDEX IF NOT EXISTS embedding_hnsw_idx 
            ON embeddings 
            USING hnsw (embedding vector_cosine_ops)
            WITH (m = 16, ef_construction = 64);
            """);
        
        // 设置查询参数
        jdbcTemplate.execute("SET hnsw.ef_search = 100;");
    }
    
    /**
     * 2. 分区表优化（数据量>100万时）
     */
    public void createPartitionedTable() {
        // 按数据集ID分区，提升查询效率
        jdbcTemplate.execute("""
            CREATE TABLE embeddings_partitioned (
                LIKE embeddings INCLUDING ALL
            ) PARTITION BY LIST (dataset_id);
            
            -- 为每个数据集创建分区
            CREATE TABLE embeddings_dataset_1 
            PARTITION OF embeddings_partitioned
            FOR VALUES IN ('dataset_1');
            """);
    }
    
    /**
     * 3. 查询优化
     */
    public List<VectorResult> optimizedVectorSearch(
            float[] queryVector, 
            String datasetId, 
            int limit) {
        
        // 使用预编译语句，避免SQL注入和重复解析
        String sql = """
            WITH vector_search AS (
                SELECT 
                    id,
                    1 - (embedding <=> ?::vector) as similarity,
                    metadata
                FROM embeddings
                WHERE dataset_id = ?
                    AND 1 - (embedding <=> ?::vector) > ?  -- 预过滤
                ORDER BY embedding <=> ?::vector
                LIMIT ?
            )
            SELECT * FROM vector_search
            WHERE similarity > 0.5;  -- 后过滤
            """;
        
        return jdbcTemplate.query(sql, ps -> {
            Array vectorArray = ps.getConnection()
                .createArrayOf("float4", queryVector);
            ps.setArray(1, vectorArray);
            ps.setString(2, datasetId);
            ps.setArray(3, vectorArray);
            ps.setFloat(4, 0.5f);  // 最低相似度
            ps.setArray(5, vectorArray);
            ps.setInt(6, limit * 2);  // 获取2倍候选
        }, (rs, rowNum) -> mapToVectorResult(rs));
    }
}
```

### 9.2 并发处理优化

```java
@Configuration
public class ConcurrencyOptimization {
    
    /**
     * 自定义线程池配置
     */
    @Bean
    public ThreadPoolTaskExecutor ragProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数 = CPU核心数
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        
        // 最大线程数 = CPU核心数 * 2
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        
        // 队列容量
        executor.setQueueCapacity(1000);
        
        // 线程名前缀
        executor.setThreadNamePrefix("rag-processing-");
        
        // 拒绝策略：调用者线程执行
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
    
    /**
     * 并发处理文档
     */
    @Service
    public class ParallelDocumentProcessor {
        
        @Autowired
        private ThreadPoolTaskExecutor executor;
        
        public void processDocuments(List<Document> documents) {
            // 使用CompletableFuture并发处理
            List<CompletableFuture<Void>> futures = documents.stream()
                .map(doc -> CompletableFuture.runAsync(
                    () -> processDocument(doc), 
                    executor
                ))
                .collect(Collectors.toList());
            
            // 等待所有任务完成
            CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            ).join();
        }
    }
}
```

---

## 🔧 第十章：故障处理与监控

### 10.1 优雅的故障处理

```java
@Component
public class RagFaultTolerance {
    
    /**
     * 多级降级策略
     */
    @Service
    public class DegradationService {
        
        public List<DocumentResult> searchWithDegradation(
                String query, 
                List<String> datasetIds) {
            
            try {
                // Level 1: 完整的混合检索
                return hybridSearch(query, datasetIds);
                
            } catch (VectorDBException e) {
                log.warn("向量库异常，降级到关键词检索", e);
                
                try {
                    // Level 2: 仅关键词检索
                    return keywordOnlySearch(query, datasetIds);
                    
                } catch (Exception e2) {
                    log.error("关键词检索也失败，降级到缓存", e2);
                    
                    // Level 3: 返回缓存结果
                    return getCachedResults(query);
                }
            }
        }
        
        /**
         * 熔断器模式
         */
        @CircuitBreaker(
            name = "embedding-service",
            fallbackMethod = "embeddingFallback"
        )
        public float[] generateEmbedding(String text) {
            return embeddingService.embed(text);
        }
        
        public float[] embeddingFallback(String text, Exception e) {
            log.warn("Embedding服务熔断，使用本地模型", e);
            return localEmbeddingModel.embed(text);
        }
    }
    
    /**
     * 重试机制
     */
    @Retryable(
        value = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void processDocumentWithRetry(Document doc) {
        // 处理逻辑，失败自动重试
    }
}
```

### 10.2 监控指标

```java
@Component
public class RagMetrics {
    
    private final MeterRegistry registry;
    
    /**
     * 关键指标监控
     */
    public void recordMetrics() {
        // 1. 文档处理指标
        registry.counter("rag.documents.processed").increment();
        registry.timer("rag.document.processing.time").record(() -> {
            // 处理逻辑
        });
        
        // 2. 检索性能指标
        registry.timer("rag.search.latency").record(Duration.ofMillis(searchTime));
        registry.counter("rag.search.requests").increment();
        
        // 3. 向量库指标
        registry.gauge("rag.vectordb.size", vectorCount);
        registry.gauge("rag.vectordb.query.qps", getCurrentQPS());
        
        // 4. 准确率指标（需要人工标注）
        registry.gauge("rag.search.precision", calculatePrecision());
        registry.gauge("rag.search.recall", calculateRecall());
    }
    
    /**
     * 健康检查
     */
    @Component
    public class RagHealthIndicator implements HealthIndicator {
        
        @Override
        public Health health() {
            try {
                // 检查向量库连接
                boolean vectorDbHealthy = checkVectorDB();
                
                // 检查Embedding服务
                boolean embeddingHealthy = checkEmbeddingService();
                
                // 检查存储空间
                boolean storageHealthy = checkStorage();
                
                if (vectorDbHealthy && embeddingHealthy && storageHealthy) {
                    return Health.up()
                        .withDetail("vectorDB", "UP")
                        .withDetail("embedding", "UP")
                        .withDetail("storage", getStorageInfo())
                        .build();
                } else {
                    return Health.down()
                        .withDetail("vectorDB", vectorDbHealthy ? "UP" : "DOWN")
                        .withDetail("embedding", embeddingHealthy ? "UP" : "DOWN")
                        .build();
                }
            } catch (Exception e) {
                return Health.down(e).build();
            }
        }
    }
}
```

---

## 🎓 核心经验总结

### 成功要素

1. **架构设计**
   - 离线/在线分离
   - 异步处理
   - 多级缓存

2. **算法选择**
   - 混合检索 > 单一检索
   - RRF融合简单有效
   - Rerank显著提升准确率

3. **工程实践**
   - 批量处理
   - 并发优化
   - 故障降级

4. **成本控制**
   - 选择合适的模型
   - 缓存热点数据
   - 按需计算

### 常见坑点

1. **切片太大**：检索不精确
2. **切片太小**：丢失上下文
3. **不做去重**：重复内容影响效果
4. **忽略Rerank**：准确率低
5. **同步处理**：用户体验差

### 性能指标参考

- 文档处理：10页/秒
- 向量化：100个切片/秒
- 检索延迟：<500ms
- 准确率：>85%
- 召回率：>80%

---

> 💡 **下一步**：查看[06-RAG实战实现指南](./06-RAG实战实现指南.md)，获取可直接使用的代码模板

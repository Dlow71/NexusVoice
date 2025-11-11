# NexusVoice RAG系统实施规划总览

> **规划版本**: v1.0  
> **目标**: 实现Markdown文档的RAG知识库系统，预留PDF/Word等格式扩展能力  
> **技术栈**: Spring Boot 3.3.5 + RocketMQ + PGVector + LangChain4j 0.35.0

---

## 📊 现状分析

### ✅ 已有基础设施

#### 1. 消息队列
- **RocketMQ 2.3.0**（已集成）
- 支持延迟消息、顺序消息、批量发送

#### 2. 向量化能力
- **DynamicAiEmbeddingBeanManager**：动态Embedding模型管理
- 已集成BCE、Qwen等Embedding模型
- 支持模型热更新、密钥池、费用统计

#### 3. 数据库能力
- **PostgreSQL + PGVector扩展**
- LangChain4j PGVector集成（0.35.0）

#### 4. 文档处理依赖
- ✅ Apache POI 5.4.0（Word/Excel）
- ✅ Apache PDFBox 3.0.2（PDF）
- ✅ Apache Tika 2.6.0（文件类型检测）
- ❌ **缺失Flexmark**（Markdown解析器）⚠️

#### 5. 领域模型（完整）
- **11个实体类**：KnowledgeBase、FileDetail、DocumentUnit、VectorStore等
- **7个枚举类**：FileType、ProcessStatus、ParseStrategy等
- **11个Repository接口**
- **5个领域服务接口**

### ⚠️ 需要补充的核心功能

#### 1. Markdown处理组件
- FlexmarkParser（AST解析器）
- DocumentTreeBuilder（文档树构建）
- StructuralMarkdownProcessor（结构化处理）
- MarkdownAstRewriter（AST重写器）
- MarkdownContentSplitter（智能分割）

#### 2. 异步处理组件
- RagDocumentProcessConsumer（文档处理消费者）
- DocumentVectorizationConsumer（向量化消费者）
- DocumentVectorizationOrchestrator（编排器）

#### 3. 检索增强组件
- HyDEService（假设文档生成）
- HybridSearchService（混合检索）
- RRFAlgorithm（RRF融合算法）
- RerankService（重排序）

---

## 🏗️ 核心架构设计

### 两阶段处理架构

```
【用户上传MD】
    ↓
【保存文件 + 发MQ】→ RocketMQ(DOC_PROCESS_TOPIC)
    ↓
【阶段1：结构化解析与原文分割】
    - Flexmark解析AST
    - 按标题层级分段
    - 保存DocumentUnit（原文）
    - is_vector = false
    ↓
    发送MQ → RocketMQ(DOC_VECTORIZE_TOPIC)
    ↓
【阶段2：翻译与智能二次分割】
    - 翻译代码块→自然语言
    - 翻译表格→结构化文本
    - OCR图片→GPT-4V描述
    - 智能分割（段落/句子/强制）
    - 重叠100字符
    ↓
【向量化与存储】
    - DynamicAiEmbeddingBeanManager生成向量
    - 存储到PGVector
    - 更新is_vector = true
    ↓
【完成】
```

### 策略模式（支持多格式扩展）

```java
// 策略接口
public interface DocumentProcessingStrategy {
    FileType supportedFileType();
    List<DocumentUnit> parseAndSplit(FileDetail fileDetail, byte[] fileBytes);
    boolean needsTranslationEnhancement();
}

// Markdown策略
@Component("markdownProcessingStrategy")
public class MarkdownProcessingStrategy implements DocumentProcessingStrategy {
    // Flexmark解析 + 两阶段处理
}

// PDF策略（预留）
@Component("pdfProcessingStrategy")
public class PdfProcessingStrategy implements DocumentProcessingStrategy {
    // PDFBox解析
}

// 策略工厂
@Component
public class DocumentProcessingStrategyFactory {
    public DocumentProcessingStrategy getStrategy(FileType fileType) {
        // 根据文件类型返回策略
    }
}
```

---

## 📦 缺失组件清单

### 1. Maven依赖

#### ⚠️ 必须添加

```xml
<!-- Flexmark Markdown解析器 -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>0.64.8</version>
</dependency>
```

### 2. Domain层缺失

#### 新增值对象
- `MarkdownNode`：Markdown节点值对象
- `DocumentTree`：文档树值对象
- `ProcessedSegment`：处理后的段落
- `SearchRequest`：检索请求
- `SearchResult`：检索结果

#### 新增领域服务接口
- `MarkdownProcessingDomainService`
- `HybridSearchDomainService`
- `RerankDomainService`

### 3. Infrastructure层缺失

```
infrastructure/rag/
├── markdown/                           # Markdown处理
│   ├── FlexmarkParser.java
│   ├── DocumentTreeBuilder.java
│   ├── StructuralMarkdownProcessor.java
│   ├── MarkdownAstRewriter.java
│   └── MarkdownContentSplitter.java
├── strategy/                           # 策略模式
│   ├── DocumentProcessingStrategy.java
│   ├── MarkdownProcessingStrategy.java
│   ├── PdfProcessingStrategy.java
│   └── DocumentProcessingStrategyFactory.java
├── vectorization/                      # 向量化
│   ├── DocumentVectorizationOrchestrator.java
│   └── NodeTranslatorService.java
├── search/                             # 检索增强
│   ├── HybridSearchService.java
│   ├── HyDEService.java
│   ├── RerankService.java
│   └── RRFAlgorithm.java
└── consumer/                           # RocketMQ消费者
    ├── RagDocumentProcessConsumer.java
    └── DocumentVectorizationConsumer.java
```

### 4. Application层缺失

```
application/rag/
├── service/
│   ├── RagDocumentApplicationService.java
│   ├── RagSearchApplicationService.java
│   └── VectorStoreApplicationService.java
├── dto/
│   ├── DocumentUploadRequestDto.java
│   ├── RagSearchRequestDto.java
│   └── RagSearchResponseDto.java
└── assembler/
    └── RagAssembler.java
```

### 5. Interfaces层缺失

```
interfaces/api/rag/
├── RagDocumentController.java
└── RagSearchController.java
```

---

## 🎯 实施优先级

### P0（必须实现）- Markdown处理核心

#### 第1周：基础准备
- [ ] 添加Flexmark依赖
- [ ] 配置RocketMQ主题
- [ ] 验证PGVector集成

#### 第2-3周：阶段1实现
- [ ] Domain层值对象（MarkdownNode、DocumentTree、ProcessedSegment）
- [ ] FlexmarkParser实现
- [ ] DocumentTreeBuilder实现
- [ ] StructuralMarkdownProcessor实现
- [ ] MarkdownProcessingStrategy实现
- [ ] 策略工厂实现
- [ ] RagDocumentProcessConsumer实现

#### 第4-5周：阶段2实现
- [ ] MarkdownAstRewriter实现（翻译）
- [ ] NodeTranslatorService实现
- [ ] MarkdownContentSplitter实现（智能分割）
- [ ] DocumentVectorizationOrchestrator实现
- [ ] VectorStoreApplicationService实现
- [ ] DocumentVectorizationConsumer实现

#### 第6周：Application和API
- [ ] RagDocumentApplicationService实现
- [ ] RagDocumentController实现（上传API）
- [ ] 端到端测试

### P1（重要功能）- 检索增强

#### 第7-8周
- [ ] HyDEService实现
- [ ] 向量检索实现（LangChain4j PGVector）
- [ ] 关键词检索实现（PostgreSQL全文检索）
- [ ] RRF融合算法实现
- [ ] HybridSearchService实现
- [ ] RerankService实现
- [ ] RagSearchApplicationService实现
- [ ] RagSearchController实现（检索API）

### P2（优化功能）- 扩展支持

#### 第9-10周
- [ ] PDF策略实现（PdfProcessingStrategy）
- [ ] Word策略实现（DocxProcessingStrategy）
- [ ] 批量处理优化
- [ ] 性能优化和缓存

---

## 🔧 配置示例

### application-rag.yml

```yaml
rag:
  markdown:
    segment-split:
      enabled: true
      max-length: 1800        # 最大分段长度
      min-length: 200         # 最小分段长度
      overlap-size: 100       # 重叠大小
  
  vector:
    max-length: 1800
    overlap-size: 100
    default-model: "siliconflow:bce-embedding-base_v1"  # 默认向量模型
  
  search:
    max-results: 15
    min-score: 0.7
    enable-hyde: true         # 启用HyDE
    enable-rerank: true       # 启用Rerank
    rerank-model: "siliconflow:bge-reranker-v2-m3"

rocketmq:
  topic:
    doc-process: DOC_PROCESS_TOPIC
    doc-vectorize: DOC_VECTORIZE_TOPIC
  consumer:
    doc-process-group: doc-process-consumer-group
    doc-vectorize-group: doc-vectorize-consumer-group
```

---

## 📚 相关文档

1. **01-RAG系统架构概览.md** - AgentX架构参考
2. **02-Markdown文档处理流程.md** - 两阶段处理详解
3. **03-核心组件实现详解.md** - 代码实现参考
4. **04-RAG检索与部署指南.md** - 检索算法和部署

---

## ✅ 总结

### 核心优势
1. **完全复用现有基础设施**：RocketMQ、DynamicAiEmbeddingBeanManager、PGVector
2. **纯血DDD架构**：Domain层零基础设施依赖
3. **策略模式**：轻松扩展PDF、Word等格式
4. **两阶段处理**：保留原文 + 翻译增强
5. **完整的检索栈**：HyDE + 向量 + 关键词 + RRF + Rerank

### 关键决策
1. ✅ 使用Flexmark而非CommonMark（功能更强大）
2. ✅ 使用RocketMQ而非RabbitMQ（已集成）
3. ✅ 使用LangChain4j PGVector而非原生pgvector（更易集成）
4. ✅ 策略模式而非if-else（易扩展）
5. ✅ 两阶段处理而非一次性（保留原文，增强检索）

### 预计工作量
- **P0（必须）**: 6周，约120小时
- **P1（重要）**: 2周，约40小时
- **P2（优化）**: 2周，约40小时
- **总计**: 10周，约200小时

---

**© 2025 NexusVoice | RAG系统规划 v1.0**

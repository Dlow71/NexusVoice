# NexusVoice RAG系统 - 缺失组件详细清单

> **文档版本**: v1.0  
> **用途**: 列出所有需要新增的组件，提供代码骨架和实现建议

---

## 📦 1. Maven依赖（必需）

### pom.xml添加

```xml
<!-- Flexmark Markdown解析器 -->
<dependency>
    <groupId>com.vladsch.flexmark</groupId>
    <artifactId>flexmark-all</artifactId>
    <version>0.64.8</version>
</dependency>
```

---

## 🎨 2. Domain层 - 值对象

### 2.1 MarkdownNode.java

**路径**: `domain/rag/model/vo/MarkdownNode.java`

```java
package com.nexusvoice.domain.rag.model.vo;

import java.util.Map;

/**
 * Markdown节点值对象
 */
public class MarkdownNode {
    
    public enum NodeType {
        HEADING,        // 标题
        PARAGRAPH,      // 段落
        CODE_BLOCK,     // 代码块
        TABLE,          // 表格
        IMAGE,          // 图片
        LIST,           // 列表
        BLOCKQUOTE      // 引用
    }
    
    private final NodeType type;
    private final int level;                    // 标题层级（1-6），其他类型为0
    private final String content;               // 节点内容
    private final String language;              // 代码语言（仅代码块）
    private final Map<String, String> attributes;
    
    // 构造函数
    public MarkdownNode(NodeType type, int level, String content, 
                        String language, Map<String, String> attributes) {
        this.type = type;
        this.level = level;
        this.content = content;
        this.language = language;
        this.attributes = attributes;
    }
    
    // 业务方法
    
    /**
     * 是否为标题
     */
    public boolean isHeading() {
        return type == NodeType.HEADING;
    }
    
    /**
     * 是否为一级标题
     */
    public boolean isH1() {
        return isHeading() && level == 1;
    }
    
    /**
     * 是否为代码块
     */
    public boolean isCodeBlock() {
        return type == NodeType.CODE_BLOCK;
    }
    
    /**
     * 是否为表格
     */
    public boolean isTable() {
        return type == NodeType.TABLE;
    }
    
    /**
     * 是否需要翻译增强
     */
    public boolean needsTranslation() {
        return isCodeBlock() || isTable() || type == NodeType.IMAGE;
    }
    
    /**
     * 获取内容长度
     */
    public int length() {
        return content != null ? content.length() : 0;
    }
    
    // Getters
    // ... 省略getter方法
}
```

### 2.2 DocumentTree.java

**路径**: `domain/rag/model/vo/DocumentTree.java`

```java
package com.nexusvoice.domain.rag.model.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档树值对象
 */
public class DocumentTree {
    
    private final List<MarkdownNode> nodes;
    private final SegmentSplitConfig config;
    
    public DocumentTree(SegmentSplitConfig config) {
        this.nodes = new ArrayList<>();
        this.config = config;
    }
    
    /**
     * 添加节点
     */
    public void addNode(MarkdownNode node) {
        nodes.add(node);
    }
    
    /**
     * 执行层次化分割
     */
    public List<ProcessedSegment> performHierarchicalSplit() {
        List<ProcessedSegment> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentLength = 0;
        
        for (MarkdownNode node : nodes) {
            String content = node.getContent();
            int length = node.length();
            
            // 一级标题强制分段
            if (node.isH1() && currentLength > 0) {
                segments.add(createSegment(current.toString(), segments.size()));
                current.setLength(0);
                currentLength = 0;
            }
            
            // 长度超限分段
            if (currentLength + length > config.getMaxLength() 
                && currentLength > config.getMinLength()) {
                segments.add(createSegment(current.toString(), segments.size()));
                current.setLength(0);
                currentLength = 0;
            }
            
            current.append(content).append("\n\n");
            currentLength += length;
        }
        
        // 最后一段
        if (currentLength > 0) {
            segments.add(createSegment(current.toString(), segments.size()));
        }
        
        return segments;
    }
    
    private ProcessedSegment createSegment(String content, int order) {
        return new ProcessedSegment(content, SegmentType.TEXT, order, null);
    }
    
    // Getters
    public List<MarkdownNode> getNodes() {
        return nodes;
    }
}
```

### 2.3 ProcessedSegment.java

**路径**: `domain/rag/model/vo/ProcessedSegment.java`

```java
package com.nexusvoice.domain.rag.model.vo;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;

/**
 * 处理后的段落值对象
 */
public class ProcessedSegment {
    
    public enum SegmentType {
        TEXT,       // 普通文本
        CODE,       // 代码块
        TABLE       // 表格
    }
    
    private final String content;
    private final SegmentType type;
    private Integer order;
    private final String titleContext;  // 标题上下文
    
    public ProcessedSegment(String content, SegmentType type, 
                           Integer order, String titleContext) {
        this.content = content;
        this.type = type;
        this.order = order;
        this.titleContext = titleContext;
    }
    
    /**
     * 转换为DocumentUnit实体
     */
    public DocumentUnit toDocumentUnit(Long fileId, Integer page) {
        DocumentUnit unit = new DocumentUnit();
        unit.setFileId(fileId);
        unit.setPage(page);
        unit.setContent(content);
        unit.setUnitType(type.name());
        unit.setIsOcr(true);
        unit.setIsVector(false);
        unit.setParagraphIndex(order);
        unit.setCharCount(content.length());
        return unit;
    }
    
    // Getters and Setters
    public String getContent() {
        return content;
    }
    
    public SegmentType getType() {
        return type;
    }
    
    public Integer getOrder() {
        return order;
    }
    
    public void setOrder(Integer order) {
        this.order = order;
    }
    
    public String getTitleContext() {
        return titleContext;
    }
}
```

### 2.4 SegmentSplitConfig.java

**路径**: `domain/rag/model/vo/SegmentSplitConfig.java`

```java
package com.nexusvoice.domain.rag.model.vo;

/**
 * 分段配置值对象
 */
public class SegmentSplitConfig {
    
    private final int maxLength;
    private final int minLength;
    private final int overlapSize;
    
    public SegmentSplitConfig(int maxLength, int minLength, int overlapSize) {
        this.maxLength = maxLength;
        this.minLength = minLength;
        this.overlapSize = overlapSize;
    }
    
    // Getters
    public int getMaxLength() {
        return maxLength;
    }
    
    public int getMinLength() {
        return minLength;
    }
    
    public int getOverlapSize() {
        return overlapSize;
    }
    
    // 默认配置
    public static SegmentSplitConfig defaultConfig() {
        return new SegmentSplitConfig(1800, 200, 100);
    }
}
```

### 2.5 SearchRequest.java / SearchResult.java

**路径**: `domain/rag/model/vo/SearchRequest.java` 和 `SearchResult.java`

```java
package com.nexusvoice.domain.rag.model.vo;

import com.nexusvoice.exception.BizException;
import com.nexusvoice.enums.ErrorCodeEnum;

import java.util.List;

/**
 * 检索请求值对象
 */
public class SearchRequest {
    
    private final List<Long> knowledgeBaseIds;
    private final String question;
    private final Integer maxResults;
    private final Double minScore;
    private final boolean enableHyde;
    private final boolean enableRerank;
    
    // Builder模式构造
    private SearchRequest(Builder builder) {
        this.knowledgeBaseIds = builder.knowledgeBaseIds;
        this.question = builder.question;
        this.maxResults = builder.maxResults;
        this.minScore = builder.minScore;
        this.enableHyde = builder.enableHyde;
        this.enableRerank = builder.enableRerank;
    }
    
    /**
     * 验证请求合法性
     */
    public void validate() {
        if (question == null || question.trim().isEmpty()) {
            throw new BizException(ErrorCodeEnum.RAG_SEARCH_QUERY_EMPTY);
        }
        if (knowledgeBaseIds == null || knowledgeBaseIds.isEmpty()) {
            throw new BizException(ErrorCodeEnum.RAG_KNOWLEDGE_BASE_EMPTY);
        }
        if (maxResults != null && maxResults <= 0) {
            throw new BizException(ErrorCodeEnum.RAG_INVALID_MAX_RESULTS);
        }
    }
    
    // Getters
    // ... 省略
    
    // Builder类
    public static class Builder {
        private List<Long> knowledgeBaseIds;
        private String question;
        private Integer maxResults = 15;
        private Double minScore = 0.7;
        private boolean enableHyde = true;
        private boolean enableRerank = true;
        
        public Builder knowledgeBaseIds(List<Long> ids) {
            this.knowledgeBaseIds = ids;
            return this;
        }
        
        public Builder question(String question) {
            this.question = question;
            return this;
        }
        
        public Builder maxResults(Integer maxResults) {
            this.maxResults = maxResults;
            return this;
        }
        
        public Builder minScore(Double minScore) {
            this.minScore = minScore;
            return this;
        }
        
        public Builder enableHyde(boolean enable) {
            this.enableHyde = enable;
            return this;
        }
        
        public Builder enableRerank(boolean enable) {
            this.enableRerank = enable;
            return this;
        }
        
        public SearchRequest build() {
            return new SearchRequest(this);
        }
    }
}

/**
 * 检索结果值对象
 */
public class SearchResult {
    
    public enum SearchType {
        VECTOR,     // 向量检索
        KEYWORD,    // 关键词检索
        HYBRID      // 混合检索
    }
    
    private final Long documentUnitId;
    private final String content;
    private Double score;
    private final SearchType searchType;
    private final Map<String, Object> metadata;
    private Double fusionScore;  // RRF融合后的分数
    
    // 构造函数
    public SearchResult(Long documentUnitId, String content, 
                       Double score, SearchType searchType,
                       Map<String, Object> metadata) {
        this.documentUnitId = documentUnitId;
        this.content = content;
        this.score = score;
        this.searchType = searchType;
        this.metadata = metadata;
    }
    
    /**
     * 获取内容预览
     */
    public String getContentPreview(int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
    
    // Getters and Setters
    // ... 省略
}
```

---

## 🔧 3. Domain层 - 领域服务接口

### 3.1 MarkdownProcessingDomainService.java

**路径**: `domain/rag/service/MarkdownProcessingDomainService.java`

```java
package com.nexusvoice.domain.rag.service;

import com.nexusvoice.domain.rag.model.vo.DocumentTree;
import com.nexusvoice.domain.rag.model.vo.ProcessingContext;

/**
 * Markdown处理领域服务
 */
public interface MarkdownProcessingDomainService {
    
    /**
     * 解析Markdown为文档树
     */
    DocumentTree parseToDocumentTree(String markdown);
    
    /**
     * 翻译特殊节点为自然语言
     */
    String translateSpecialNodes(String markdown, ProcessingContext context);
}
```

### 3.2 HybridSearchDomainService.java

**路径**: `domain/rag/service/HybridSearchDomainService.java`

```java
package com.nexusvoice.domain.rag.service;

import com.nexusvoice.domain.rag.model.vo.SearchRequest;
import com.nexusvoice.domain.rag.model.vo.SearchResult;

import java.util.List;

/**
 * 混合检索领域服务
 */
public interface HybridSearchDomainService {
    
    /**
     * 执行混合检索（向量+关键词+RRF融合）
     */
    List<SearchResult> hybridSearch(SearchRequest request);
    
    /**
     * HyDE：生成假设文档
     */
    String generateHypotheticalDocument(String question);
}
```

### 3.3 RerankDomainService.java

**路径**: `domain/rag/service/RerankDomainService.java`

```java
package com.nexusvoice.domain.rag.service;

import com.nexusvoice.domain.rag.model.vo.SearchResult;

import java.util.List;

/**
 * Rerank重排序领域服务
 */
public interface RerankDomainService {
    
    /**
     * 重排序检索结果
     */
    List<SearchResult> rerank(List<SearchResult> results, String question);
}
```

---

## 🏗️ 4. Infrastructure层 - Markdown处理组件

### 4.1 FlexmarkParser.java

**路径**: `infrastructure/rag/markdown/FlexmarkParser.java`

```java
package com.nexusvoice.infrastructure.rag.markdown;

import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.stereotype.Component;

/**
 * Flexmark解析器封装
 */
@Component
public class FlexmarkParser {
    
    private final Parser parser;
    
    public FlexmarkParser() {
        MutableDataSet options = new MutableDataSet();
        // 启用扩展：表格、代码高亮、任务列表等
        options.set(Parser.EXTENSIONS, Arrays.asList(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListExtension.create()
        ));
        this.parser = Parser.builder(options).build();
    }
    
    /**
     * 解析Markdown为AST
     */
    public Node parse(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return null;
        }
        return parser.parse(markdown);
    }
}
```

### 4.2 DocumentTreeBuilder.java

**路径**: `infrastructure/rag/markdown/DocumentTreeBuilder.java`

```java
package com.nexusvoice.infrastructure.rag.markdown;

import com.nexusvoice.domain.rag.model.vo.DocumentTree;
import com.nexusvoice.domain.rag.model.vo.MarkdownNode;
import com.nexusvoice.domain.rag.model.vo.SegmentSplitConfig;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.util.ast.Node;
import org.springframework.stereotype.Component;

/**
 * 文档树构建器
 */
@Component
public class DocumentTreeBuilder {
    
    /**
     * 构建文档树（原文模式）
     */
    public DocumentTree buildRawDocumentTree(Node document, SegmentSplitConfig config) {
        DocumentTree tree = new DocumentTree(config);
        
        for (Node child : document.getChildren()) {
            MarkdownNode node = processNode(child);
            if (node != null) {
                tree.addNode(node);
            }
        }
        
        return tree;
    }
    
    /**
     * 处理单个节点
     */
    private MarkdownNode processNode(Node node) {
        if (node instanceof Heading heading) {
            return createHeadingNode(heading);
        } else if (node instanceof Paragraph paragraph) {
            return createParagraphNode(paragraph);
        } else if (node instanceof FencedCodeBlock codeBlock) {
            return createCodeBlockNode(codeBlock);
        } else if (node instanceof TableBlock table) {
            return createTableNode(table);
        } else if (node instanceof Image image) {
            return createImageNode(image);
        } else if (node instanceof BulletList || node instanceof OrderedList) {
            return createListNode(node);
        }
        return null;
    }
    
    private MarkdownNode createHeadingNode(Heading heading) {
        int level = heading.getLevel();
        String content = heading.getText().toString();
        return new MarkdownNode(
            MarkdownNode.NodeType.HEADING,
            level,
            "# ".repeat(level) + content,
            null,
            null
        );
    }
    
    private MarkdownNode createCodeBlockNode(FencedCodeBlock codeBlock) {
        String code = codeBlock.getContentChars().toString();
        String lang = codeBlock.getInfo().toString();
        String content = "```" + lang + "\n" + code + "\n```";
        return new MarkdownNode(
            MarkdownNode.NodeType.CODE_BLOCK,
            0,
            content,
            lang,
            null
        );
    }
    
    // 其他节点创建方法...
}
```

### 4.3 MarkdownProcessingStrategy.java（核心）

**路径**: `infrastructure/rag/strategy/MarkdownProcessingStrategy.java`

```java
package com.nexusvoice.infrastructure.rag.strategy;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.enums.FileType;
import com.nexusvoice.domain.rag.model.vo.DocumentTree;
import com.nexusvoice.domain.rag.model.vo.ProcessedSegment;
import com.nexusvoice.domain.rag.model.vo.SegmentSplitConfig;
import com.nexusvoice.infrastructure.rag.markdown.DocumentTreeBuilder;
import com.nexusvoice.infrastructure.rag.markdown.FlexmarkParser;
import com.vladsch.flexmark.util.ast.Node;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Markdown文档处理策略
 */
@Slf4j
@Component("markdownProcessingStrategy")
public class MarkdownProcessingStrategy implements DocumentProcessingStrategy {
    
    private final FlexmarkParser flexmarkParser;
    private final DocumentTreeBuilder treeBuilder;
    
    public MarkdownProcessingStrategy(FlexmarkParser flexmarkParser,
                                     DocumentTreeBuilder treeBuilder) {
        this.flexmarkParser = flexmarkParser;
        this.treeBuilder = treeBuilder;
    }
    
    @Override
    public FileType supportedFileType() {
        return FileType.MARKDOWN;
    }
    
    @Override
    public List<DocumentUnit> parseAndSplit(FileDetail fileDetail, byte[] fileBytes) {
        log.info("开始解析Markdown文档，文件ID：{}", fileDetail.getId());
        
        // 1. 字节数组转字符串
        String markdown = new String(fileBytes, StandardCharsets.UTF_8);
        
        // 2. 解析为AST
        Node document = flexmarkParser.parse(markdown);
        
        // 3. 构建文档树
        SegmentSplitConfig config = SegmentSplitConfig.defaultConfig();
        DocumentTree tree = treeBuilder.buildRawDocumentTree(document, config);
        
        // 4. 执行层次化分割
        List<ProcessedSegment> segments = tree.performHierarchicalSplit();
        
        // 5. 转换为DocumentUnit
        List<DocumentUnit> units = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            DocumentUnit unit = segments.get(i).toDocumentUnit(fileDetail.getId(), i);
            units.add(unit);
        }
        
        log.info("Markdown文档解析完成，共{}个段落", units.size());
        return units;
    }
    
    @Override
    public boolean needsTranslationEnhancement() {
        return true;  // Markdown需要翻译代码块、表格等
    }
}
```

---

## 📋 5. 待续组件

由于篇幅限制，以下组件请参考AgentX文档中的实现：

1. **MarkdownAstRewriter** - 参考03-核心组件实现详解.md第5节
2. **MarkdownContentSplitter** - 参考03-核心组件实现详解.md第6节
3. **RagDocumentProcessConsumer** - RocketMQ消费者
4. **DocumentVectorizationOrchestrator** - 参考03-核心组件实现详解.md第4节
5. **HybridSearchService** - 参考03-核心组件实现详解.md第8节

---

## ✅ 实施检查清单

### Phase 1：基础组件
- [ ] 添加Flexmark依赖
- [ ] 创建Domain层值对象（5个）
- [ ] 创建Domain层领域服务接口（3个）
- [ ] 创建FlexmarkParser
- [ ] 创建DocumentTreeBuilder
- [ ] 创建MarkdownProcessingStrategy

### Phase 2：向量化组件
- [ ] 创建MarkdownAstRewriter
- [ ] 创建NodeTranslatorService
- [ ] 创建MarkdownContentSplitter
- [ ] 创建DocumentVectorizationOrchestrator
- [ ] 创建RocketMQ消费者（2个）

### Phase 3：检索组件
- [ ] 创建HyDEService
- [ ] 创建HybridSearchService
- [ ] 创建RRF算法实现
- [ ] 创建RerankService

### Phase 4：Application和API
- [ ] 创建Application服务（3个）
- [ ] 创建DTO和Assembler
- [ ] 创建REST Controller（2个）

---

**© 2025 NexusVoice | 缺失组件清单 v1.0**

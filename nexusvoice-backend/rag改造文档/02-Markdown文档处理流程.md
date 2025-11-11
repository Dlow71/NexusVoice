# Markdown文档处理流程详解

> **系列文档**: RAG知识库系统实现指南 (2/4)  
> **前置阅读**: 01-RAG系统架构概览.md

---

## 📋 本文概要

详细介绍Markdown文档从上传到可检索的完整流程，包括：

1. 两阶段处理架构详解
2. 阶段1：结构化解析与原文分割
3. 阶段2：翻译与智能二次分割
4. 数据流转与状态管理

---

## 🔄 完整处理流程

```
用户上传MD → 保存OSS → 发MQ(DOC_SYNC_OCR)
    ↓
【阶段1】结构化解析 → 原文分割 → 保存DocumentUnit → 发MQ(DOC_SYNC_RAG)
    ↓
【阶段2】翻译特殊节点 → 智能二次分割 → 发MQ向量化
    ↓
向量化 → 存储PGVector → 完成
```

---

## 📝 阶段1：结构化解析与原文分割

### 核心目标

- ✅ 保留原始Markdown
- ✅ 按标题层级分段
- ✅ 保持文档结构

### 处理步骤

#### 1. 解析为AST树

```java
// Flexmark解析
Parser parser = Parser.builder(options).build();
Node document = parser.parse(markdownContent);
```

**AST结构示例**:
```
Document
├─ Heading(level=1) "系统架构"
├─ Paragraph "内容..."
├─ Heading(level=2) "核心组件"
├─ FencedCodeBlock "代码..."
└─ TableBlock "表格..."
```

#### 2. 构建文档树

```java
DocumentTree tree = new DocumentTree();
for (Node child : document.getChildren()) {
    if (child instanceof Heading) {
        tree.addHeading(level, content);
    } else if (child instanceof Paragraph) {
        tree.addParagraph(content);
    } else if (child instanceof FencedCodeBlock) {
        tree.addCodeBlock(code);  // 保持原样
    }
}
```

#### 3. 层次化分割

**分割规则**:
1. 一级标题强制新建段落
2. 累积长度超过maxLength时保存
3. 保持标题上下文

```java
List<ProcessedSegment> segments = new ArrayList<>();
StringBuilder current = new StringBuilder();

for (DocumentNode node : tree.getNodes()) {
    if (node.isH1() && current.length() > 0) {
        segments.add(current.toString());
        current.setLength(0);
    }
    
    if (current.length() + node.length() > maxLength) {
        segments.add(current.toString());
        current.setLength(0);
    }
    
    current.append(node.getContent()).append("\n\n");
}
```

#### 4. 保存DocumentUnit

```java
for (int i = 0; i < segments.size(); i++) {
    DocumentUnitEntity unit = new DocumentUnitEntity();
    unit.setFileId(fileId);
    unit.setPage(i);
    unit.setContent(segments.get(i));  // 原文！
    unit.setIsOcr(true);
    unit.setIsVector(false);
    
    repository.insert(unit);
}
```

---

## 🔄 阶段2：翻译与智能二次分割

### 核心目标

- ✅ 翻译特殊节点
- ✅ 控制向量长度
- ✅ 重叠保留上下文

### 特殊节点翻译

#### 代码块翻译

**原始**:
```java
public class UserService {
    @Autowired
    private UserRepository repo;
}
```

**翻译**:
```
【代码描述】这是UserService类，包含自动注入的UserRepository用于数据访问。
```

#### 表格翻译

**原始**:
```markdown
| 组件 | 版本 |
|-----|------|
| 网关 | 3.1.0 |
```

**翻译**:
```
【表格内容】系统组件：网关使用3.1.0版本。
```

#### 图片OCR

```
【图片描述】这是一张系统架构图，包含API网关、服务层、数据层...
```

### 智能二次分割

#### 三层策略

| 策略 | 分隔符 | 优先级 |
|-----|--------|--------|
| 段落分割 | `\n\n` | 最高 |
| 句子分割 | `。！？` | 中等 |
| 强制截断 | 固定长度 | 兜底 |

#### 重叠机制

```
Chunk 0: [======100重叠======]
              Chunk 1: [======100重叠======]
                           Chunk 2: [======]
```

**参数**:
- maxLength: 1800字符
- minLength: 200字符
- overlap: 100字符

---

## 💾 数据存储

### 表结构

**file_detail (文件元数据)**
```sql
CREATE TABLE file_detail (
    id VARCHAR(64) PRIMARY KEY,
    status VARCHAR(32),  -- 状态机
    file_page_size INTEGER
);
```

**document_unit (原文段落)**
```sql
CREATE TABLE document_unit (
    id VARCHAR(64) PRIMARY KEY,
    file_id VARCHAR(64),
    page INTEGER,
    content TEXT,        -- 原始Markdown
    is_ocr BOOLEAN,
    is_vector BOOLEAN
);
```

**PGVector (向量)**
```sql
CREATE TABLE langchain4j_pgvector_embedding (
    id UUID PRIMARY KEY,
    embedding VECTOR(1536),
    text TEXT,           -- 翻译后的文本
    metadata JSONB
);
```

### 状态机

```
PENDING → PROCESSING → OCR_COMPLETED → EMBEDDING → COMPLETED
```

---

## 📊 完整示例

### 输入

```markdown
# Spring Boot

简介...

## 配置

```java
@SpringBootApplication
public class App {}
```

| 配置 | 说明 |
|-----|------|
| port | 端口 |
```

### 阶段1输出 (1条DocumentUnit)

```json
{
  "id": "unit-001",
  "page": 0,
  "content": "# Spring Boot\n\n简介...\n\n## 配置\n\n```java\n...\n```\n\n| 配置 | 说明 |\n..."
}
```

### 阶段2输出 (可能3条向量)

```json
[
  {
    "text": "# Spring Boot\n\n简介...",
    "embedding": [0.1, 0.2, ...],
    "metadata": {"file_id": "f-1", "page": 0}
  },
  {
    "text": "## 配置\n\n【代码描述】这是Spring Boot主类...",
    "embedding": [0.3, 0.4, ...],
    "metadata": {"file_id": "f-1", "page": 1000}
  },
  {
    "text": "【表格内容】配置项说明：port表示端口...",
    "embedding": [0.5, 0.6, ...],
    "metadata": {"file_id": "f-1", "page": 1001}
  }
]
```

---

## 🎯 关键要点

### 设计原则

1. **原文不变**: DocumentUnit.content永远是原始Markdown
2. **翻译增强**: 特殊节点翻译提升检索效果
3. **智能分割**: 三层策略保证质量
4. **重叠保留**: 100字符重叠保持上下文

### 性能优化

1. **异步处理**: 用户上传立即返回
2. **批量向量化**: 减少API调用
3. **并行处理**: 多个DocumentUnit并行翻译

### 容错机制

1. **阶段隔离**: 阶段1失败不影响文件保存
2. **重试机制**: MQ消费失败自动重试
3. **降级方案**: 翻译失败使用原文

---

## 📚 相关文档

- ⏮️ 01-RAG系统架构概览.md
- ➡️ 03-核心组件实现详解.md
- ⏭️ 04-RAG检索与部署指南.md

---

**© 2025 AgentX RAG System**

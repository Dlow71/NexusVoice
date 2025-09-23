# NexusVoice DDD架构设计文档

## 📋 概述

NexusVoice采用领域驱动设计(DDD)架构，参考AgentX项目的最佳实践，构建了清晰的分层架构。

## 🏗️ 架构分层

### 1. interfaces层 - 接口层
负责处理外部请求和响应，包括REST API、WebSocket等。

```
interfaces/
├── api/           # REST API控制器
│   ├── agent/     # Agent相关API
│   ├── user/      # 用户相关API
│   ├── conversation/ # 对话相关API
│   └── knowledge/ # 知识库相关API
├── dto/           # 数据传输对象
└── websocket/     # WebSocket处理器
```

### 2. application层 - 应用层
负责业务流程编排和应用服务实现。

```
application/
├── agent/
│   ├── service/   # Agent应用服务
│   ├── dto/       # Agent应用DTO
│   └── assembler/ # 对象转换器
├── user/
│   ├── service/   # 用户应用服务
│   ├── dto/       # 用户应用DTO
│   └── assembler/ # 对象转换器
├── conversation/
│   ├── service/   # 对话应用服务
│   ├── dto/       # 对话应用DTO
│   └── assembler/ # 对象转换器
└── common/        # 通用应用组件
```

### 3. domain层 - 领域层
包含核心业务逻辑和领域模型。

```
domain/
├── agent/
│   ├── model/     # Agent领域实体
│   ├── repository/ # Agent仓储接口
│   ├── service/   # Agent领域服务
│   └── constant/  # Agent常量
├── user/
│   ├── model/     # 用户领域实体
│   ├── repository/ # 用户仓储接口
│   ├── service/   # 用户领域服务
│   └── constant/  # 用户常量
├── conversation/
│   ├── model/     # 对话领域实体
│   ├── repository/ # 对话仓储接口
│   ├── service/   # 对话领域服务
│   └── constant/  # 对话常量
└── common/        # 通用领域组件
```

### 4. infrastructure层 - 基础设施层
提供技术实现和外部系统集成。

```
infrastructure/
├── database/
│   ├── mapper/    # MyBatis映射器
│   └── entity/    # 数据库实体
├── external/      # 外部系统集成
├── config/        # 配置类
└── converter/     # 类型转换器
```

## 🔄 依赖关系

- **interfaces** → **application** → **domain**
- **infrastructure** → **domain**
- 各层之间通过接口进行依赖倒置

## 📦 核心组件

### 基础设施组件
- `BaseEntity`: 通用实体基类
- `UserContext`: 用户上下文工具
- `MyBatisPlusConfig`: MyBatis-Plus配置
- `ListStringConverter`: JSON列表转换器
- `MapConverter`: JSON对象转换器

### 异常处理
- `BizException`: 业务异常类
- `GlobalExceptionHandler`: 全局异常处理器
- `ErrorCodeEnum`: 错误码枚举

### 响应封装
- `Result<T>`: 统一响应结果类

## 🎯 设计原则

1. **单一职责**: 每个类只负责一个职责
2. **依赖倒置**: 高层模块不依赖低层模块
3. **开闭原则**: 对扩展开放，对修改关闭
4. **接口隔离**: 使用小而专的接口

## 🚀 开发指南

### 新增功能开发流程

1. **定义领域模型** (domain层)
   - 创建实体类
   - 定义仓储接口
   - 实现领域服务

2. **实现应用服务** (application层)
   - 创建应用服务类
   - 定义DTO对象
   - 实现对象转换器

3. **提供API接口** (interfaces层)
   - 创建控制器类
   - 定义请求/响应DTO

4. **实现基础设施** (infrastructure层)
   - 实现仓储接口
   - 配置数据库映射

### 代码规范

- 使用中文注释和错误提示
- 遵循ESLint规范要求
- 使用`BizException`抛出业务异常
- 错误码统一定义在`ErrorCodeEnum`中

## 📈 后续计划

1. **第二阶段**: 用户和Agent管理模块
2. **第三阶段**: 对话系统核心功能
3. **第四阶段**: RAG知识库系统
4. **第五阶段**: 高级功能和性能优化

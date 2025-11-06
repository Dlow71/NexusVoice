# JoyAgent多智能体系统总体设计文档
# （AI辅助改造专用版）

> **文档目的**: 提供完整、清晰、可执行的多智能体系统设计方案，供AI Agent理解并辅助开发者改造现有项目
> 
> **适用场景**: 将单一LLM应用改造为多智能体协作系统，或从零构建企业级AI应用

---

## 📋 文档导航

```
第一部分: 系统架构概览 (理解整体设计)
第二部分: 核心组件设计 (理解各模块职责)
第三部分: 执行流程设计 (理解运行机制)
第四部分: 接口定义规范 (理解编程接口)
第五部分: 实现代码模板 (直接使用的代码)
第六部分: 改造实施指南 (按步骤改造)
第七部分: 常见问题解决 (避免踩坑)
```

---

# 第一部分: 系统架构概览

## 1.1 核心架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (UI)                            │
│  React + TypeScript + TailwindCSS                            │
│  - 聊天界面  - 实时流式显示  - 文件上传                       │
└─────────────────────────────────────────────────────────────┘
                            ↓ HTTP/SSE
┌─────────────────────────────────────────────────────────────┐
│                     控制器层 (Controller)                     │
│  GenieController.java                                        │
│  - 请求路由  - SSE管理  - 参数验证                           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  业务逻辑层 (Service Layer)                   │
│  MultiAgentService.java                                      │
│  - 会话管理  - Agent调度  - 结果处理                         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                  Agent调度层 (Handler Layer)                  │
│  AgentHandlerFactory.java                                    │
│  - ReactHandler (简单任务)                                    │
│  - PlanSolveHandler (复杂任务)                               │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    Agent执行层 (Agent Layer)                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ PlanningAgent│  │ ExecutorAgent│  │ SummaryAgent │      │
│  │   (规划)     │  │   (执行)     │  │   (总结)     │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ ReactImplAgent│ │  DataAgent   │  │  CustomAgent │      │
│  │  (ReAct模式) │  │ (数据查询)   │  │  (自定义)    │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                     工具层 (Tool Layer)                       │
│  ToolCollection.java                                         │
│  - BaseTool接口  - 工具注册  - 并发执行  - 结果聚合          │
│                                                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │搜索工具  │ │代码执行  │ │文件处理  │ │数据查询  │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    基础设施层 (Infrastructure)                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │   LLM    │ │  Qdrant  │ │  Redis   │ │  MySQL   │      │
│  │(AI模型)  │ │(向量库)  │ │(缓存)    │ │(存储)    │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
└─────────────────────────────────────────────────────────────┘
```

---

## 1.2 技术栈选型

### 后端技术栈
```yaml
语言: Java 17
框架: Spring Boot 3.2.2
构建: Maven 3.9+
依赖:
  - spring-boot-starter-web: REST API
  - spring-boot-starter-data-jpa: 数据持久化
  - spring-boot-starter-data-redis: 缓存
  - httpcomponents: HTTP客户端
  - jackson: JSON处理
  - lombok: 代码简化
```

### 工具服务技术栈
```yaml
语言: Python 3.11+
框架: FastAPI 0.109+
依赖:
  - uvicorn: ASGI服务器
  - httpx: 异步HTTP客户端
  - pydantic: 数据验证
```

### 前端技术栈
```yaml
语言: TypeScript
框架: React 18
构建: Vite
样式: TailwindCSS
状态: Zustand/Redux
```

---

## 1.3 核心设计理念

### 设计理念1: 模板方法模式 (Template Method)
```
目的: 统一Agent执行流程，子类只需实现差异化逻辑

实现:
BaseAgent (抽象类)
  └─ run() [final] → 固定流程，不可重写
       ├─ 初始化
       ├─ while (currentStep < maxSteps)
       │    └─ step() [abstract] → 子类实现
       └─ 返回结果

优势:
✓ 统一的执行流程
✓ 易于扩展新Agent
✓ 代码复用度高
```

### 设计理念2: 策略模式 (Strategy)
```
目的: 根据任务类型动态选择执行策略

实现:
AgentHandlerFactory
  ├─ ReactHandler → 简单任务策略
  └─ PlanSolveHandler → 复杂任务策略

选择逻辑:
if (任务简单 || 单步骤)
    使用 ReactHandler
else
    使用 PlanSolveHandler

优势:
✓ 灵活切换执行模式
✓ 适配不同任务类型
✓ 易于添加新策略
```

### 设计理念3: 工厂模式 (Factory)
```
目的: 统一Agent和工具的创建

实现:
AgentHandlerFactory.createHandler(type)
ToolRegistry.getTool(name)

优势:
✓ 解耦创建和使用
✓ 统一管理生命周期
✓ 便于依赖注入
```

---

# 第二部分: 核心组件设计

## 2.1 BaseAgent (Agent基类)

### 职责定义
```
1. 定义Agent统一执行流程
2. 管理Agent状态和生命周期
3. 提供工具调用能力
4. 管理对话记忆(Memory)
5. 支持SSE流式输出
```

### 核心属性
```java
public abstract class BaseAgent {
    // 身份信息
    protected String name;              // Agent名称
    protected String description;       // Agent描述
    
    // Prompt系统
    protected String systemPrompt;      // 系统提示词
    protected String nextStepPrompt;    // 下一步引导
    
    // 资源依赖
    protected ToolCollection availableTools;  // 可用工具
    protected Memory memory;                  // 对话记忆
    protected LLM llm;                        // LLM实例
    protected AgentContext context;           // 上下文
    
    // 执行控制
    protected AgentState state;         // 状态: IDLE/RUNNING/FINISHED
    protected int maxSteps = 10;        // 最大执行步数
    protected int currentStep = 0;      // 当前步数
    
    // 输出控制
    protected Printer printer;          // SSE推送器
}
```

### 核心方法
```java
public abstract class BaseAgent {
    
    /**
     * 模板方法: 主执行流程 (final, 不可重写)
     */
    public final String run(String query) {
        // 1. 初始化
        this.state = AgentState.IDLE;
        this.currentStep = 0;
        this.memory.addMessage(RoleType.USER, query);
        
        // 2. 执行循环
        while (currentStep < maxSteps && state != AgentState.FINISHED) {
            currentStep++;
            state = AgentState.RUNNING;
            
            // 调用子类实现的step方法
            String result = step();
            
            if (state == AgentState.FINISHED) {
                return result;
            }
        }
        
        // 3. 超过最大步数
        if (currentStep >= maxSteps) {
            return "执行超时，请简化问题重试";
        }
        
        return "执行完成";
    }
    
    /**
     * 抽象方法: 单步执行逻辑 (子类必须实现)
     */
    public abstract String step();
    
    /**
     * 执行单个工具
     */
    protected String executeTool(String toolName, Map<String, Object> params) {
        BaseTool tool = availableTools.getTool(toolName);
        if (tool == null) {
            return "工具不存在: " + toolName;
        }
        
        try {
            String result = tool.execute(params);
            printer.println("🔧 工具调用: " + toolName);
            printer.println("📊 结果: " + result);
            return result;
        } catch (Exception e) {
            return "工具执行失败: " + e.getMessage();
        }
    }
    
    /**
     * 并发执行多个工具
     */
    protected Map<String, String> executeTools(List<ToolCall> toolCalls) {
        CountDownLatch latch = new CountDownLatch(toolCalls.size());
        Map<String, String> results = new ConcurrentHashMap<>();
        
        for (ToolCall call : toolCalls) {
            executorService.submit(() -> {
                try {
                    String result = executeTool(call.getName(), call.getParams());
                    results.put(call.getName(), result);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return results;
    }
    
    /**
     * 更新记忆
     */
    protected void updateMemory(RoleType role, String content, Object data) {
        Message message = Message.builder()
            .role(role)
            .content(content)
            .data(data)
            .timestamp(System.currentTimeMillis())
            .build();
        memory.addMessage(message);
    }
}
```

---

## 2.2 ReactImplAgent (ReAct模式Agent)

### 职责定义
```
实现Think-Act-Observe循环
适用于简单、直接的任务
```

### 实现代码
```java
public class ReactImplAgent extends ReActAgent {
    
    @Override
    public String step() {
        // 1. Think: LLM思考下一步
        think();
        
        // 2. Act: 执行工具
        String result = act();
        
        // 3. Observe: 观察结果并决定是否继续
        return observe(result);
    }
    
    /**
     * Think阶段: LLM决策
     */
    protected void think() {
        // 构造Prompt
        String prompt = buildThinkPrompt();
        
        // 调用LLM
        String decision = llm.ask(prompt);
        
        // 解析决策
        memory.addMessage(RoleType.ASSISTANT, decision);
    }
    
    /**
     * Act阶段: 执行工具
     */
    protected String act() {
        // 从最后的Assistant消息中提取工具调用
        Message lastMessage = memory.getLastMessage(RoleType.ASSISTANT);
        List<ToolCall> toolCalls = parseToolCalls(lastMessage.getContent());
        
        if (toolCalls.isEmpty()) {
            // 没有工具调用，直接返回回答
            setState(AgentState.FINISHED);
            return lastMessage.getContent();
        }
        
        // 执行工具（支持并发）
        Map<String, String> results = executeTools(toolCalls);
        
        // 合并结果
        return JSON.toJSONString(results);
    }
    
    /**
     * Observe阶段: 观察结果
     */
    protected String observe(String actionResult) {
        // 添加工具结果到记忆
        updateMemory(RoleType.TOOL, actionResult, null);
        
        // 判断是否完成
        if (isTaskCompleted(actionResult)) {
            setState(AgentState.FINISHED);
            
            // 生成最终回答
            return generateFinalAnswer();
        }
        
        // 继续循环
        return null;
    }
    
    /**
     * 构造Think的Prompt
     */
    private String buildThinkPrompt() {
        StringBuilder prompt = new StringBuilder();
        
        // System Prompt
        prompt.append(systemPrompt).append("\n\n");
        
        // 历史对话
        for (Message msg : memory.getMessages()) {
            prompt.append(msg.getRole()).append(": ")
                  .append(msg.getContent()).append("\n");
        }
        
        // 下一步引导
        prompt.append("\n").append(nextStepPrompt);
        
        return prompt.toString();
    }
}
```

---

## 2.3 PlanningAgent (规划Agent)

### 职责定义
```
1. 接收复杂任务
2. 拆解为子任务
3. 识别任务依赖关系
4. 支持并行任务标记
5. 生成可执行计划
```

### 核心数据结构
```java
@Data
public class Plan {
    private String planId;              // 计划ID
    private List<Task> tasks;           // 任务列表
    private Map<String, Object> context; // 上下文
    private PlanStatus status;          // 状态
}

@Data
public class Task {
    private String taskId;              // 任务ID
    private String description;         // 任务描述
    private List<String> dependencies;  // 依赖的任务ID
    private boolean canParallel;        // 是否可并行
    private TaskStatus status;          // 状态
    private String result;              // 执行结果
}
```

### 实现代码
```java
public class PlanningAgent extends ReActAgent {
    
    private Plan currentPlan;
    
    @Override
    public String step() {
        // 规划阶段只需要一步: 生成计划
        think();  // LLM生成计划
        act();    // 创建Plan对象
        
        setState(AgentState.FINISHED);
        return JSON.toJSONString(currentPlan);
    }
    
    /**
     * Think: LLM生成任务计划
     */
    @Override
    protected void think() {
        String planningPrompt = buildPlanningPrompt();
        String planText = llm.ask(planningPrompt);
        
        // 保存到记忆
        updateMemory(RoleType.ASSISTANT, planText, null);
    }
    
    /**
     * Act: 解析计划并创建Plan对象
     */
    @Override
    protected String act() {
        Message lastMessage = memory.getLastMessage(RoleType.ASSISTANT);
        String planText = lastMessage.getContent();
        
        // 解析计划
        currentPlan = parsePlan(planText);
        
        return JSON.toJSONString(currentPlan);
    }
    
    /**
     * 构造规划Prompt
     */
    private String buildPlanningPrompt() {
        String userQuery = memory.getLastMessage(RoleType.USER).getContent();
        
        return String.format("""
            你是一个任务规划专家。请将用户的复杂任务拆解为多个子任务。
            
            用户任务: %s
            
            可用工具:
            %s
            
            请按以下格式输出计划:
            
            任务1: [任务描述]
            工具: [使用的工具名称]
            依赖: []
            
            任务2: [任务描述]
            工具: [使用的工具名称]
            依赖: [任务1]
            
            任务3: [任务描述] <sep> 任务4: [任务描述]
            说明: 使用<sep>分隔可以并行执行的任务
            
            注意:
            1. 任务之间要有明确的依赖关系
            2. 可以并行的任务用<sep>分隔
            3. 每个任务只做一件事
            4. 任务描述要清晰具体
            """,
            userQuery,
            getToolDescriptions()
        );
    }
    
    /**
     * 解析LLM生成的计划文本
     */
    private Plan parsePlan(String planText) {
        Plan plan = new Plan();
        plan.setPlanId(UUID.randomUUID().toString());
        plan.setTasks(new ArrayList<>());
        plan.setStatus(PlanStatus.PENDING);
        
        // 按行解析
        String[] lines = planText.split("\n");
        Task currentTask = null;
        
        for (String line : lines) {
            line = line.trim();
            
            if (line.startsWith("任务")) {
                // 检查是否有并行标记
                String[] parallelTasks = line.split("<sep>");
                
                for (String taskLine : parallelTasks) {
                    Task task = new Task();
                    task.setTaskId("task_" + UUID.randomUUID().toString());
                    task.setDescription(extractTaskDescription(taskLine));
                    task.setCanParallel(parallelTasks.length > 1);
                    task.setStatus(TaskStatus.PENDING);
                    
                    plan.getTasks().add(task);
                    currentTask = task;
                }
            }
            else if (line.startsWith("依赖:") && currentTask != null) {
                List<String> deps = extractDependencies(line);
                currentTask.setDependencies(deps);
            }
        }
        
        return plan;
    }
}
```

---

## 2.4 ExecutorAgent (执行Agent)

### 职责定义
```
1. 接收Plan中的单个Task
2. 调用相应的工具执行
3. 处理执行结果
4. 支持错误重试
```

### 实现代码
```java
public class ExecutorAgent extends ReActAgent {
    
    private Task currentTask;
    
    public String executeTask(Task task) {
        this.currentTask = task;
        return run(task.getDescription());
    }
    
    @Override
    public String step() {
        // 执行单个任务
        think();  // 分析任务需要什么工具
        String result = act();    // 执行工具
        return observe(result);   // 返回结果
    }
    
    @Override
    protected void think() {
        String prompt = String.format("""
            任务: %s
            
            可用工具:
            %s
            
            请选择合适的工具并提供参数。
            
            返回格式:
            {
                "tool": "工具名称",
                "params": {
                    "参数名": "参数值"
                }
            }
            """,
            currentTask.getDescription(),
            getToolDescriptions()
        );
        
        String decision = llm.ask(prompt);
        updateMemory(RoleType.ASSISTANT, decision, null);
    }
    
    @Override
    protected String act() {
        Message lastMessage = memory.getLastMessage(RoleType.ASSISTANT);
        
        try {
            // 解析工具调用
            JSONObject json = JSON.parseObject(lastMessage.getContent());
            String toolName = json.getString("tool");
            Map<String, Object> params = json.getObject("params", Map.class);
            
            // 执行工具
            String result = executeTool(toolName, params);
            
            // 更新任务状态
            currentTask.setStatus(TaskStatus.COMPLETED);
            currentTask.setResult(result);
            
            return result;
            
        } catch (Exception e) {
            currentTask.setStatus(TaskStatus.FAILED);
            return "执行失败: " + e.getMessage();
        }
    }
}
```

---

## 2.5 工具系统 (Tool System)

### BaseTool接口定义
```java
public interface BaseTool {
    /**
     * 工具名称
     */
    String getName();
    
    /**
     * 工具描述
     */
    String getDescription();
    
    /**
     * 参数定义
     */
    List<Parameter> getParameters();
    
    /**
     * 执行工具
     */
    String execute(Map<String, Object> parameters);
    
    /**
     * 是否是原子工具
     */
    default boolean isAtomic() {
        return true;
    }
}

@Data
@Builder
public class Parameter {
    private String name;        // 参数名
    private String type;        // 参数类型: string/number/boolean/object/array
    private String description; // 参数描述
    private boolean required;   // 是否必需
    private Object defaultValue; // 默认值
}
```

### 工具实现示例
```java
@Component
public class SearchTool implements BaseTool {
    
    @Override
    public String getName() {
        return "web_search";
    }
    
    @Override
    public String getDescription() {
        return "搜索互联网信息";
    }
    
    @Override
    public List<Parameter> getParameters() {
        return List.of(
            Parameter.builder()
                .name("query")
                .type("string")
                .description("搜索关键词")
                .required(true)
                .build(),
            Parameter.builder()
                .name("max_results")
                .type("number")
                .description("最大结果数")
                .required(false)
                .defaultValue(10)
                .build()
        );
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        String query = (String) parameters.get("query");
        Integer maxResults = (Integer) parameters.getOrDefault("max_results", 10);
        
        try {
            // 调用搜索API
            List<SearchResult> results = searchAPI.search(query, maxResults);
            
            return JSON.toJSONString(Map.of(
                "success", true,
                "results", results
            ));
            
        } catch (Exception e) {
            return JSON.toJSONString(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}
```

---

# 第三部分: 执行流程设计

## 3.1 ReAct模式完整流程

```
┌─────────────────────────────────────────────────────────┐
│                     用户输入查询                          │
│                  "北京今天天气怎么样?"                     │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│                  创建ReactImplAgent                      │
│  agent = new ReactImplAgent(context)                    │
│  agent.setAvailableTools(toolCollection)                │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│              Step 1: Think (LLM决策)                     │
│  Prompt: "用户问天气，我需要调用weather_search工具"       │
│  LLM Response: {                                         │
│    "action": "tool_call",                               │
│    "tool": "weather_search",                            │
│    "params": {"city": "北京"}                           │
│  }                                                      │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│              Step 2: Act (执行工具)                      │
│  executeTool("weather_search", {city: "北京"})          │
│  Result: {                                              │
│    "city": "北京",                                      │
│    "weather": "晴",                                     │
│    "temperature": 25                                    │
│  }                                                      │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│           Step 3: Observe (观察结果)                     │
│  判断: 已获取天气信息，任务完成                           │
│  生成回答: "北京今天晴天，气温25°C"                       │
│  setState(FINISHED)                                     │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│                    返回结果给用户                         │
└─────────────────────────────────────────────────────────┘
```

---

## 3.2 Plan+Execute模式完整流程

```
┌─────────────────────────────────────────────────────────┐
│                      用户输入复杂查询                      │
│    "分析美元和黄金最近一个月走势，生成对比报告"             │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│              阶段1: Planning (规划)                       │
│  agent = new PlanningAgent(context)                     │
│  plan = agent.run(query)                                │
│                                                         │
│  生成的计划:                                             │
│  Task 1: 搜索美元汇率数据(最近30天)                       │
│    - 工具: web_search                                   │
│    - 依赖: []                                           │
│    - 可并行: true                                       │
│                                                         │
│  Task 2: 搜索黄金价格数据(最近30天)                       │
│    - 工具: web_search                                   │
│    - 依赖: []                                           │
│    - 可并行: true                                       │
│                                                         │
│  Task 3: 数据清洗和处理                                  │
│    - 工具: code_interpreter                             │
│    - 依赖: [Task 1, Task 2]                            │
│    - 可并行: false                                      │
│                                                         │
│  Task 4: 生成对比分析                                    │
│    - 工具: code_interpreter                             │
│    - 依赖: [Task 3]                                    │
│    - 可并行: false                                      │
│                                                         │
│  Task 5: 生成HTML报告                                   │
│    - 工具: report_generator                             │
│    - 依赖: [Task 4]                                    │
│    - 可并行: false                                      │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│             阶段2: Execute (执行)                         │
│  executorAgent = new ExecutorAgent(context)             │
│                                                         │
│  执行顺序 (基于依赖关系):                                 │
│                                                         │
│  [并发执行] Task 1 ═══╗                                 │
│                       ║                                 │
│  [并发执行] Task 2 ═══╝                                 │
│                       ↓                                 │
│  [顺序执行] Task 3                                       │
│                       ↓                                 │
│  [顺序执行] Task 4                                       │
│                       ↓                                 │
│  [顺序执行] Task 5                                       │
│                                                         │
│  每个Task的执行:                                         │
│    1. executorAgent.executeTask(task)                  │
│    2. task.status = COMPLETED                          │
│    3. task.result = "..."                              │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│             阶段3: Summary (总结)                         │
│  summaryAgent = new SummaryAgent(context)               │
│  finalResult = summaryAgent.summarize(plan)             │
│                                                         │
│  汇总所有任务结果:                                        │
│  - Task 1结果: 美元数据                                  │
│  - Task 2结果: 黄金数据                                  │
│  - Task 3结果: 清洗后数据                                │
│  - Task 4结果: 分析结论                                  │
│  - Task 5结果: HTML报告                                 │
│                                                         │
│  生成最终回答                                            │
└─────────────────────────────────────────────────────────┘
                         ↓
┌─────────────────────────────────────────────────────────┐
│                   返回结果给用户                          │
│  ✓ 展示分析报告                                          │
│  ✓ 提供下载链接                                          │
└─────────────────────────────────────────────────────────┘
```

---

# 第四部分: 接口定义规范

## 4.1 REST API接口

### 4.1.1 聊天接口
```http
POST /api/chat
Content-Type: application/json

Request:
{
  "sessionId": "session_123",    // 会话ID
  "message": "用户输入的消息",
  "mode": "auto",                // auto/react/plan (自动/ReAct/Plan+Execute)
  "attachments": [               // 可选: 附件
    {
      "type": "file",
      "name": "data.csv",
      "content": "base64..."
    }
  ]
}

Response:
{
  "success": true,
  "sessionId": "session_123",
  "messageId": "msg_456",
  "content": "AI的回复",
  "metadata": {
    "mode": "plan",              // 实际使用的模式
    "steps": 5,                  // 执行步数
    "tools_used": ["search", "code"],  // 使用的工具
    "execution_time": 12.5       // 执行时间(秒)
  }
}
```

### 4.1.2 SSE流式接口
```http
GET /api/chat/stream?sessionId=session_123
Accept: text/event-stream

Response (SSE格式):
event: start
data: {"type": "start", "message": "开始处理"}

event: thinking
data: {"type": "thinking", "message": "正在分析任务..."}

event: planning
data: {"type": "planning", "plan": {...}}

event: tool_call
data: {"type": "tool_call", "tool": "search", "status": "running"}

event: tool_result
data: {"type": "tool_result", "tool": "search", "result": "..."}

event: answer
data: {"type": "answer", "content": "这是答案"}

event: done
data: {"type": "done", "summary": {...}}
```

---

## 4.2 内部接口规范

### 4.2.1 Agent接口
```java
public interface Agent {
    /**
     * 执行Agent
     * @param query 用户查询
     * @return 执行结果
     */
    String run(String query);
    
    /**
     * 获取Agent名称
     */
    String getName();
    
    /**
     * 获取Agent状态
     */
    AgentState getState();
    
    /**
     * 设置可用工具
     */
    void setAvailableTools(ToolCollection tools);
    
    /**
     * 设置输出器
     */
    void setPrinter(Printer printer);
}
```

### 4.2.2 Handler接口
```java
public interface AgentHandler {
    /**
     * 处理聊天请求
     * @param request 请求对象
     * @return 处理结果
     */
    String handle(ChatRequest request);
    
    /**
     * 获取Handler类型
     */
    String getHandlerType();
    
    /**
     * 是否支持该请求
     */
    boolean supports(ChatRequest request);
}
```

---

# 第五部分: 实现代码模板

## 5.1 项目结构模板

```
src/main/java/com/yourcompany/agent/
├── agent/                      # Agent实现
│   ├── BaseAgent.java          # Agent基类
│   ├── ReActAgent.java         # ReAct抽象类
│   ├── ReactImplAgent.java     # ReAct实现
│   ├── PlanningAgent.java      # 规划Agent
│   ├── ExecutorAgent.java      # 执行Agent
│   └── SummaryAgent.java       # 总结Agent
│
├── handler/                    # Handler实现
│   ├── AgentHandler.java       # Handler接口
│   ├── AgentHandlerFactory.java # Handler工厂
│   ├── ReactHandler.java       # ReAct处理器
│   └── PlanSolveHandler.java   # Plan处理器
│
├── tool/                       # 工具实现
│   ├── BaseTool.java           # 工具接口
│   ├── ToolCollection.java     # 工具集合
│   ├── ToolRegistry.java       # 工具注册
│   └── common/                 # 通用工具
│       ├── SearchTool.java
│       ├── CodeInterpreterTool.java
│       └── ...
│
├── dto/                        # 数据传输对象
│   ├── ChatRequest.java
│   ├── ChatResponse.java
│   ├── Message.java
│   ├── Plan.java
│   └── Task.java
│
├── memory/                     # 记忆管理
│   ├── Memory.java
│   └── VectorMemory.java
│
├── llm/                        # LLM适配
│   ├── LLM.java
│   └── DeepSeekLLM.java
│
├── service/                    # 业务服务
│   ├── MultiAgentService.java
│   └── SessionService.java
│
├── controller/                 # 控制器
│   └── ChatController.java
│
└── config/                     # 配置类
    ├── AgentConfig.java
    └── ToolConfig.java
```

---

## 5.2 配置文件模板

### application.yml
```yaml
server:
  port: 8080

spring:
  application:
    name: multi-agent-system

# LLM配置
llm:
  provider: deepseek  # deepseek/openai/custom
  api-key: ${LLM_API_KEY}
  api-base: https://api.deepseek.com/v1
  model: deepseek-chat
  max-tokens: 8192
  temperature: 0.7
  timeout: 30000

# Agent配置
agent:
  max-steps: 10
  concurrent-tools: 5
  timeout: 60000
  
  # React模式配置
  react:
    enabled: true
    max-iterations: 5
  
  # Plan模式配置
  plan:
    enabled: true
    max-tasks: 20
    parallel-execution: true

# 工具配置
tool:
  service-url: http://localhost:1601
  timeout: 30000
  
  # 启用的工具
  enabled-tools:
    - web_search
    - code_interpreter
    - file_processor

# Redis配置
spring:
  redis:
    host: localhost
    port: 6379
    password:
    database: 0

# 日志配置
logging:
  level:
    com.yourcompany.agent: DEBUG
    root: INFO
```

---

## 5.3 Maven依赖模板

### pom.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.yourcompany</groupId>
    <artifactId>multi-agent-system</artifactId>
    <version>1.0.0</version>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.2</version>
    </parent>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Boot Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- Spring Boot Redis -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- HTTP Client -->
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
            <version>5.2.1</version>
        </dependency>
        
        <!-- JSON处理 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>2.0.43</version>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <!-- MySQL -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

# 第六部分: 改造实施指南

## 6.1 从零开始构建

### 步骤1: 创建项目骨架
```bash
# 1. 使用Spring Initializr创建项目
# 选择: Spring Boot 3.2.2, Java 17, Maven

# 2. 创建目录结构
mkdir -p src/main/java/com/yourcompany/agent/{agent,handler,tool,dto,memory,llm,service,controller,config}

# 3. 创建配置文件
touch src/main/resources/application.yml
```

### 步骤2: 实现BaseAgent
```java
// 复制第二部分2.1的完整代码
// 文件: src/main/java/com/yourcompany/agent/agent/BaseAgent.java
```

### 步骤3: 实现ReactImplAgent
```java
// 复制第二部分2.2的完整代码
// 文件: src/main/java/com/yourcompany/agent/agent/ReactImplAgent.java
```

### 步骤4: 实现工具系统
```java
// 复制第二部分2.5的完整代码
// 实现至少一个工具(如SearchTool)
```

### 步骤5: 实现Handler
```java
// 实现ReactHandler
// 集成Agent和工具
```

### 步骤6: 实现Controller
```java
// 实现REST API
// 实现SSE流式输出
```

### 步骤7: 测试验证
```java
// 编写单元测试
// 集成测试
// 端到端测试
```

---

## 6.2 改造现有项目

### 评估现状
```
1. 现有架构分析
   - 是否有LLM调用层?
   - 是否有工具系统?
   - 是否有状态管理?

2. 改造范围确定
   - 最小改造: 只添加ReAct模式
   - 中等改造: 添加ReAct + Planning
   - 完整改造: 完整多智能体系统

3. 兼容性考虑
   - API接口兼容
   - 数据结构兼容
   - 现有功能不受影响
```

### 渐进式改造步骤

#### 阶段1: 引入Agent抽象 (1-2天)
```java
// 1. 创建BaseAgent抽象类
// 2. 将现有LLM调用逻辑封装到Agent中
// 3. 保持API接口不变

// 示例:
public class LegacyAgent extends BaseAgent {
    @Override
    public String step() {
        // 包装现有逻辑
        return existingLLMCall(getLastUserMessage());
    }
}
```

#### 阶段2: 添加工具系统 (2-3天)
```java
// 1. 定义BaseTool接口
// 2. 将现有功能封装为工具
// 3. 实现ToolCollection

// 示例:
@Component
public class ExistingFeatureTool implements BaseTool {
    @Autowired
    private ExistingService existingService;
    
    @Override
    public String execute(Map<String, Object> params) {
        // 调用现有服务
        return existingService.doSomething(params);
    }
}
```

#### 阶段3: 实现ReAct模式 (3-4天)
```java
// 1. 实现ReactImplAgent
// 2. 添加Handler层
// 3. 逐步迁移现有功能

// 迁移策略:
// - 先迁移简单功能
// - 保留旧接口作为fallback
// - 灰度发布测试
```

#### 阶段4: 添加Plan模式 (4-5天)
```java
// 1. 实现PlanningAgent
// 2. 实现ExecutorAgent
// 3. 实现智能路由

// 路由策略:
if (isComplexTask(query)) {
    return planSolveHandler.handle(request);
} else {
    return reactHandler.handle(request);
}
```

---

## 6.3 关键决策点

### 决策1: 选择模式
```
问题: 是否需要Plan+Execute模式?

考虑因素:
✓ 任务复杂度: 多数是单步还是多步?
✓ 并发需求: 是否需要并行执行?
✓ 开发成本: 团队资源和时间

建议:
- 简单场景: 只实现ReAct
- 复杂场景: ReAct + Plan
- 企业应用: 完整实现
```

### 决策2: 工具架构
```
问题: Java实现还是Python实现工具?

Java工具:
✓ 优点: 统一技术栈、性能好
✗ 缺点: AI库生态较弱

Python工具:
✓ 优点: AI库丰富、开发快
✗ 缺点: 需要额外服务、跨语言调用

建议:
- 通用工具: Java实现
- AI相关工具: Python实现
- 混合架构: MCP协议统一
```

### 决策3: 部署方式
```
问题: 单体还是微服务?

单体部署:
✓ 优点: 简单、易维护
✗ 缺点: 扩展性有限

微服务部署:
✓ 优点: 可独立扩展、高可用
✗ 缺点: 复杂度高

建议:
- 初期: 单体部署
- 规模增长: 服务拆分
- 大规模: K8s编排
```

---

# 第七部分: 常见问题解决

## 7.1 性能问题

### 问题1: Agent执行慢
```
原因分析:
1. LLM调用延迟高
2. 工具执行时间长
3. 串行执行无并发

解决方案:
1. LLM优化
   - 使用更快的模型
   - 减少prompt长度
   - 启用流式输出

2. 工具优化
   - 异步执行
   - 超时控制
   - 结果缓存

3. 并发优化
   - Plan模式并行执行
   - 工具并发调用
   - 合理配置线程池
```

### 问题2: 内存占用高
```
原因分析:
1. Memory保存过多历史
2. 对象创建过多
3. 缓存未释放

解决方案:
1. Memory管理
   // 限制历史消息数量
   if (messages.size() > maxMessages) {
       messages.subList(0, messages.size() - maxMessages).clear();
   }

2. 对象池复用
   // 使用对象池
   private static final ObjectPool<StringBuilder> POOL = ...;

3. 定时清理
   @Scheduled(fixedRate = 3600000)
   public void cleanupCache() {
       cache.cleanup();
   }
```

---

## 7.2 功能问题

### 问题1: Agent陷入循环
```
现象: Agent重复执行相同步骤

原因: 
1. 没有明确的完成条件
2. maxSteps设置过大
3. Prompt引导不明确

解决方案:
1. 设置合理的maxSteps
   private int maxSteps = 5;  // 简单任务5步足够

2. 明确完成条件
   protected boolean isTaskCompleted(String result) {
       // 检查是否包含最终答案标记
       return result.contains("[DONE]") || 
              result.contains("Final Answer:");
   }

3. 优化Prompt
   在nextStepPrompt中明确要求:
   "如果任务已完成,请输出'Final Answer: [你的答案]'"
```

### 问题2: 工具调用失败
```
现象: 工具执行返回错误

排查步骤:
1. 检查工具是否注册
   BaseTool tool = toolCollection.getTool(toolName);
   if (tool == null) {
       log.error("工具未注册: {}", toolName);
   }

2. 检查参数是否正确
   List<Parameter> params = tool.getParameters();
   validateParameters(actualParams, params);

3. 检查工具服务状态
   # Python工具服务是否启动
   curl http://localhost:1601/health

解决方案:
1. 完善错误处理
   try {
       return tool.execute(params);
   } catch (Exception e) {
       log.error("工具执行失败", e);
       return "Error: " + e.getMessage();
   }

2. 添加重试机制
   @Retryable(maxAttempts = 3)
   public String executeTool(...) {
       // ...
   }

3. 降级处理
   if (toolFailed) {
       return "工具暂时不可用,已切换到备用方案";
   }
```

---

## 7.3 集成问题

### 问题1: SSE连接中断
```
现象: 流式输出中途断开

原因:
1. 网络超时
2. 代理服务器限制
3. 客户端断开

解决方案:
1. 心跳保活
   @Scheduled(fixedRate = 10000)
   public void sendHeartbeat() {
       sseEmitter.send(SseEmitter.event()
           .name("heartbeat")
           .data("ping"));
   }

2. 错误重连
   // 前端代码
   const eventSource = new EventSource(url);
   eventSource.onerror = () => {
       setTimeout(() => reconnect(), 1000);
   };

3. 超时配置
   @Bean
   public SseEmitter createEmitter() {
       SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
       return emitter;
   }
```

---

## 7.4 调试技巧

### 技巧1: 完整的日志记录
```java
@Slf4j
public class BaseAgent {
    
    @Override
    public final String run(String query) {
        log.info("=== Agent开始执行 ===");
        log.info("Query: {}", query);
        log.info("Agent: {}", getName());
        
        String result = null;
        try {
            while (currentStep < maxSteps) {
                log.info("--- Step {} ---", currentStep);
                
                result = step();
                
                log.info("Step {} 完成", currentStep);
                log.info("Result: {}", result);
                
                if (state == AgentState.FINISHED) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Agent执行异常", e);
            throw e;
        } finally {
            log.info("=== Agent执行结束 ===");
        }
        
        return result;
    }
}
```

### 技巧2: 单元测试覆盖
```java
@SpringBootTest
public class AgentTest {
    
    @Test
    public void testReactAgent() {
        // 1. 准备测试数据
        AgentContext context = createTestContext();
        ReactImplAgent agent = new ReactImplAgent(context);
        
        // 2. 执行测试
        String result = agent.run("测试查询");
        
        // 3. 验证结果
        assertNotNull(result);
        assertTrue(result.contains("期望的内容"));
        
        // 4. 验证状态
        assertEquals(AgentState.FINISHED, agent.getState());
    }
}
```

---

# 附录: 快速参考

## A. 核心类清单
```
✓ BaseAgent.java - Agent基类
✓ ReactImplAgent.java - ReAct实现
✓ PlanningAgent.java - 规划Agent
✓ ExecutorAgent.java - 执行Agent
✓ BaseTool.java - 工具接口
✓ ToolCollection.java - 工具集合
✓ AgentHandler.java - Handler接口
✓ AgentHandlerFactory.java - Handler工厂
✓ Memory.java - 记忆管理
✓ LLM.java - LLM适配
```

## B. 配置参数清单
```yaml
agent:
  max-steps: 10              # 最大执行步数
  timeout: 60000             # 超时时间(ms)
  concurrent-tools: 5        # 并发工具数

llm:
  max-tokens: 8192          # 最大token数
  temperature: 0.7          # 温度参数
  timeout: 30000            # 超时时间(ms)

tool:
  service-url: http://localhost:1601
  timeout: 30000
```

## C. API端点清单
```
POST   /api/chat           # 普通聊天
GET    /api/chat/stream    # SSE流式
POST   /api/session/create # 创建会话
DELETE /api/session/{id}   # 删除会话
GET    /api/tools/list     # 工具列表
```

---

# 总结

本文档提供了完整的JoyAgent多智能体系统设计方案，包括:

1. ✅ **清晰的架构设计** - 分层架构、职责明确
2. ✅ **完整的代码模板** - 可直接使用的实现代码
3. ✅ **详细的实施指南** - 从零构建或改造现有项目
4. ✅ **实用的问题解决** - 常见问题和解决方案

**使用建议**:
- AI Agent可以直接参考本文档理解系统设计
- 开发者可以复制代码模板快速实现
- 按照改造指南逐步迁移现有系统
- 参考问题解决章节避免踩坑

**文档维护**:
- 版本: v1.0
- 更新日期: 2025-11-06
- 适用于: JoyAgent及类似多智能体系统

---

END OF DOCUMENT


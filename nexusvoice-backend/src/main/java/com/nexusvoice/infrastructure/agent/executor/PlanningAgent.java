package com.nexusvoice.infrastructure.agent.executor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nexusvoice.domain.agent.enums.TaskStatus;
import com.nexusvoice.infrastructure.agent.registry.ToolRegistryImpl;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import com.nexusvoice.domain.agent.model.*;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 规划Agent
 * 
 * 职责：
 * - 接收复杂任务
 * - 拆解为多个子任务
 * - 识别任务依赖关系
 * - 标记可并行任务
 * - 生成可执行计划
 * 
 * 输出：PlanExecution对象
 */
@Slf4j
@Component
public class PlanningAgent {
    
    @Autowired
    private AiChatService aiChatService;
    
    @Autowired
    private ToolRegistryImpl toolRegistry;
    
    /**
     * 创建任务计划
     * 
     * @param query 用户查询
     * @param agent Agent配置
     * @param context 执行上下文
     * @return 任务计划
     */
    public PlanExecution createPlan(String query, Agent agent, AgentContext context) {
        log.info("开始规划任务，查询：{}", query);
        
        try {
            // 1. 调用LLM生成计划
            String planText = generatePlanWithLLM(query, agent);
            
            // 2. 解析计划文本
            PlanExecution plan = parsePlan(planText, query);
            
            // 3. 验证和优化
            validateAndOptimizePlan(plan);
            
            // 4. 记录到上下文
            context.setVariable("plan", plan);
            context.addMessage(AgentMessage.assistant("已生成任务计划，共" + plan.getTasks().size() + "个任务"));
            
            log.info("任务规划完成，共{}个任务", plan.getTasks().size());
            
            return plan;
            
        } catch (Exception e) {
            log.error("任务规划失败", e);
            throw new RuntimeException("任务规划失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 使用LLM生成计划
     */
    private String generatePlanWithLLM(String query, Agent agent) {
        String planningPrompt = buildPlanningPrompt(query, agent);
        
        // 获取模型名称（从Agent配置或使用默认值）
        String modelName = agent.getConfig() != null && agent.getConfig().getModelName() != null
            ? agent.getConfig().getModelName()
            : "deepseek:deepseek-v3.1";  // 默认使用DeepSeek V3.1
        
        ChatRequest request = ChatRequest.builder()
            .messages(List.of(
                ChatMessage.system(agent.getSystemPrompt()),
                ChatMessage.user(planningPrompt)
            ))
            .model(modelName)
            .temperature(0.3)  // 较低温度，保持计划稳定
            .maxTokens(2000)
            .build();
        
        ChatResponse response = aiChatService.chat(request);
        
        if (!response.getSuccess()) {
            throw new RuntimeException("LLM调用失败：" + response.getErrorMessage());
        }
        
        return response.getContent();
    }
    
    /**
     * 构建规划提示词（针对DeepSeek V3.1优化）
     */
    private String buildPlanningPrompt(String query, Agent agent) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个任务规划专家。请将复杂任务拆解为可执行的子任务序列。\n\n");
        
        prompt.append("【用户任务】\n");
        prompt.append(query).append("\n\n");
        
        prompt.append("【可用工具】\n");
        prompt.append(buildToolDescriptions(agent)).append("\n");
        
        prompt.append("【规划方法】\n");
        prompt.append("1️⃣ 分析任务目标和所需信息\n");
        prompt.append("2️⃣ 识别可独立完成的子任务\n");
        prompt.append("3️⃣ 确定任务间的依赖关系\n");
        prompt.append("4️⃣ 标记可并行执行的任务\n");
        prompt.append("5️⃣ 为每个任务分配合适的工具\n\n");
        
        prompt.append("【输出格式】JSON\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"description\": \"计划整体描述（简短）\",\n");
        prompt.append("  \"tasks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"taskId\": \"task_1\",\n");
        prompt.append("      \"description\": \"具体任务描述（清晰明确）\",\n");
        prompt.append("      \"toolName\": \"工具名称\",\n");
        prompt.append("      \"toolParams\": {\"参数名\": \"参数值\"},\n");
        prompt.append("      \"dependencies\": [],  // 依赖的taskId列表\n");
        prompt.append("      \"canParallel\": false,  // 是否可与其他任务并行\n");
        prompt.append("      \"executionOrder\": 1\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        
        prompt.append("【规划原则】\n");
        prompt.append("✓ 任务粒度：每个任务只做一件事，职责单一\n");
        prompt.append("✓ 依赖关系：明确标注dependencies，确保执行顺序\n");
        prompt.append("✓ 并行优化：独立任务设置canParallel=true，提升效率\n");
        prompt.append("✓ 工具选择：为每个任务选择最合适的工具和参数\n");
        prompt.append("✓ 执行顺序：按executionOrder从小到大排序\n");
        prompt.append("✓ 数量控制：任务数建议3-8个，不超过10个\n\n");
        
        prompt.append("⚠️ 重要：仅返回JSON，不要包含其他文字！\n");
        
        return prompt.toString();
    }
    
    /**
     * 构建工具描述
     */
    private String buildToolDescriptions(Agent agent) {
        if (agent == null || agent.getAvailableTools() == null || agent.getAvailableTools().isEmpty()) {
            return "无可用工具";
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (String toolName : agent.getAvailableTools()) {
            if (toolName == null || toolName.trim().isEmpty()) {
                continue;
            }
            
            com.nexusvoice.infrastructure.agent.tool.BaseTool tool = toolRegistry.getBaseTool(toolName);
            if (tool == null) {
                log.warn("工具未找到：{}", toolName);
                continue;
            }
            
            sb.append(String.format("- %s: %s\n", tool.getName(), tool.getDescription()));
            
            if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                sb.append("  参数：\n");
                for (ToolParameter param : tool.getParameters()) {
                    sb.append(String.format("    • %s (%s)%s: %s\n",
                        param.getName(),
                        param.getType(),
                        param.getRequired() ? " [必需]" : "",
                        param.getDescription()
                    ));
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 解析计划文本
     */
    private PlanExecution parsePlan(String planText, String originalQuery) {
        try {
            // 提取JSON
            String json = extractJson(planText);
            
            // 解析JSON
            Map<String, Object> planMap = JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {});
            
            // 创建PlanExecution
            PlanExecution plan = PlanExecution.builder()
                .planId(UUID.randomUUID().toString())
                .description((String) planMap.get("description"))
                .originalQuery(originalQuery)
                .tasks(new ArrayList<>())
                .createdAt(System.currentTimeMillis())
                .build();
            
            // 解析任务列表
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> taskMaps = (List<Map<String, Object>>) planMap.get("tasks");
            
            if (taskMaps != null) {
                for (Map<String, Object> taskMap : taskMaps) {
                    AgentTask task = parseTask(taskMap);
                    plan.addTask(task);
                }
            }
            
            return plan;
            
        } catch (Exception e) {
            log.error("解析计划失败：{}", planText, e);
            throw new RuntimeException("解析计划失败", e);
        }
    }
    
    /**
     * 解析单个任务
     */
    private AgentTask parseTask(Map<String, Object> taskMap) {
        // 解析依赖列表
        @SuppressWarnings("unchecked")
        List<String> dependencies = (List<String>) taskMap.getOrDefault("dependencies", new ArrayList<>());
        
        // 解析工具参数
        @SuppressWarnings("unchecked")
        Map<String, Object> toolParams = (Map<String, Object>) taskMap.get("toolParams");
        String toolParamsJson = toolParams != null ? JsonUtils.toJson(toolParams) : "{}";
        
        return AgentTask.builder()
            .taskId((String) taskMap.get("taskId"))
            .description((String) taskMap.get("description"))
            .toolName((String) taskMap.get("toolName"))
            .toolParams(toolParamsJson)
            .dependencies(dependencies)
            .canParallel(Boolean.TRUE.equals(taskMap.get("canParallel")))
            .executionOrder(getInteger(taskMap.get("executionOrder")))
            .status(TaskStatus.PENDING)
            .retryCount(0)
            .maxRetries(3)
            .build();
    }
    
    /**
     * 提取JSON内容
     */
    private String extractJson(String text) {
        // 尝试提取 ```json ... ``` 或 ``` ... ``` 代码块
        if (text.contains("```json")) {
            int start = text.indexOf("```json") + 7;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        } else if (text.contains("```")) {
            int start = text.indexOf("```") + 3;
            int end = text.indexOf("```", start);
            if (end > start) {
                return text.substring(start, end).trim();
            }
        }
        
        // 尝试提取 { ... } JSON对象
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end > start) {
            return text.substring(start, end + 1).trim();
        }
        
        // 直接返回原文本
        return text.trim();
    }
    
    /**
     * 验证和优化计划
     */
    private void validateAndOptimizePlan(PlanExecution plan) {
        if (plan.getTasks() == null || plan.getTasks().isEmpty()) {
            throw new RuntimeException("计划中没有任务");
        }
        
        // 验证任务ID唯一性
        Set<String> taskIds = new HashSet<>();
        for (AgentTask task : plan.getTasks()) {
            if (taskIds.contains(task.getTaskId())) {
                throw new RuntimeException("任务ID重复：" + task.getTaskId());
            }
            taskIds.add(task.getTaskId());
        }
        
        // 验证依赖关系有效性
        for (AgentTask task : plan.getTasks()) {
            if (task.getDependencies() != null) {
                for (String depId : task.getDependencies()) {
                    if (!taskIds.contains(depId)) {
                        log.warn("任务{}依赖的任务{}不存在，已移除", task.getTaskId(), depId);
                        task.getDependencies().remove(depId);
                    }
                }
            }
        }
        
        // 检测循环依赖
        detectCircularDependencies(plan);
        
        log.info("计划验证通过");
    }
    
    /**
     * 检测循环依赖
     */
    private void detectCircularDependencies(PlanExecution plan) {
        // 使用拓扑排序检测循环依赖
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        
        // 构建图
        for (AgentTask task : plan.getTasks()) {
            String taskId = task.getTaskId();
            graph.putIfAbsent(taskId, new HashSet<>());
            inDegree.putIfAbsent(taskId, 0);
            
            if (task.getDependencies() != null) {
                for (String dep : task.getDependencies()) {
                    graph.get(dep).add(taskId);
                    inDegree.put(taskId, inDegree.get(taskId) + 1);
                }
            }
        }
        
        // 拓扑排序
        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }
        
        int processedCount = 0;
        while (!queue.isEmpty()) {
            String taskId = queue.poll();
            processedCount++;
            
            for (String nextTask : graph.get(taskId)) {
                inDegree.put(nextTask, inDegree.get(nextTask) - 1);
                if (inDegree.get(nextTask) == 0) {
                    queue.offer(nextTask);
                }
            }
        }
        
        if (processedCount != plan.getTasks().size()) {
            throw new RuntimeException("检测到循环依赖");
        }
    }
    
    /**
     * 安全获取Integer值
     */
    private Integer getInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}


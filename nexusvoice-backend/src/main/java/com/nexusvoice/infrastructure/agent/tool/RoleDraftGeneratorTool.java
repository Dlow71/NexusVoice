package com.nexusvoice.infrastructure.agent.tool;

import com.nexusvoice.application.role.dto.RoleBriefDto;
import com.nexusvoice.domain.agent.model.ToolParameter;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.model.ChatMessage;
import com.nexusvoice.infrastructure.ai.model.ChatRequest;
import com.nexusvoice.infrastructure.ai.model.ChatResponse;
import com.nexusvoice.infrastructure.ai.service.AiChatService;
import com.nexusvoice.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 角色草稿生成工具
 * 
 * 职责：
 * - 根据对话上下文生成角色草稿
 * - 提供结构化的角色定义
 */
@Slf4j
@Component
public class RoleDraftGeneratorTool implements BaseTool {

    private static final String ROLE_BRIEF_MODEL_NAME = "deepseek:deepseek-v3.2-251201";
    
    @Autowired
    private AiChatService aiChatService;
    
    @Override
    public String getName() {
        return "role_draft_generator";
    }
    
    @Override
    public String getDescription() {
        return "根据对话内容或角色描述，生成结构化的AI角色草稿。" +
               "包括角色名称、描述、人设提示词、开场白等字段。";
    }
    
    @Override
    public List<ToolParameter> getParameters() {
        return List.of(
            ToolParameter.builder()
                .name("conversation_summary")
                .type("string")
                .description("对话摘要或角色描述")
                .required(true)
                .example("用户想要创建一个擅长讲解哈利波特魔法知识的角色...")
                .build(),
            ToolParameter.builder()
                .name("style_hints")
                .type("string")
                .description("风格提示（可选）")
                .required(false)
                .example("温和、耐心、避免剧透")
                .build()
        );
    }
    
    @Override
    public String execute(Map<String, Object> parameters) {
        try {
            String conversationSummary = (String) parameters.get("conversation_summary");
            if (conversationSummary == null || conversationSummary.trim().isEmpty()) {
                throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "对话摘要不能为空");
            }
            
            String styleHints = (String) parameters.getOrDefault("style_hints", "");
            
            // 构造提示词（针对DeepSeek V3.1优化）
            String systemPrompt = """
                你是一个AI角色设计专家，擅长创建生动、有趣、符合设定的AI角色。
                
                【设计原则】
                ✓ 个性鲜明：角色要有独特的性格和说话风格
                ✓ 背景丰富：提供合理的背景故事和知识领域
                ✓ 边界清晰：明确角色能做什么、不能做什么
                ✓ 用户友好：开场白要温暖、引导性强
                
                【输出格式】严格JSON格式
                {
                  "name": "角色名称（简洁有记忆点）",
                  "description": "角色一句话描述（20-50字）",
                  "personaPrompt": "详细人设（200-500字，包含：角色背景、性格特点、说话风格、知识范围、行为准则、禁忌话题）",
                  "greetingMessage": "开场白（20-50字，友好亲切）",
                  "voiceType": "qiniu_zh_female_dmytwz",
                  "disclaimers": ["版权声明", "内容边界说明"]
                }
                
                【质量要求】
                1. 名称：简洁、有辨识度、避免具体IP专有名词
                2. 描述：一句话概括角色核心特征
                3. 人设：详细、具体、可操作，包含说话风格和知识边界
                4. 开场白：温暖、引导用户开始对话、体现角色性格
                5. 声明：包含版权、内容适用范围、禁止事项
                
                ⚠️ 重要：仅返回JSON，不要包含其他文字！
                """;
            
            String userPrompt = String.format(
                "需求描述：\n%s\n\n风格提示：\n%s\n\n请生成角色草稿（仅返回JSON）",
                conversationSummary,
                styleHints
            );
            
            // 调用AI生成草稿（默认使用DeepSeek V3.1）
            String modelName = ROLE_BRIEF_MODEL_NAME;
            
            ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                    ChatMessage.system(systemPrompt),
                    ChatMessage.user(userPrompt)
                ))
                .model(modelName)
                .temperature(0.7)
                .maxTokens(1200)
                .build();
            
            ChatResponse response = aiChatService.chat(request);
            
            if (!response.getSuccess()) {
                throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, 
                    "生成角色草稿失败：" + response.getErrorMessage());
            }
            
            // 解析并验证JSON
            String content = response.getContent();
            RoleBriefDto draft = parseAndValidateDraft(content);
            
            log.info("成功生成角色草稿：{}", draft.getName());
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("success", true);
            result.put("draft", draft);
            return JsonUtils.toJson(result);
            
        } catch (BizException e) {
            log.error("角色草稿生成失败", e);
            throw e;
        } catch (Exception e) {
            log.error("角色草稿生成异常", e);
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, "生成失败：" + e.getMessage());
        }
    }
    
    /**
     * 解析并验证草稿JSON
     */
    private RoleBriefDto parseAndValidateDraft(String jsonContent) {
        try {
            // 提取JSON（可能包含markdown代码块）
            String json = extractJson(jsonContent);
            
            // 解析为DTO
            RoleBriefDto draft = JsonUtils.fromJson(json, RoleBriefDto.class);
            
            // 验证必要字段
            if (draft.getName() == null || draft.getName().isEmpty()) {
                draft.setName("AI助手");
            }
            if (draft.getPersonaPrompt() == null || draft.getPersonaPrompt().isEmpty()) {
                throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, "生成的人设提示词为空");
            }
            
            return draft;
            
        } catch (Exception e) {
            log.error("解析角色草稿JSON失败：{}", jsonContent, e);
            throw BizException.of(ErrorCodeEnum.AI_SERVICE_ERROR, "解析草稿失败");
        }
    }
    
    /**
     * 提取JSON内容（去除markdown代码块）
     */
    private String extractJson(String content) {
        if (content.contains("```json")) {
            int start = content.indexOf("```json") + 7;
            int end = content.indexOf("```", start);
            return content.substring(start, end).trim();
        } else if (content.contains("```")) {
            int start = content.indexOf("```") + 3;
            int end = content.indexOf("```", start);
            return content.substring(start, end).trim();
        }
        return content.trim();
    }
    
    @Override
    public String getCategory() {
        return "content_generation";
    }
    
    @Override
    public Long getEstimatedDurationMs() {
        return 8000L;
    }
    
    @Override
    public Integer getPriority() {
        return 3;
    }
}

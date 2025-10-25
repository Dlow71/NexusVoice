package com.nexusvoice.domain.ai.model;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.utils.JsonUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI模型配置实体
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class AiModel extends BaseDomainEntity {
    
    /**
     * 厂商代码：openai/claude/qwen等
     */
    private String providerCode;
    
    /**
     * 模型代码：gpt-4o-mini/claude-3-opus等
     */
    private String modelCode;
    
    /**
     * 模型类型：chat/embedding/rerank
     */
    private String modelType;
    
    /**
     * 模型显示名称
     */
    private String modelName;
    
    /**
     * 模型描述
     */
    private String description;
    
    /**
     * LangChain4j模型类名
     * 如：OpenAiChatModel/ClaudeChatModel
     */
    private String modelClass;
    
    /**
     * 默认API端点
     */
    private String defaultBaseUrl;
    
    /**
     * 默认温度参数
     */
    private BigDecimal defaultTemperature;
    
    /**
     * 默认最大tokens
     */
    private Integer defaultMaxTokens;
    
    /**
     * 默认超时时间（秒）
     */
    private Integer defaultTimeoutSeconds;
    
    /**
     * 上下文窗口大小
     */
    private Integer contextWindow;
    
    /**
     * 输入token单价（元/千tokens）
     */
    private BigDecimal inputTokenPrice;
    
    /**
     * 输出token单价（元/千tokens）
     */
    private BigDecimal outputTokenPrice;
    
    /**
     * 额外配置JSON
     */
    private String configJson;
    
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
    
    /**
     * 优先级（越小越优先）
     */
    private Integer priority;
    
    /**
     * 模型支持的能力列表（数组）
     * 如：ocr, vision, thinking, tool_call, multimodal
     */
    private String[] capabilities;
    
    /**
     * 支持的输入类型（数组）
     * 如：text, image, video, audio
     */
    private String[] inputTypes;
    
    /**
     * 支持的输出类型（数组）
     * 如：text, image, audio
     */
    private String[] outputTypes;
    
    /**
     * 获取模型唯一标识
     */
    public String getModelKey() {
        return providerCode + ":" + modelCode;
    }
    
    /**
     * 计算费用
     * @param inputTokens 输入token数
     * @param outputTokens 输出token数
     * @return 总费用（元）
     */
    public BigDecimal calculateCost(int inputTokens, int outputTokens) {
        BigDecimal inputCost = BigDecimal.ZERO;
        BigDecimal outputCost = BigDecimal.ZERO;
        
        if (inputTokenPrice != null) {
            inputCost = inputTokenPrice.multiply(BigDecimal.valueOf(inputTokens))
                    .divide(BigDecimal.valueOf(1000), 6, java.math.RoundingMode.HALF_UP);
        }
        
        if (outputTokenPrice != null) {
            outputCost = outputTokenPrice.multiply(BigDecimal.valueOf(outputTokens))
                    .divide(BigDecimal.valueOf(1000), 6, java.math.RoundingMode.HALF_UP);
        }
        
        return inputCost.add(outputCost);
    }
    
    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }
    
    /**
     * 获取模型类型枚举
     */
    public AiModelType getModelTypeEnum() {
        return AiModelType.fromCode(modelType);
    }
    
    /**
     * 是否为对话模型
     */
    public boolean isChatModel() {
        AiModelType type = getModelTypeEnum();
        return type != null && type.isChat();
    }
    
    /**
     * 是否为向量模型
     */
    public boolean isEmbeddingModel() {
        AiModelType type = getModelTypeEnum();
        return type != null && type.isEmbedding();
    }
    
    /**
     * 是否为重排序模型
     */
    public boolean isRerankModel() {
        AiModelType type = getModelTypeEnum();
        return type != null && type.isRerank();
    }
    
    /**
     * 是否为图像生成模型
     */
    public boolean isImageModel() {
        AiModelType type = getModelTypeEnum();
        return type != null && type.isImage();
    }
    
    /**
     * 获取配置Map
     */
    public Map<String, Object> getConfigMap() {
        return JsonUtils.toMap(configJson);
    }
    
    /**
     * 获取能力列表
     */
    public List<String> getCapabilitiesList() {
        return capabilities != null ? Arrays.asList(capabilities) : Collections.emptyList();
    }
    
    /**
     * 获取输入类型列表
     */
    public List<String> getInputTypesList() {
        return inputTypes != null ? Arrays.asList(inputTypes) : Collections.emptyList();
    }
    
    /**
     * 获取输出类型列表
     */
    public List<String> getOutputTypesList() {
        return outputTypes != null ? Arrays.asList(outputTypes) : Collections.emptyList();
    }
    
    /**
     * 检查是否支持指定能力
     * @param capability 能力名称（ocr/vision/thinking/tool_call/multimodal）
     * @return 是否支持
     */
    public boolean hasCapability(String capability) {
        if (capabilities == null || capability == null) {
            return false;
        }
        return Arrays.asList(capabilities).contains(capability.toLowerCase());
    }
    
    /**
     * 检查是否支持OCR能力
     */
    public boolean supportsOcr() {
        return hasCapability("ocr");
    }
    
    /**
     * 检查是否支持视觉理解能力
     */
    public boolean supportsVision() {
        return hasCapability("vision");
    }
    
    /**
     * 检查是否支持深度思考能力
     */
    public boolean supportsThinking() {
        return hasCapability("thinking");
    }
    
    /**
     * 检查是否支持工具调用能力
     */
    public boolean supportsToolCall() {
        return hasCapability("tool_call");
    }
    
    /**
     * 检查是否为多模态模型
     */
    public boolean isMultimodal() {
        return hasCapability("multimodal");
    }
    
    /**
     * 检查是否支持指定输入类型
     * @param inputType 输入类型（text/image/video/audio）
     * @return 是否支持
     */
    public boolean supportsInputType(String inputType) {
        if (inputTypes == null || inputType == null) {
            return false;
        }
        return Arrays.asList(inputTypes).contains(inputType.toLowerCase());
    }
    
    /**
     * 检查是否支持图像输入
     */
    public boolean supportsImageInput() {
        return supportsInputType("image");
    }
    
    /**
     * 检查是否支持视频输入
     */
    public boolean supportsVideoInput() {
        return supportsInputType("video");
    }
    
    /**
     * 检查是否支持音频输入
     */
    public boolean supportsAudioInput() {
        return supportsInputType("audio");
    }
}

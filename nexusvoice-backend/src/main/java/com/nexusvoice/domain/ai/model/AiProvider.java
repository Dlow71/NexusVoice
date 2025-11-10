package com.nexusvoice.domain.ai.model;

import com.nexusvoice.domain.common.BaseDomainEntity;
import com.nexusvoice.utils.JsonUtils;

import java.util.Map;

/**
 * AI服务提供商领域实体
 * 代表一个AI服务提供商（如OpenAI、DeepSeek、Grok等）
 * 包含服务商级别的配置信息
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
public class AiProvider extends BaseDomainEntity {
    
    /**
     * 厂商代码：openai/grok/deepseek等（唯一标识）
     */
    private String providerCode;
    
    /**
     * 厂商显示名称
     */
    private String providerName;
    
    /**
     * 协议类型：openai_compatible/anthropic/dashscope等
     */
    private String protocol;
    
    /**
     * 厂商描述
     */
    private String description;
    
    /**
     * 默认API端点URL
     */
    private String defaultBaseUrl;
    
    /**
     * 厂商特定配置JSON
     * 可存储：超时配置、重试策略、特殊参数等
     */
    private String configJson;
    
    /**
     * 是否官方内置服务商
     */
    private Boolean isOfficial;
    
    /**
     * 用户ID（自定义服务商的创建者）
     * 官方服务商此字段为null
     */
    private Long userId;
    
    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
    
    /**
     * 优先级（越小越优先）
     */
    private Integer priority;
    
    // ========== 业务方法 ==========
    
    /**
     * 是否启用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }
    
    /**
     * 是否为官方服务商
     */
    public boolean isOfficialProvider() {
        return isOfficial != null && isOfficial;
    }
    
    /**
     * 是否为用户自定义服务商
     */
    public boolean isUserCustomProvider() {
        return !isOfficialProvider() && userId != null;
    }
    
    /**
     * 获取协议枚举
     */
    public ProviderProtocol getProtocolEnum() {
        return ProviderProtocol.fromCode(protocol);
    }
    
    /**
     * 检查是否为OpenAI兼容协议
     */
    public boolean isOpenAiCompatible() {
        return getProtocolEnum().isOpenAiCompatible();
    }
    
    /**
     * 获取配置JSON的Map表示
     * 
     * @return 配置Map，如果configJson为空则返回空Map
     */
    public Map<String, Object> getConfigMap() {
        return JsonUtils.toMap(configJson);
    }
    
    /**
     * 启用服务商
     */
    public void enable() {
        this.status = 1;
        this.onUpdate();
    }
    
    /**
     * 禁用服务商
     */
    public void disable() {
        this.status = 0;
        this.onUpdate();
    }
    
    /**
     * 验证服务商信息是否完整
     * 
     * @throws IllegalStateException 如果验证失败
     */
    public void validate() {
        if (providerCode == null || providerCode.trim().isEmpty()) {
            throw new IllegalStateException("服务商代码不能为空");
        }
        
        if (providerName == null || providerName.trim().isEmpty()) {
            throw new IllegalStateException("服务商名称不能为空");
        }
        
        if (protocol == null || protocol.trim().isEmpty()) {
            throw new IllegalStateException("协议类型不能为空");
        }
        
        // 官方服务商不能有userId
        if (Boolean.TRUE.equals(isOfficial) && userId != null) {
            throw new IllegalStateException("官方服务商不能关联用户ID");
        }
        
        // 用户自定义服务商必须有userId
        if (Boolean.FALSE.equals(isOfficial) && userId == null) {
            throw new IllegalStateException("用户自定义服务商必须关联用户ID");
        }
    }
    
    /**
     * 检查当前用户是否有权限操作此服务商
     * 
     * @param currentUserId 当前用户ID
     * @return true-有权限，false-无权限
     */
    public boolean hasPermission(Long currentUserId) {
        // 官方服务商只能由管理员操作（在Service层判断）
        if (isOfficialProvider()) {
            return false;
        }
        
        // 用户自定义服务商只能由创建者操作
        return userId != null && userId.equals(currentUserId);
    }
    
    // ========== Getter and Setter ==========
    
    public String getProviderCode() {
        return providerCode;
    }
    
    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDefaultBaseUrl() {
        return defaultBaseUrl;
    }
    
    public void setDefaultBaseUrl(String defaultBaseUrl) {
        this.defaultBaseUrl = defaultBaseUrl;
    }
    
    public String getConfigJson() {
        return configJson;
    }
    
    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
    
    public Boolean getIsOfficial() {
        return isOfficial;
    }
    
    public void setIsOfficial(Boolean isOfficial) {
        this.isOfficial = isOfficial;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public Integer getStatus() {
        return status;
    }
    
    public void setStatus(Integer status) {
        this.status = status;
    }
    
    public Integer getPriority() {
        return priority;
    }
    
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    
    @Override
    public String toString() {
        return "AiProvider{" +
                "id=" + id +
                ", providerCode='" + providerCode + '\'' +
                ", providerName='" + providerName + '\'' +
                ", protocol='" + protocol + '\'' +
                ", isOfficial=" + isOfficial +
                ", status=" + status +
                ", priority=" + priority +
                '}';
    }
}

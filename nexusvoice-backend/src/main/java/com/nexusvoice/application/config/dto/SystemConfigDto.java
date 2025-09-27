package com.nexusvoice.application.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 系统配置DTO
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Schema(description = "系统配置信息")
public class SystemConfigDto {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "配置键", example = "ai.model.default")
    private String configKey;

    @Schema(description = "配置值", example = "gpt-4")
    private String configValue;

    @Schema(description = "配置描述", example = "默认AI模型")
    private String description;

    @Schema(description = "配置分组", example = "ai")
    private String configGroup;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "是否只读", example = "false")
    private Boolean readonly;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // 构造函数
    public SystemConfigDto() {
    }

    // Getter and Setter methods
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getConfigGroup() {
        return configGroup;
    }

    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getReadonly() {
        return readonly;
    }

    public void setReadonly(Boolean readonly) {
        this.readonly = readonly;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

package com.nexusvoice.application.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 系统配置更新请求DTO
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Schema(description = "系统配置更新请求")
public class SystemConfigUpdateRequest {

    @Schema(description = "配置ID", required = true)
    @NotNull(message = "配置ID不能为空")
    private Long id;

    @Schema(description = "配置键", example = "ai.model.default", required = true)
    @NotBlank(message = "配置键不能为空")
    @Size(max = 100, message = "配置键长度不能超过100个字符")
    private String configKey;

    @Schema(description = "配置值", example = "gpt-4", required = true)
    @NotBlank(message = "配置值不能为空")
    @Size(max = 1000, message = "配置值长度不能超过1000个字符")
    private String configValue;

    @Schema(description = "配置描述", example = "默认AI模型", required = true)
    @NotBlank(message = "配置描述不能为空")
    @Size(max = 200, message = "配置描述长度不能超过200个字符")
    private String description;

    @Schema(description = "配置分组", example = "ai")
    @Size(max = 50, message = "配置分组长度不能超过50个字符")
    private String configGroup;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "是否只读", example = "false")
    private Boolean readonly;

    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

    // 构造函数
    public SystemConfigUpdateRequest() {
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
}

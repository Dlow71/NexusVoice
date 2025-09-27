package com.nexusvoice.domain.config.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.nexusvoice.domain.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统配置领域实体
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Schema(description = "系统配置")
@TableName("system_config")
public class SystemConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

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

    // 构造函数
    public SystemConfig() {
        super();
    }

    public SystemConfig(String configKey, String configValue, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.enabled = true;
        this.readonly = false;
        this.sortOrder = 0;
    }

    // Getter and Setter methods
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

    /**
     * 验证配置键是否有效
     */
    public boolean isValidKey() {
        return configKey != null && !configKey.trim().isEmpty();
    }

    /**
     * 检查配置是否可以修改
     */
    public boolean isModifiable() {
        return !Boolean.TRUE.equals(readonly);
    }

    /**
     * 检查配置是否启用
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(enabled);
    }

    @Override
    public String toString() {
        return "SystemConfig{" +
                "id=" + getId() +
                ", configKey='" + configKey + '\'' +
                ", configValue='" + configValue + '\'' +
                ", description='" + description + '\'' +
                ", configGroup='" + configGroup + '\'' +
                ", enabled=" + enabled +
                ", readonly=" + readonly +
                ", sortOrder=" + sortOrder +
                ", remark='" + remark + '\'' +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }
}

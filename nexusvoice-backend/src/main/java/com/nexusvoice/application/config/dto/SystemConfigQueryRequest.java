package com.nexusvoice.application.config.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 系统配置查询请求DTO
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Schema(description = "系统配置查询请求")
public class SystemConfigQueryRequest {

    @Schema(description = "配置键（模糊查询）", example = "ai")
    private String configKey;

    @Schema(description = "配置分组", example = "ai")
    private String configGroup;

    @Schema(description = "是否启用", example = "true")
    private Boolean enabled;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    // 构造函数
    public SystemConfigQueryRequest() {
    }

    // Getter and Setter methods
    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
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

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}

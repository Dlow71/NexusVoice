package com.nexusvoice.application.config.assembler;

import com.nexusvoice.application.config.dto.SystemConfigCreateRequest;
import com.nexusvoice.application.config.dto.SystemConfigDto;
import com.nexusvoice.application.config.dto.SystemConfigUpdateRequest;
import com.nexusvoice.domain.config.model.SystemConfig;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置对象转换器
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
public class SystemConfigAssembler {

    /**
     * 领域对象转DTO
     */
    public static SystemConfigDto toDto(SystemConfig systemConfig) {
        if (systemConfig == null) {
            return null;
        }

        SystemConfigDto dto = new SystemConfigDto();
        dto.setId(systemConfig.getId());
        dto.setConfigKey(systemConfig.getConfigKey());
        dto.setConfigValue(systemConfig.getConfigValue());
        dto.setDescription(systemConfig.getDescription());
        dto.setConfigGroup(systemConfig.getConfigGroup());
        dto.setEnabled(systemConfig.getEnabled());
        dto.setReadonly(systemConfig.getReadonly());
        dto.setSortOrder(systemConfig.getSortOrder());
        dto.setRemark(systemConfig.getRemark());
        dto.setCreatedAt(systemConfig.getCreatedAt());
        dto.setUpdatedAt(systemConfig.getUpdatedAt());

        return dto;
    }

    /**
     * 领域对象列表转DTO列表
     */
    public static List<SystemConfigDto> toDtoList(List<SystemConfig> systemConfigs) {
        if (systemConfigs == null) {
            return null;
        }

        return systemConfigs.stream()
                .map(SystemConfigAssembler::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 创建请求转领域对象
     */
    public static SystemConfig toDomain(SystemConfigCreateRequest request) {
        if (request == null) {
            return null;
        }

        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setConfigKey(request.getConfigKey());
        systemConfig.setConfigValue(request.getConfigValue());
        systemConfig.setDescription(request.getDescription());
        systemConfig.setConfigGroup(request.getConfigGroup());
        systemConfig.setEnabled(request.getEnabled());
        systemConfig.setReadonly(request.getReadonly());
        systemConfig.setSortOrder(request.getSortOrder());
        systemConfig.setRemark(request.getRemark());

        return systemConfig;
    }

    /**
     * 更新请求转领域对象
     */
    public static SystemConfig toDomain(SystemConfigUpdateRequest request) {
        if (request == null) {
            return null;
        }

        SystemConfig systemConfig = new SystemConfig();
        systemConfig.setId(request.getId());
        systemConfig.setConfigKey(request.getConfigKey());
        systemConfig.setConfigValue(request.getConfigValue());
        systemConfig.setDescription(request.getDescription());
        systemConfig.setConfigGroup(request.getConfigGroup());
        systemConfig.setEnabled(request.getEnabled());
        systemConfig.setReadonly(request.getReadonly());
        systemConfig.setSortOrder(request.getSortOrder());
        systemConfig.setRemark(request.getRemark());

        return systemConfig;
    }

    /**
     * 更新领域对象属性
     */
    public static void updateDomain(SystemConfig target, SystemConfigUpdateRequest request) {
        if (target == null || request == null) {
            return;
        }

        target.setConfigKey(request.getConfigKey());
        target.setConfigValue(request.getConfigValue());
        target.setDescription(request.getDescription());
        target.setConfigGroup(request.getConfigGroup());
        target.setEnabled(request.getEnabled());
        target.setReadonly(request.getReadonly());
        target.setSortOrder(request.getSortOrder());
        target.setRemark(request.getRemark());
    }
}

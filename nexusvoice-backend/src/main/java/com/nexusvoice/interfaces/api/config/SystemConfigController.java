package com.nexusvoice.interfaces.api.config;

import com.nexusvoice.application.config.dto.SystemConfigCreateRequest;
import com.nexusvoice.application.config.dto.SystemConfigDto;
import com.nexusvoice.application.config.dto.SystemConfigQueryRequest;
import com.nexusvoice.application.config.dto.SystemConfigUpdateRequest;
import com.nexusvoice.application.config.service.SystemConfigApplicationService;
import com.nexusvoice.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Tag(name = "系统配置管理", description = "系统配置相关接口")
@RestController
@RequestMapping("/api/v1/system/config")
@Validated
public class SystemConfigController {

    private static final Logger logger = LoggerFactory.getLogger(SystemConfigController.class);

    @Autowired
    private SystemConfigApplicationService systemConfigApplicationService;

    @Operation(summary = "创建系统配置", description = "创建新的系统配置项")
    @PostMapping
    public Result<SystemConfigDto> createConfig(@RequestBody @Validated SystemConfigCreateRequest request) {
        logger.info("接收创建系统配置请求，配置键: {}", request.getConfigKey());
        return systemConfigApplicationService.createConfig(request);
    }

    @Operation(summary = "更新系统配置", description = "更新指定的系统配置项")
    @PutMapping
    public Result<SystemConfigDto> updateConfig(@RequestBody @Validated SystemConfigUpdateRequest request) {
        logger.info("接收更新系统配置请求，ID: {}", request.getId());
        return systemConfigApplicationService.updateConfig(request);
    }

    @Operation(summary = "删除系统配置", description = "根据ID删除系统配置项")
    @DeleteMapping("/{id}")
    public Result<Void> deleteConfig(
            @Parameter(description = "配置ID", required = true) @PathVariable Long id) {
        logger.info("接收删除系统配置请求，ID: {}", id);
        return systemConfigApplicationService.deleteConfig(id);
    }

    @Operation(summary = "根据ID查询系统配置", description = "根据配置ID查询系统配置详情")
    @GetMapping("/{id}")
    public Result<SystemConfigDto> getConfigById(
            @Parameter(description = "配置ID", required = true) @PathVariable Long id) {
        logger.info("接收查询系统配置请求，ID: {}", id);
        return systemConfigApplicationService.getConfigById(id);
    }

    @Operation(summary = "根据配置键查询系统配置", description = "根据配置键查询系统配置详情")
    @GetMapping("/key/{configKey}")
    public Result<SystemConfigDto> getConfigByKey(
            @Parameter(description = "配置键", required = true) @PathVariable String configKey) {
        logger.info("接收根据配置键查询系统配置请求，配置键: {}", configKey);
        return systemConfigApplicationService.getConfigByKey(configKey);
    }

    @Operation(summary = "分页查询系统配置", description = "根据条件分页查询系统配置列表")
    @GetMapping("/page")
    public Result<Map<String, Object>> queryConfigs(
            @Parameter(description = "配置键（模糊查询）") @RequestParam(required = false) String configKey,
            @Parameter(description = "配置分组") @RequestParam(required = false) String configGroup,
            @Parameter(description = "是否启用") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size) {
        
        logger.info("接收分页查询系统配置请求，页码: {}, 每页大小: {}", page, size);
        
        SystemConfigQueryRequest request = new SystemConfigQueryRequest();
        request.setConfigKey(configKey);
        request.setConfigGroup(configGroup);
        request.setEnabled(enabled);
        request.setPage(page);
        request.setSize(size);
        
        return systemConfigApplicationService.queryConfigs(request);
    }

    @Operation(summary = "根据分组查询配置列表", description = "根据配置分组查询配置列表")
    @GetMapping("/group/{configGroup}")
    public Result<List<SystemConfigDto>> getConfigsByGroup(
            @Parameter(description = "配置分组", required = true) @PathVariable String configGroup) {
        logger.info("接收根据分组查询配置列表请求，分组: {}", configGroup);
        return systemConfigApplicationService.getConfigsByGroup(configGroup);
    }

    @Operation(summary = "查询所有启用的配置", description = "查询所有启用状态的系统配置")
    @GetMapping("/enabled")
    public Result<List<SystemConfigDto>> getAllEnabledConfigs() {
        logger.info("接收查询所有启用配置请求");
        return systemConfigApplicationService.getAllEnabledConfigs();
    }

    @Operation(summary = "批量更新配置状态", description = "批量更新多个配置的启用状态")
    @PutMapping("/batch/status")
    public Result<Void> batchUpdateStatus(
            @Parameter(description = "配置ID列表", required = true) @RequestParam List<Long> ids,
            @Parameter(description = "是否启用", required = true) @RequestParam Boolean enabled) {
        logger.info("接收批量更新配置状态请求，配置数量: {}, 状态: {}", ids.size(), enabled);
        return systemConfigApplicationService.batchUpdateStatus(ids, enabled);
    }
}

package com.nexusvoice.interfaces.api.admin;

import com.nexusvoice.annotation.RequireAdmin;
import com.nexusvoice.application.ai.dto.log.AdminAiApiCallLogStatsDTO;
import com.nexusvoice.application.ai.dto.provider.AiProviderDTO;
import com.nexusvoice.application.ai.service.AiProviderApplicationService;
import com.nexusvoice.application.user.dto.PageResult;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.model.AiProvider;
import com.nexusvoice.domain.ai.repository.AiApiCallLogRepository;
import com.nexusvoice.domain.ai.repository.AiApiKeyRepository;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.domain.ai.repository.AiProviderRepository;
import com.nexusvoice.interfaces.api.admin.dto.AdminAiApiKeySaveRequest;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiEmbeddingBeanManager;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiRerankBeanManager;
import com.nexusvoice.infrastructure.ai.pool.ApiKeyPoolManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员-AI模型管理控制器
 * 用于管理AI模型配置和API密钥
 *
 * @author NexusVoice
 * @since 2025-10-27
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ai-models")
@RequireAdmin
@Tag(name = "管理员-AI模型管理", description = "AI模型和API密钥管理接口")
public class AdminAiModelController {
    
    @Autowired
    private AiModelRepository modelRepository;
    
    @Autowired
    private AiApiKeyRepository apiKeyRepository;

    @Autowired
    private AiApiCallLogRepository aiApiCallLogRepository;

    @Autowired
    private AiProviderRepository providerRepository;

    @Autowired
    private AiProviderApplicationService aiProviderApplicationService;
    
    @Autowired
    private DynamicAiModelBeanManager modelBeanManager;
    
    @Autowired
    private ApiKeyPoolManager apiKeyPoolManager;
    
    @Autowired(required = false)
    private DynamicAiEmbeddingBeanManager embeddingBeanManager;
    
    @Autowired(required = false)
    private DynamicAiRerankBeanManager rerankBeanManager;
    
    // ==================== 模型管理 ====================
    
    /**
     * 获取所有模型配置
     */
    @GetMapping("/models/all")
    @Operation(summary = "获取全部模型配置", description = "查询所有模型配置，包含禁用模型")
    public Result<List<AiModel>> getAllModelConfigs() {
        List<AiModel> models = modelRepository.findAll();
        log.info("管理员查询到{}个模型配置", models.size());
        return Result.success(models);
    }

    /**
     * 获取所有模型配置
     */
    @GetMapping("/models")
    @Operation(summary = "获取所有模型配置", description = "查询所有已启用的模型配置")
    public Result<List<AiModel>> getAllModels() {
        List<AiModel> models = modelRepository.findAllEnabled();
        log.info("管理员查询到{}个启用的模型", models.size());
        return Result.success(models);
    }

    /**
     * 获取所有服务商选项
     */
    @GetMapping("/providers")
    @Operation(summary = "获取服务商选项", description = "查询所有未删除的AI服务商，用于后台表单联动")
    public Result<List<AiProviderDTO>> getAllProviders() {
        List<AiProviderDTO> providers = aiProviderApplicationService.getAllProviders();
        log.info("管理员查询到{}个服务商选项", providers.size());
        return Result.success(providers);
    }
    
    /**
     * 获取可用的模型列表
     */
    @GetMapping("/available")
    @Operation(summary = "获取可用的模型列表", description = "获取当前运行时可用的模型列表")
    public Result<List<String>> getAvailableModels() {
        List<String> modelKeys = modelBeanManager.getAvailableModelKeys();
        log.info("当前可用模型：{}", modelKeys);
        return Result.success(modelKeys);
    }
    
    /**
     * 添加新模型配置
     */
    @PostMapping("/models")
    @Operation(summary = "添加新模型配置", description = "添加新的AI模型配置")
    public Result<AiModel> addModel(@RequestBody AiModel model) {
        Optional<AiProvider> providerOpt = resolveProvider(model.getProviderId(), model.getProviderCode());
        if (providerOpt.isEmpty()) {
            return Result.error("服务商不存在");
        }
        syncModelProvider(model, providerOpt.get());

        // 检查是否已存在
        if (modelRepository.exists(model.getProviderCode(), model.getModelCode())) {
            return Result.error("模型已存在");
        }
        
        // 设置默认值
        if (model.getStatus() == null) {
            model.setStatus(1);
        }
        if (model.getPriority() == null) {
            model.setPriority(100);
        }
        if (model.getDefaultTemperature() == null) {
            model.setDefaultTemperature(new BigDecimal("0.7"));
        }
        if (model.getDefaultMaxTokens() == null) {
            model.setDefaultMaxTokens(2000);
        }
        if (model.getDefaultTimeoutSeconds() == null) {
            model.setDefaultTimeoutSeconds(60);
        }
        
        model = modelRepository.save(model);
        
        // 刷新服务
        modelBeanManager.refreshModelService(model.getProviderCode(), model.getModelCode());
        
        log.info("管理员添加新模型配置：{}:{}", model.getProviderCode(), model.getModelCode());
        
        return Result.success(model);
    }

    /**
     * 获取模型详情
     */
    @GetMapping("/models/{id}")
    @Operation(summary = "获取模型详情", description = "根据ID查询模型配置详情")
    public Result<AiModel> getModelById(
            @Parameter(description = "模型ID", required = true) @PathVariable Long id) {
        return modelRepository.findById(id)
                .map(Result::success)
                .orElseGet(() -> Result.error("模型不存在"));
    }

    /**
     * 更新模型配置
     */
    @PutMapping("/models/{id}")
    @Operation(summary = "更新模型配置", description = "更新指定的AI模型配置")
    public Result<AiModel> updateModel(
            @Parameter(description = "模型ID", required = true) @PathVariable Long id,
            @RequestBody AiModel request) {
        return modelRepository.findById(id)
                .map(existingModel -> {
                    Long nextProviderId = request.getProviderId() != null
                            ? request.getProviderId()
                            : existingModel.getProviderId();
                    String nextProviderCode = hasText(request.getProviderCode())
                            ? request.getProviderCode().trim()
                            : existingModel.getProviderCode();
                    Optional<AiProvider> providerOpt = resolveProvider(nextProviderId, nextProviderCode);
                    if (providerOpt.isEmpty()) {
                        return Result.<AiModel>error("服务商不存在");
                    }

                    syncModelProvider(request, providerOpt.get());
                    Optional<AiModel> duplicatedModel =
                            modelRepository.findByProviderAndModel(request.getProviderCode(), request.getModelCode());
                    if (duplicatedModel.isPresent() && !duplicatedModel.get().getId().equals(id)) {
                        return Result.<AiModel>error("模型已存在");
                    }

                    request.setId(id);
                    request.setCreatedAt(existingModel.getCreatedAt());
                    request.setUpdatedAt(existingModel.getUpdatedAt());
                    request.setDeleted(existingModel.getDeleted());

                    if (request.getStatus() == null) {
                        request.setStatus(existingModel.getStatus() == null ? 1 : existingModel.getStatus());
                    }
                    if (request.getPriority() == null) {
                        request.setPriority(existingModel.getPriority() == null ? 100 : existingModel.getPriority());
                    }
                    if (request.getDefaultTemperature() == null) {
                        request.setDefaultTemperature(existingModel.getDefaultTemperature() == null
                                ? new BigDecimal("0.7")
                                : existingModel.getDefaultTemperature());
                    }
                    if (request.getDefaultMaxTokens() == null) {
                        request.setDefaultMaxTokens(existingModel.getDefaultMaxTokens() == null
                                ? 2000
                                : existingModel.getDefaultMaxTokens());
                    }
                    if (request.getDefaultTimeoutSeconds() == null) {
                        request.setDefaultTimeoutSeconds(existingModel.getDefaultTimeoutSeconds() == null
                                ? 60
                                : existingModel.getDefaultTimeoutSeconds());
                    }
                    if (request.getContextWindow() == null) {
                        request.setContextWindow(existingModel.getContextWindow());
                    }
                    if (request.getIsOfficial() == null) {
                        request.setIsOfficial(existingModel.getIsOfficial());
                    }
                    if (request.getProviderId() == null) {
                        request.setProviderId(existingModel.getProviderId());
                    }
                    if (request.getUserId() == null) {
                        request.setUserId(existingModel.getUserId());
                    }

                    AiModel updatedModel = modelRepository.save(request);

                    String oldModelKey = existingModel.getModelKey();
                    String newModelKey = updatedModel.getModelKey();
                    if (!oldModelKey.equals(newModelKey)) {
                        modelBeanManager.refreshModelService(existingModel.getProviderCode(), existingModel.getModelCode());
                    }
                    modelBeanManager.refreshModelService(updatedModel.getProviderCode(), updatedModel.getModelCode());

                    log.info("管理员更新模型配置：{} -> {}", oldModelKey, newModelKey);
                    return Result.success(updatedModel);
                })
                .orElseGet(() -> Result.error("模型不存在"));
    }
    
    /**
     * 更新模型状态
     */
    @PutMapping("/models/{id}/status")
    @Operation(summary = "更新模型状态", description = "启用或禁用指定的模型")
    public Result<String> updateModelStatus(
            @Parameter(description = "模型ID", required = true) @PathVariable Long id,
            @Parameter(description = "状态（1启用，0禁用）", required = true) @RequestParam Integer status) {
        modelRepository.updateStatus(id, status);
        
        // 获取模型信息并刷新
        modelRepository.findById(id).ifPresent(model -> {
            modelBeanManager.refreshModelService(model.getProviderCode(), model.getModelCode());
        });
        
        log.info("管理员更新模型状态，ID：{}，状态：{}", id, status);
        return Result.success("更新成功");
    }

    /**
     * 删除模型配置
     */
    @DeleteMapping("/models/{id}")
    @Operation(summary = "删除模型配置", description = "逻辑删除指定AI模型并刷新运行时服务")
    public Result<String> deleteModel(
            @Parameter(description = "模型ID", required = true) @PathVariable Long id) {
        return modelRepository.findById(id)
                .<Result<String>>map(model -> {
                    modelRepository.delete(id);
                    modelBeanManager.refreshModelService(model.getProviderCode(), model.getModelCode());
                    log.info("管理员删除模型配置：{}:{}", model.getProviderCode(), model.getModelCode());
                    return Result.success("删除成功");
                })
                .orElseGet(() -> Result.error("模型不存在"));
    }
    
    /**
     * 刷新模型服务
     */
    @PostMapping("/models/refresh/{providerCode}/{modelCode}")
    @Operation(summary = "刷新模型服务", description = "重新加载指定模型的配置和服务")
    public Result<String> refreshModelService(
            @Parameter(description = "提供商代码", required = true) @PathVariable String providerCode,
            @Parameter(description = "模型代码", required = true) @PathVariable String modelCode) {
        modelBeanManager.refreshModelService(providerCode, modelCode);
        log.info("管理员刷新模型服务：{}:{}", providerCode, modelCode);
        return Result.success("刷新成功");
    }
    
    /**
     * 获取向量模型列表
     */
    @GetMapping("/embedding-models")
    @Operation(summary = "获取向量模型列表", description = "查询所有可用的向量模型")
    public Result<List<AiModel>> getEmbeddingModels() {
        if (embeddingBeanManager == null) {
            return Result.error("向量模型管理器未初始化");
        }
        List<AiModel> models = embeddingBeanManager.getAvailableModels();
        log.info("管理员查询到{}个可用的向量模型", models.size());
        return Result.success(models);
    }
    
    /**
     * 获取重排序模型列表
     */
    @GetMapping("/rerank-models")
    @Operation(summary = "获取重排序模型列表", description = "查询所有可用的重排序模型")
    public Result<List<AiModel>> getRerankModels() {
        if (rerankBeanManager == null) {
            return Result.error("重排序模型管理器未初始化");
        }
        List<AiModel> models = rerankBeanManager.getAvailableModels();
        log.info("管理员查询到{}个可用的重排序模型", models.size());
        return Result.success(models);
    }
    
    // ==================== API密钥管理 ====================
    
    /**
     * 分页查询 API 密钥
     */
    @GetMapping("/api-keys/all")
    @Operation(summary = "分页查询API密钥", description = "分页查询全部AI API密钥")
    public Result<PageResult<AiApiKey>> pageApiKeys(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "提供商代码") @RequestParam(required = false) String providerCode,
            @Parameter(description = "模型代码") @RequestParam(required = false) String modelCode,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        PageResult<AiApiKey> result = apiKeyRepository.pageAll(page, size, keyword, providerCode, modelCode, status);
        return Result.success(result);
    }

    /**
     * 添加API密钥
     */
    @PostMapping("/api-keys")
    @Operation(summary = "添加API密钥", description = "为指定模型添加API密钥")
    public Result<AiApiKey> addApiKey(@RequestBody AdminAiApiKeySaveRequest request) {
        Optional<AiProvider> providerOpt = resolveProvider(request.getProviderId(), request.getProviderCode());
        if (providerOpt.isEmpty()) {
            return Result.error("服务商不存在");
        }

        AiProvider provider = providerOpt.get();
        String providerCode = provider.getProviderCode();
        String modelCode = request.getModelCode();

        if (providerCode == null || providerCode.isBlank() || modelCode == null || modelCode.isBlank()) {
            return Result.error("提供商代码和模型代码不能为空");
        }
        if (request.getApiKey() == null || request.getApiKey().isBlank()) {
            return Result.error("API密钥不能为空");
        }

        // 检查模型是否存在
        if (!modelRepository.exists(providerCode, modelCode)) {
            return Result.error("模型不存在：" + providerCode + ":" + modelCode);
        }
        
        AiApiKey apiKeyEntity = new AiApiKey();
        apiKeyEntity.setProviderId(provider.getId());
        apiKeyEntity.setProviderCode(providerCode);
        apiKeyEntity.setModelCode(modelCode);
        apiKeyEntity.setApiKey(request.getApiKey());
        apiKeyEntity.setApiSecret(request.getApiSecret());
        apiKeyEntity.setBaseUrl(request.getBaseUrl());
        apiKeyEntity.setProxyUrl(request.getProxyUrl());
        apiKeyEntity.setWeight(request.getWeight() == null ? 1 : request.getWeight());
        apiKeyEntity.setRateLimit(request.getRateLimit());
        apiKeyEntity.setConcurrentLimit(request.getConcurrentLimit());
        apiKeyEntity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        apiKeyEntity.setDailyQuotaLimit(request.getDailyQuotaLimit());
        apiKeyEntity.setMonthlyQuotaLimit(request.getMonthlyQuotaLimit());
        
        apiKeyEntity = apiKeyRepository.save(apiKeyEntity);
        
        // 刷新密钥池
        apiKeyPoolManager.refreshModelPool(providerCode, modelCode);
        
        log.info("管理员添加API密钥成功，模型：{}:{}，密钥ID：{}", 
                providerCode, modelCode, apiKeyEntity.getId());
        
        return Result.success(apiKeyEntity);
    }
    
    /**
     * 获取指定模型的API密钥列表
     */
    @GetMapping("/api-keys/{providerCode}/{modelCode}")
    @Operation(summary = "获取指定模型的API密钥列表", description = "查询指定模型的所有API密钥")
    public Result<List<AiApiKey>> getApiKeysByModel(
            @Parameter(description = "提供商代码", required = true) @PathVariable String providerCode,
            @Parameter(description = "模型代码", required = true) @PathVariable String modelCode) {
        List<AiApiKey> keys = apiKeyRepository.findAllByModel(providerCode, modelCode);
        log.info("管理员查询模型{}:{}的API密钥，数量：{}", providerCode, modelCode, keys.size());
        return Result.success(keys);
    }
    
    /**
     * 获取API密钥详情
     */
    @GetMapping("/api-keys/detail/{id}")
    @Operation(summary = "获取API密钥详情", description = "根据ID查询API密钥详细信息")
    public Result<AiApiKey> getApiKeyById(
            @Parameter(description = "密钥ID", required = true) @PathVariable Long id) {
        return apiKeyRepository.findById(id)
                .map(key -> {
                    log.info("管理员查询API密钥详情，ID：{}", id);
                    return Result.success(key);
                })
                .orElseGet(() -> Result.error("API密钥不存在"));
    }
    
    /**
     * 更新API密钥
     */
    @PutMapping("/api-keys/{id}")
    @Operation(summary = "更新API密钥", description = "更新API密钥的配置信息")
    public Result<AiApiKey> updateApiKey(
            @Parameter(description = "密钥ID", required = true) @PathVariable Long id,
            @RequestBody AdminAiApiKeySaveRequest request) {
        
        return apiKeyRepository.findById(id)
                .map(apiKey -> {
                    boolean updated = false;
                    String oldProviderCode = apiKey.getProviderCode();
                    String oldModelCode = apiKey.getModelCode();

                    Long nextProviderId = request.getProviderId() != null
                            ? request.getProviderId()
                            : apiKey.getProviderId();
                    String nextProviderCode = hasText(request.getProviderCode())
                            ? request.getProviderCode().trim()
                            : apiKey.getProviderCode();
                    String nextModelCode = hasText(request.getModelCode())
                            ? request.getModelCode().trim()
                            : apiKey.getModelCode();

                    Optional<AiProvider> providerOpt = resolveProvider(nextProviderId, nextProviderCode);
                    if (providerOpt.isEmpty()) {
                        return Result.<AiApiKey>error("服务商不存在");
                    }

                    AiProvider provider = providerOpt.get();

                    if (!modelRepository.exists(provider.getProviderCode(), nextModelCode)) {
                        return Result.<AiApiKey>error("模型不存在：" + provider.getProviderCode() + ":" + nextModelCode);
                    }

                    if (!provider.getId().equals(apiKey.getProviderId())
                            || !provider.getProviderCode().equals(apiKey.getProviderCode())) {
                        apiKey.setProviderId(provider.getId());
                        apiKey.setProviderCode(provider.getProviderCode());
                        updated = true;
                    }
                    if (!nextModelCode.equals(apiKey.getModelCode())) {
                        apiKey.setModelCode(nextModelCode);
                        updated = true;
                    }
                    if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
                        apiKey.setApiKey(request.getApiKey());
                        updated = true;
                    }
                    if (request.getApiSecret() != null) {
                        apiKey.setApiSecret(request.getApiSecret());
                        updated = true;
                    }
                    if (request.getBaseUrl() != null) {
                        apiKey.setBaseUrl(request.getBaseUrl());
                        updated = true;
                    }
                    if (request.getProxyUrl() != null) {
                        apiKey.setProxyUrl(request.getProxyUrl());
                        updated = true;
                    }
                    if (request.getWeight() != null) {
                        apiKey.setWeight(request.getWeight());
                        updated = true;
                    }
                    if (request.getRateLimit() != null) {
                        apiKey.setRateLimit(request.getRateLimit());
                        updated = true;
                    }
                    if (request.getConcurrentLimit() != null) {
                        apiKey.setConcurrentLimit(request.getConcurrentLimit());
                        updated = true;
                    }
                    if (request.getStatus() != null) {
                        apiKey.setStatus(request.getStatus());
                        updated = true;
                    }
                    if (request.getDailyQuotaLimit() != null) {
                        apiKey.setDailyQuotaLimit(request.getDailyQuotaLimit());
                        updated = true;
                    }
                    if (request.getMonthlyQuotaLimit() != null) {
                        apiKey.setMonthlyQuotaLimit(request.getMonthlyQuotaLimit());
                        updated = true;
                    }
                    
                    if (updated) {
                        // 保存更新
                        apiKeyRepository.save(apiKey);

                        if (!oldProviderCode.equals(apiKey.getProviderCode()) || !oldModelCode.equals(apiKey.getModelCode())) {
                            apiKeyPoolManager.refreshModelPool(oldProviderCode, oldModelCode);
                        }
                        apiKeyPoolManager.refreshModelPool(apiKey.getProviderCode(), apiKey.getModelCode());
                        
                        log.info("管理员更新API密钥成功，ID：{}", id);
                    }
                    
                    return Result.success(apiKey);
                })
                .orElseGet(() -> Result.error("API密钥不存在"));
    }

    /**
     * 分页查询调用日志
     */
    @GetMapping("/api-call-logs")
    @Operation(summary = "分页查询API调用日志", description = "管理端分页查询 AI API 调用日志")
    public Result<PageResult<AiApiCallLog>> pageApiCallLogs(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "提供商代码") @RequestParam(required = false) String providerCode,
            @Parameter(description = "模型代码") @RequestParam(required = false) String modelCode,
            @Parameter(description = "API密钥ID") @RequestParam(required = false) Long apiKeyId,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "状态：0失败 1成功") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键字（请求ID/提供商/模型/错误信息）") @RequestParam(required = false) String keyword,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        PageResult<AiApiCallLog> result = aiApiCallLogRepository.pageAdminLogs(
                page, size, providerCode, modelCode, apiKeyId, userId, status, startTime, endTime, keyword);
        return Result.success(result);
    }

    /**
     * 查询调用日志统计概览
     */
    @GetMapping("/api-call-logs/stats")
    @Operation(summary = "查询API调用日志统计", description = "管理端查询 AI API 调用日志统计概览和热门模型")
    public Result<AdminAiApiCallLogStatsDTO> getApiCallLogStats(
            @Parameter(description = "提供商代码") @RequestParam(required = false) String providerCode,
            @Parameter(description = "模型代码") @RequestParam(required = false) String modelCode,
            @Parameter(description = "API密钥ID") @RequestParam(required = false) Long apiKeyId,
            @Parameter(description = "用户ID") @RequestParam(required = false) Long userId,
            @Parameter(description = "状态：0失败 1成功") @RequestParam(required = false) Integer status,
            @Parameter(description = "关键字（请求ID/提供商/模型/错误信息）") @RequestParam(required = false) String keyword,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        AdminAiApiCallLogStatsDTO result = aiApiCallLogRepository.summarizeAdminLogs(
                providerCode, modelCode, apiKeyId, userId, status, startTime, endTime, keyword);
        return Result.success(result);
    }
    
    /**
     * 更新API密钥状态
     */
    @PutMapping("/api-keys/{id}/status")
    @Operation(summary = "更新API密钥状态", description = "启用或禁用API密钥")
    public Result<String> updateApiKeyStatus(
            @Parameter(description = "密钥ID", required = true) @PathVariable Long id,
            @Parameter(description = "状态（1启用，0禁用）", required = true) @RequestParam Integer status) {
        
        return apiKeyRepository.findById(id)
                .<Result<String>>map(apiKey -> {
                    apiKeyRepository.updateStatus(id, status);
                    
                    // 刷新密钥池
                    apiKeyPoolManager.refreshModelPool(apiKey.getProviderCode(), apiKey.getModelCode());
                    
                    log.info("管理员更新API密钥状态，ID：{}，状态：{}", id, status);
                    return Result.success("状态更新成功");
                })
                .orElseGet(() -> Result.error("API密钥不存在"));
    }
    
    /**
     * 删除API密钥
     */
    @DeleteMapping("/api-keys/{id}")
    @Operation(summary = "删除API密钥", description = "删除指定的API密钥（逻辑删除）")
    public Result<String> deleteApiKey(
            @Parameter(description = "密钥ID", required = true) @PathVariable Long id) {
        
        return apiKeyRepository.findById(id)
                .<Result<String>>map(apiKey -> {
                    String providerCode = apiKey.getProviderCode();
                    String modelCode = apiKey.getModelCode();
                    
                    apiKeyRepository.delete(id);
                    
                    // 刷新密钥池
                    apiKeyPoolManager.refreshModelPool(providerCode, modelCode);
                    
                    log.info("管理员删除API密钥，ID：{}，模型：{}:{}", id, providerCode, modelCode);
                    return Result.success("删除成功");
                })
                .orElseGet(() -> Result.error("API密钥不存在"));
    }
    
    // ==================== 监控与状态 ====================
    
    /**
     * 获取密钥池状态
     */
    @GetMapping("/pool-status")
    @Operation(summary = "获取密钥池状态", description = "查询所有模型的密钥池状态")
    public Result<Map<String, Object>> getPoolStatus() {
        Map<String, Object> status = new HashMap<>();
        
        List<String> modelKeys = modelBeanManager.getAvailableModelKeys();
        for (String modelKey : modelKeys) {
            String[] parts = modelKey.split(":", 2);
            if (parts.length == 2) {
                int availableCount = apiKeyPoolManager.getAvailableKeyCount(parts[0], parts[1]);
                status.put(modelKey, Map.of(
                    "availableKeys", availableCount,
                    "status", availableCount > 0 ? "正常" : "无可用密钥"
                ));
            }
        }
        
        log.info("管理员查询密钥池状态，模型数：{}", status.size());
        return Result.success(status);
    }

    private Optional<AiProvider> resolveProvider(Long providerId, String providerCode) {
        if (providerId != null) {
            return providerRepository.findById(providerId);
        }
        if (hasText(providerCode)) {
            return providerRepository.findByCode(providerCode.trim());
        }
        return Optional.empty();
    }

    private void syncModelProvider(AiModel model, AiProvider provider) {
        model.setProviderId(provider.getId());
        model.setProviderCode(provider.getProviderCode());
        if (model.getIsOfficial() == null) {
            model.setIsOfficial(provider.getIsOfficial());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

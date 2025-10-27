package com.nexusvoice.interfaces.api.admin;

import com.nexusvoice.annotation.RequireAdmin;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.repository.AiApiKeyRepository;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiEmbeddingBeanManager;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiRerankBeanManager;
import com.nexusvoice.infrastructure.ai.pool.ApiKeyPoolManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @GetMapping("/models")
    @Operation(summary = "获取所有模型配置", description = "查询所有已启用的模型配置")
    public Result<List<AiModel>> getAllModels() {
        List<AiModel> models = modelRepository.findAllEnabled();
        log.info("管理员查询到{}个启用的模型", models.size());
        return Result.success(models);
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
     * 添加API密钥
     */
    @PostMapping("/api-keys")
    @Operation(summary = "添加API密钥", description = "为指定模型添加API密钥")
    public Result<AiApiKey> addApiKey(
            @Parameter(description = "提供商代码", required = true) @RequestParam String providerCode,
            @Parameter(description = "模型代码", required = true) @RequestParam String modelCode,
            @Parameter(description = "API密钥", required = true) @RequestParam String apiKey,
            @Parameter(description = "Base URL") @RequestParam(required = false) String baseUrl) {
        // 检查模型是否存在
        if (!modelRepository.exists(providerCode, modelCode)) {
            return Result.error("模型不存在：" + providerCode + ":" + modelCode);
        }
        
        AiApiKey apiKeyEntity = new AiApiKey();
        apiKeyEntity.setProviderCode(providerCode);
        apiKeyEntity.setModelCode(modelCode);
        apiKeyEntity.setApiKey(apiKey);
        apiKeyEntity.setBaseUrl(baseUrl);
        apiKeyEntity.setStatus(1);
        apiKeyEntity.setWeight(1);
        
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
            @Parameter(description = "Base URL") @RequestParam(required = false) String baseUrl,
            @Parameter(description = "权重") @RequestParam(required = false) Integer weight,
            @Parameter(description = "日限额（tokens）") @RequestParam(required = false) Long dailyQuotaLimit,
            @Parameter(description = "月限额（tokens）") @RequestParam(required = false) Long monthlyQuotaLimit) {
        
        return apiKeyRepository.findById(id)
                .map(apiKey -> {
                    boolean updated = false;
                    
                    if (baseUrl != null) {
                        apiKey.setBaseUrl(baseUrl);
                        updated = true;
                    }
                    if (weight != null) {
                        apiKey.setWeight(weight);
                        updated = true;
                    }
                    if (dailyQuotaLimit != null) {
                        apiKey.setDailyQuotaLimit(dailyQuotaLimit);
                        updated = true;
                    }
                    if (monthlyQuotaLimit != null) {
                        apiKey.setMonthlyQuotaLimit(monthlyQuotaLimit);
                        updated = true;
                    }
                    
                    if (updated) {
                        // 保存更新
                        apiKeyRepository.save(apiKey);
                        
                        // 刷新密钥池
                        apiKeyPoolManager.refreshModelPool(apiKey.getProviderCode(), apiKey.getModelCode());
                        
                        log.info("管理员更新API密钥成功，ID：{}", id);
                    }
                    
                    return Result.success(apiKey);
                })
                .orElseGet(() -> Result.error("API密钥不存在"));
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
}

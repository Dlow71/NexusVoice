package com.nexusvoice.interfaces.api.developer;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.developer.dto.*;
import com.nexusvoice.application.developer.service.DeveloperApiKeyApplicationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 开发者API密钥控制器
 * 提供API密钥的CRUD操作
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/developer/api-keys")
@Tag(name = "开发者API密钥", description = "开发者API密钥管理相关接口")
@RequireAuth
@Validated
public class DeveloperApiKeyController {
    
    @Autowired
    private DeveloperApiKeyApplicationService apiKeyApplicationService;
    
    /**
     * 创建API密钥
     */
    @Operation(summary = "创建API密钥", 
               description = "创建新的开发者API密钥。密钥将仅在创建时完整返回一次，请妥善保存。")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "创建失败")
    })
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<CreateApiKeyResponseDTO> createApiKey(
            @Parameter(description = "创建API密钥请求", required = true)
            @Valid @RequestBody CreateApiKeyRequestDTO requestDTO) {
        
        // 获取当前用户ID
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> {
                    log.warn("创建API密钥失败：用户未登录");
                    return BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录");
                });
        
        log.info("用户 {} 开始创建API密钥，名称: {}", userId, requestDTO.getKeyName());
        
        long startTime = System.currentTimeMillis();
        try {
            CreateApiKeyResponseDTO responseDTO = apiKeyApplicationService.createApiKey(requestDTO, userId);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("用户 {} 创建API密钥成功，密钥ID: {}，耗时: {}ms", 
                    userId, responseDTO.getId(), processingTime);
            
            return Result.success("创建API密钥成功", responseDTO);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("用户 {} 创建API密钥失败，耗时: {}ms", userId, processingTime, e);
            throw e;
        }
    }
    
    /**
     * 更新API密钥
     */
    @Operation(summary = "更新API密钥", 
               description = "更新API密钥的配置信息（不包括密钥值本身）")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "404", description = "API密钥不存在"),
        @ApiResponse(responseCode = "500", description = "更新失败")
    })
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ApiKeyResponseDTO> updateApiKey(
            @Parameter(description = "更新API密钥请求", required = true)
            @Valid @RequestBody UpdateApiKeyRequestDTO requestDTO) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录"));
        
        log.info("用户 {} 开始更新API密钥，密钥ID: {}", userId, requestDTO.getId());
        
        long startTime = System.currentTimeMillis();
        try {
            ApiKeyResponseDTO responseDTO = apiKeyApplicationService.updateApiKey(requestDTO, userId);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("用户 {} 更新API密钥成功，密钥ID: {}，耗时: {}ms", 
                    userId, requestDTO.getId(), processingTime);
            
            return Result.success("更新API密钥成功", responseDTO);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("用户 {} 更新API密钥失败，密钥ID: {}，耗时: {}ms", 
                    userId, requestDTO.getId(), processingTime, e);
            throw e;
        }
    }
    
    /**
     * 删除API密钥
     */
    @Operation(summary = "删除API密钥", 
               description = "删除指定的API密钥（逻辑删除）")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "404", description = "API密钥不存在"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> deleteApiKey(
            @Parameter(description = "API密钥ID", required = true)
            @PathVariable("id") Long id) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录"));
        
        log.info("用户 {} 开始删除API密钥，密钥ID: {}", userId, id);
        
        long startTime = System.currentTimeMillis();
        try {
            apiKeyApplicationService.deleteApiKey(id, userId);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("用户 {} 删除API密钥成功，密钥ID: {}，耗时: {}ms", 
                    userId, id, processingTime);
            
            return Result.success("删除API密钥成功");
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("用户 {} 删除API密钥失败，密钥ID: {}，耗时: {}ms", 
                    userId, id, processingTime, e);
            throw e;
        }
    }
    
    /**
     * 批量删除API密钥
     */
    @Operation(summary = "批量删除API密钥", 
               description = "批量删除多个API密钥（逻辑删除）")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "批量删除成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "批量删除失败")
    })
    @DeleteMapping(value = "/batch", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> batchDeleteApiKeys(
            @Parameter(description = "API密钥ID列表", required = true)
            @RequestBody List<Long> ids) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录"));
        
        log.info("用户 {} 开始批量删除API密钥，数量: {}", userId, ids.size());
        
        long startTime = System.currentTimeMillis();
        try {
            apiKeyApplicationService.batchDeleteApiKeys(ids, userId);
            
            long processingTime = System.currentTimeMillis() - startTime;
            log.info("用户 {} 批量删除API密钥成功，数量: {}，耗时: {}ms", 
                    userId, ids.size(), processingTime);
            
            return Result.success("批量删除API密钥成功");
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("用户 {} 批量删除API密钥失败，数量: {}，耗时: {}ms", 
                    userId, ids.size(), processingTime, e);
            throw e;
        }
    }
    
    /**
     * 启用API密钥
     */
    @Operation(summary = "启用API密钥", 
               description = "启用已禁用的API密钥")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "启用成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "404", description = "API密钥不存在"),
        @ApiResponse(responseCode = "500", description = "启用失败")
    })
    @PostMapping(value = "/{id}/enable", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> enableApiKey(
            @Parameter(description = "API密钥ID", required = true)
            @PathVariable("id") Long id) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录"));
        
        log.info("用户 {} 开始启用API密钥，密钥ID: {}", userId, id);
        
        try {
            apiKeyApplicationService.enableApiKey(id, userId);
            log.info("用户 {} 启用API密钥成功，密钥ID: {}", userId, id);
            return Result.success("启用API密钥成功");
            
        } catch (Exception e) {
            log.error("用户 {} 启用API密钥失败，密钥ID: {}", userId, id, e);
            throw e;
        }
    }
    
    /**
     * 禁用API密钥
     */
    @Operation(summary = "禁用API密钥", 
               description = "禁用API密钥，禁用后将无法使用")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "禁用成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "404", description = "API密钥不存在"),
        @ApiResponse(responseCode = "500", description = "禁用失败")
    })
    @PostMapping(value = "/{id}/disable", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Void> disableApiKey(
            @Parameter(description = "API密钥ID", required = true)
            @PathVariable("id") Long id) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录"));
        
        log.info("用户 {} 开始禁用API密钥，密钥ID: {}", userId, id);
        
        try {
            apiKeyApplicationService.disableApiKey(id, userId);
            log.info("用户 {} 禁用API密钥成功，密钥ID: {}", userId, id);
            return Result.success("禁用API密钥成功");
            
        } catch (Exception e) {
            log.error("用户 {} 禁用API密钥失败，密钥ID: {}", userId, id, e);
            throw e;
        }
    }
    
    /**
     * 查询API密钥详情
     */
    @Operation(summary = "查询API密钥详情", 
               description = "查询指定API密钥的详细信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "404", description = "API密钥不存在"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ApiKeyResponseDTO> getApiKey(
            @Parameter(description = "API密钥ID", required = true)
            @PathVariable("id") Long id) {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录"));
        
        log.debug("用户 {} 查询API密钥详情，密钥ID: {}", userId, id);
        
        try {
            ApiKeyResponseDTO responseDTO = apiKeyApplicationService.getApiKey(id, userId);
            return Result.success("查询API密钥成功", responseDTO);
            
        } catch (Exception e) {
            log.error("用户 {} 查询API密钥失败，密钥ID: {}", userId, id, e);
            throw e;
        }
    }
    
    /**
     * 查询当前用户的所有API密钥
     */
    @Operation(summary = "查询API密钥列表", 
               description = "查询当前用户的所有API密钥")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<ApiKeyResponseDTO>> listApiKeys() {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录"));
        
        log.debug("用户 {} 查询API密钥列表", userId);
        
        try {
            List<ApiKeyResponseDTO> apiKeys = apiKeyApplicationService.listApiKeys(userId);
            log.info("用户 {} 查询API密钥列表成功，数量: {}", userId, apiKeys.size());
            return Result.success("查询API密钥列表成功", apiKeys);
            
        } catch (Exception e) {
            log.error("用户 {} 查询API密钥列表失败", userId, e);
            throw e;
        }
    }
    
    /**
     * 查询API密钥统计信息
     */
    @Operation(summary = "查询API密钥统计信息", 
               description = "查询当前用户的API密钥使用统计信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping(value = "/statistics", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<ApiKeyStatisticsDTO> getStatistics() {
        
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.UNAUTHORIZED, "请先登录"));
        
        log.debug("用户 {} 查询API密钥统计信息", userId);
        
        try {
            ApiKeyStatisticsDTO statistics = apiKeyApplicationService.getStatistics(userId);
            log.info("用户 {} 查询API密钥统计信息成功", userId);
            return Result.success("查询统计信息成功", statistics);
            
        } catch (Exception e) {
            log.error("用户 {} 查询API密钥统计信息失败", userId, e);
            throw e;
        }
    }
}

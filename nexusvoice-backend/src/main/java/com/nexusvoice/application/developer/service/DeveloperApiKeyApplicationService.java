package com.nexusvoice.application.developer.service;

import com.nexusvoice.application.developer.assembler.DeveloperApiKeyAssembler;
import com.nexusvoice.application.developer.dto.*;
import com.nexusvoice.domain.developer.constant.ApiKeyStatus;
import com.nexusvoice.domain.developer.model.DeveloperApiKey;
import com.nexusvoice.domain.developer.repository.DeveloperApiKeyRepository;
import com.nexusvoice.domain.developer.service.ApiKeyGeneratorService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import com.nexusvoice.domain.developer.port.DailyCostCounterPort;

/**
 * 开发者API密钥应用服务
 * 负责API密钥的CRUD操作和统计查询
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Slf4j
@Service
public class DeveloperApiKeyApplicationService {
    
    @Autowired
    private DeveloperApiKeyRepository apiKeyRepository;
    
    @Autowired
    private ApiKeyGeneratorService apiKeyGenerator;
    
    @Autowired
    private DeveloperApiKeyAssembler assembler;

    @Autowired(required = false)
    private DailyCostCounterPort dailyCostCounter;
    
    /**
     * 用户可创建的最大密钥数量（可配置）
     */
    private static final int MAX_API_KEYS_PER_USER = 20;
    
    
    /**
     * 创建API密钥
     * 
     * @param requestDTO 创建请求
     * @param userId 用户ID
     * @return 创建响应（包含完整密钥）
     */
    @Transactional
    public CreateApiKeyResponseDTO createApiKey(CreateApiKeyRequestDTO requestDTO, Long userId) {
        log.info("用户 {} 开始创建API密钥，名称: {}", userId, requestDTO.getKeyName());
        
        // 1. 检查用户的API密钥数量限制
        long existingCount = apiKeyRepository.countByUserId(userId);
        if (existingCount >= MAX_API_KEYS_PER_USER) {
            log.warn("用户 {} 的API密钥数量已达上限: {}", userId, existingCount);
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_QUOTA_EXCEEDED, 
                    String.format("API密钥数量已达上限（%d个）", MAX_API_KEYS_PER_USER));
        }
        
        // 2. 检查密钥名称唯一性
        if (apiKeyRepository.findByUserIdAndKeyName(userId, requestDTO.getKeyName()).isPresent()) {
            log.warn("用户 {} 的API密钥名称已存在: {}", userId, requestDTO.getKeyName());
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_NAME_EXISTS, 
                    "该密钥名称已存在");
        }
        
        // 3. 生成API密钥
        String apiKey = apiKeyGenerator.generate();
        String keyValueHash = apiKeyGenerator.hash(apiKey);
        String keyPrefix = apiKeyGenerator.generatePrefix(apiKey);
        
        // 4. 创建领域对象
        DeveloperApiKey domain = assembler.toDomain(requestDTO, userId, apiKey, keyValueHash, keyPrefix);
        
        // 5. 保存到数据库
        apiKeyRepository.save(domain);
        
        log.info("用户 {} 创建API密钥成功，ID: {}，名称: {}", userId, domain.getId(), requestDTO.getKeyName());
        
        // 6. 返回响应（包含完整密钥，仅此一次）
        return assembler.toCreateResponseDTO(domain, apiKey);
    }
    
    /**
     * 更新API密钥
     * 
     * @param requestDTO 更新请求
     * @param userId 用户ID（用于权限校验）
     * @return 更新后的密钥信息
     */
    @Transactional
    public ApiKeyResponseDTO updateApiKey(UpdateApiKeyRequestDTO requestDTO, Long userId) {
        log.info("用户 {} 开始更新API密钥，ID: {}", userId, requestDTO.getId());
        
        // 1. 查询现有密钥
        DeveloperApiKey existing = apiKeyRepository.findById(requestDTO.getId())
                .orElseThrow(() -> {
                    log.warn("API密钥不存在，ID: {}", requestDTO.getId());
                    return BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_NOT_FOUND, "API密钥不存在");
                });
        
        // 2. 验证权限（只能更新自己的密钥）
        if (!existing.getUserId().equals(userId)) {
            log.warn("用户 {} 无权更新API密钥，密钥所属用户: {}", userId, existing.getUserId());
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "无权操作此API密钥");
        }
        
        // 3. 检查密钥名称唯一性（如果修改了名称）
        if (requestDTO.getKeyName() != null && !requestDTO.getKeyName().equals(existing.getKeyName())) {
            if (apiKeyRepository.findByUserIdAndKeyName(userId, requestDTO.getKeyName()).isPresent()) {
                log.warn("用户 {} 的API密钥名称已存在: {}", userId, requestDTO.getKeyName());
                throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_NAME_EXISTS, "该密钥名称已存在");
            }
        }
        
        // 4. 更新领域对象
        assembler.updateDomain(requestDTO, existing);
        
        // 5. 保存到数据库
        apiKeyRepository.update(existing);
        
        log.info("用户 {} 更新API密钥成功，ID: {}", userId, requestDTO.getId());
        
        // 6. 返回响应
        return assembler.toResponseDTO(existing);
    }
    
    /**
     * 删除API密钥（逻辑删除）
     * 
     * @param apiKeyId API密钥ID
     * @param userId 用户ID（用于权限校验）
     */
    @Transactional
    public void deleteApiKey(Long apiKeyId, Long userId) {
        log.info("用户 {} 开始删除API密钥，ID: {}", userId, apiKeyId);
        
        // 1. 查询现有密钥
        DeveloperApiKey existing = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> {
                    log.warn("API密钥不存在，ID: {}", apiKeyId);
                    return BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_NOT_FOUND, "API密钥不存在");
                });
        
        // 2. 验证权限
        if (!existing.getUserId().equals(userId)) {
            log.warn("用户 {} 无权删除API密钥，密钥所属用户: {}", userId, existing.getUserId());
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "无权操作此API密钥");
        }
        
        // 3. 逻辑删除
        apiKeyRepository.delete(apiKeyId);
        
        log.info("用户 {} 删除API密钥成功，ID: {}", userId, apiKeyId);
    }
    
    /**
     * 批量删除API密钥
     * 
     * @param apiKeyIds API密钥ID列表
     * @param userId 用户ID（用于权限校验）
     */
    @Transactional
    public void batchDeleteApiKeys(List<Long> apiKeyIds, Long userId) {
        log.info("用户 {} 开始批量删除API密钥，数量: {}", userId, apiKeyIds.size());
        
        if (apiKeyIds == null || apiKeyIds.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "删除的密钥ID列表不能为空");
        }
        
        // 验证所有密钥的权限
        for (Long apiKeyId : apiKeyIds) {
            DeveloperApiKey existing = apiKeyRepository.findById(apiKeyId)
                    .orElseThrow(() -> BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_NOT_FOUND, "API密钥不存在: " + apiKeyId));
            
            if (!existing.getUserId().equals(userId)) {
                throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "无权操作API密钥: " + apiKeyId);
            }
        }
        
        // 批量删除
        apiKeyRepository.batchDelete(apiKeyIds);
        
        log.info("用户 {} 批量删除API密钥成功，数量: {}", userId, apiKeyIds.size());
    }
    
    /**
     * 启用API密钥
     * 
     * @param apiKeyId API密钥ID
     * @param userId 用户ID（用于权限校验）
     */
    @Transactional
    public void enableApiKey(Long apiKeyId, Long userId) {
        log.info("用户 {} 开始启用API密钥，ID: {}", userId, apiKeyId);
        
        DeveloperApiKey existing = getAndValidateOwnership(apiKeyId, userId);
        existing.enable();
        apiKeyRepository.update(existing);
        
        log.info("用户 {} 启用API密钥成功，ID: {}", userId, apiKeyId);
    }
    
    /**
     * 禁用API密钥
     * 
     * @param apiKeyId API密钥ID
     * @param userId 用户ID（用于权限校验）
     */
    @Transactional
    public void disableApiKey(Long apiKeyId, Long userId) {
        log.info("用户 {} 开始禁用API密钥，ID: {}", userId, apiKeyId);
        
        DeveloperApiKey existing = getAndValidateOwnership(apiKeyId, userId);
        existing.disable();
        apiKeyRepository.update(existing);
        
        log.info("用户 {} 禁用API密钥成功，ID: {}", userId, apiKeyId);
    }
    
    /**
     * 查询API密钥详情
     * 
     * @param apiKeyId API密钥ID
     * @param userId 用户ID（用于权限校验）
     * @return API密钥详情
     */
    public ApiKeyResponseDTO getApiKey(Long apiKeyId, Long userId) {
        DeveloperApiKey existing = getAndValidateOwnership(apiKeyId, userId);
        return assembler.toResponseDTO(existing);
    }
    
    /**
     * 查询用户的所有API密钥
     * 
     * @param userId 用户ID
     * @return API密钥列表
     */
    public List<ApiKeyResponseDTO> listApiKeys(Long userId) {
        List<DeveloperApiKey> apiKeys = apiKeyRepository.findByUserId(userId);
        return assembler.toResponseDTOList(apiKeys);
    }
    
    /**
     * 查询用户的API密钥统计信息
     * 
     * @param userId 用户ID
     * @return 统计信息
     */
    public ApiKeyStatisticsDTO getStatistics(Long userId) {
        List<DeveloperApiKey> apiKeys = apiKeyRepository.findByUserId(userId);
        
        ApiKeyStatisticsDTO statistics = new ApiKeyStatisticsDTO();
        statistics.setTotalKeys((long) apiKeys.size());
        
        // 统计各状态密钥数量
        long activeCount = apiKeys.stream().filter(k -> k.getStatus() == ApiKeyStatus.ACTIVE && !k.isExpired()).count();
        long disabledCount = apiKeys.stream().filter(k -> k.getStatus() == ApiKeyStatus.DISABLED).count();
        long expiredCount = apiKeys.stream().filter(DeveloperApiKey::isExpired).count();
        
        statistics.setActiveKeys(activeCount);
        statistics.setDisabledKeys(disabledCount);
        statistics.setExpiredKeys(expiredCount);
        
        // 统计总请求次数和Token使用量
        long totalRequests = apiKeys.stream()
                .mapToLong(k -> k.getTotalRequestCount() != null ? k.getTotalRequestCount() : 0)
                .sum();
        long todayRequests = apiKeys.stream()
                .mapToLong(k -> k.getTodayRequestCount() != null ? k.getTodayRequestCount() : 0)
                .sum();
        long totalTokens = apiKeys.stream()
                .mapToLong(k -> k.getTotalTokens() != null ? k.getTotalTokens() : 0)
                .sum();
        long todayTokens = apiKeys.stream()
                .mapToLong(k -> k.getTodayTokens() != null ? k.getTodayTokens() : 0)
                .sum();
        
        statistics.setTotalRequests(String.valueOf(totalRequests));
        statistics.setTodayRequests(todayRequests);
        statistics.setTotalTokens(String.valueOf(totalTokens));
        statistics.setTodayTokens(String.valueOf(todayTokens));
        
        // 统计总费用
        BigDecimal totalCost = apiKeys.stream()
                .map(k -> k.getTotalCost() != null ? k.getTotalCost() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        statistics.setTotalCost(totalCost);
        // 今日费用从Redis读取
        BigDecimal todayCost = BigDecimal.ZERO;
        if (dailyCostCounter != null) {
            for (DeveloperApiKey k : apiKeys) {
                todayCost = todayCost.add(dailyCostCounter.getTodayCost(k.getId()));
            }
        }
        statistics.setTodayCost(todayCost);
        
        return statistics;
    }
    
    /**
     * 获取密钥并验证所有权
     * 
     * @param apiKeyId API密钥ID
     * @param userId 用户ID
     * @return API密钥领域对象
     */
    private DeveloperApiKey getAndValidateOwnership(Long apiKeyId, Long userId) {
        DeveloperApiKey existing = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> {
                    log.warn("API密钥不存在，ID: {}", apiKeyId);
                    return BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_NOT_FOUND, "API密钥不存在");
                });
        
        if (!existing.getUserId().equals(userId)) {
            log.warn("用户 {} 无权访问API密钥，密钥所属用户: {}", userId, existing.getUserId());
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "无权操作此API密钥");
        }
        
        return existing;
    }
}

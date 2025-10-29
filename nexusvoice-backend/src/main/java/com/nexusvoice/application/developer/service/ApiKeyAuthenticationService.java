package com.nexusvoice.application.developer.service;

import com.nexusvoice.domain.developer.model.DeveloperApiKey;
import com.nexusvoice.domain.developer.repository.DeveloperApiKeyRepository;
import com.nexusvoice.domain.developer.service.ApiKeyGeneratorService;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
 

/**
 * 开发者API密钥认证服务
 * 负责密钥验证、限流、使用统计等核心功能
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Slf4j
@Service
public class ApiKeyAuthenticationService {
    
    @Autowired
    private DeveloperApiKeyRepository apiKeyRepository;
    
    @Autowired
    private ApiKeyGeneratorService apiKeyGenerator;
    
    @Autowired
    private com.nexusvoice.domain.developer.port.RateLimiterPort rateLimiter;
    
    @Autowired
    private com.nexusvoice.domain.developer.port.DailyCostCounterPort dailyCostCounter;
    
    /**
     * 验证并获取API密钥
     * 
     * @param apiKey 原始API密钥
     * @param clientIp 客户端IP
     * @return 验证通过的API密钥领域对象
     */
    public DeveloperApiKey authenticateAndValidate(String apiKey, String clientIp) {
        // 1. 验证密钥格式
        if (!apiKeyGenerator.validateFormat(apiKey)) {
            log.warn("API密钥格式无效: {}", apiKeyGenerator.generatePrefix(apiKey));
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_INVALID, "API密钥格式无效");
        }
        
        // 2. 计算哈希值并查询数据库
        String keyHash = apiKeyGenerator.hash(apiKey);
        DeveloperApiKey developerApiKey = apiKeyRepository.findByKeyValueHash(keyHash)
                .orElseThrow(() -> {
                    log.warn("API密钥不存在: {}", apiKeyGenerator.generatePrefix(apiKey));
                    return BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_NOT_FOUND, "API密钥不存在");
                });
        
        // 3. 检查密钥状态
        if (!developerApiKey.isValid()) {
            if (developerApiKey.isExpired()) {
                log.warn("API密钥已过期，ID: {}", developerApiKey.getId());
                throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_EXPIRED, "API密钥已过期");
            } else {
                log.warn("API密钥已禁用，ID: {}", developerApiKey.getId());
                throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_DISABLED, "API密钥已禁用");
            }
        }
        
        // 4. 检查IP白名单
        if (!developerApiKey.isIpAllowed(clientIp)) {
            log.warn("IP地址不在白名单，API密钥ID: {}，IP: {}", developerApiKey.getId(), clientIp);
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_IP_DENIED, "IP地址不在白名单");
        }
        
        log.info("API密钥验证成功，ID: {}，用户ID: {}", developerApiKey.getId(), developerApiKey.getUserId());
        return developerApiKey;
    }
    
    /**
     * 检查权限范围
     * 
     * @param apiKey API密钥对象
     * @param requiredScope 需要的权限
     */
    public void checkScope(DeveloperApiKey apiKey, String requiredScope) {
        if (!apiKey.hasScope(requiredScope)) {
            log.warn("API密钥权限不足，ID: {}，需要权限: {}", apiKey.getId(), requiredScope);
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_SCOPE_DENIED, 
                    String.format("API密钥没有%s权限", requiredScope));
        }
    }
    
    /**
     * 检查模型访问权限
     * 
     * @param apiKey API密钥对象
     * @param modelKey 模型标识（如openai:gpt-4）
     */
    public void checkModelAccess(DeveloperApiKey apiKey, String modelKey) {
        if (!apiKey.isModelAllowed(modelKey)) {
            log.warn("API密钥不允许访问该模型，ID: {}，模型: {}", apiKey.getId(), modelKey);
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_MODEL_DENIED, 
                    String.format("API密钥不允许访问模型: %s", modelKey));
        }
    }
    
    /**
     * 检查并扣减速率限制
     * 
     * @param apiKey API密钥对象
     */
    public void checkAndConsumeRateLimit(DeveloperApiKey apiKey) {
        // 由端口实现限流（基础设施层提供Redis实现）
        rateLimiter.checkAndConsumePerMinute(apiKey.getId(), apiKey.getRateLimitPerMinute());
        rateLimiter.checkAndConsumePerDay(apiKey.getId(), apiKey.getRateLimitPerDay());
    }
    
    /**
     * 检查Token限额
     * 
     * @param apiKey API密钥对象
     */
    public void checkTokenLimit(DeveloperApiKey apiKey) {
        // 检查每日Token限额
        if (!apiKey.isWithinDailyTokenLimit()) {
            log.warn("API密钥每日Token限额超限，ID: {}，今日使用: {}，限制: {}", 
                    apiKey.getId(), apiKey.getTodayTokens(), apiKey.getDailyTokenLimit());
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_TOKEN_LIMIT_EXCEEDED, 
                    "API每日Token使用量超限");
        }
    }
    
    /**
     * 检查费用限额
     * 
     * @param apiKey API密钥对象
     */
    public void checkCostLimit(DeveloperApiKey apiKey) {
        if (apiKey.getDailyCostLimit() == null) {
            return; // 未设置限额，不检查
        }
        BigDecimal todayCost = dailyCostCounter.getTodayCost(apiKey.getId());
        
        if (!apiKey.isWithinDailyCostLimit(todayCost)) {
            log.warn("API密钥每日费用限额超限，ID: {}，今日费用: {}，限制: {}", 
                    apiKey.getId(), todayCost, apiKey.getDailyCostLimit());
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_COST_LIMIT_EXCEEDED, 
                    "API每日费用超限");
        }
    }
    
    /**
     * 记录API调用（使用计数、Token统计、费用累计）
     * 
     * @param apiKey API密钥对象
     * @param clientIp 客户端IP
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     * @param cost 费用（元）
     */
    public void recordUsage(DeveloperApiKey apiKey, String clientIp, long inputTokens, long outputTokens, BigDecimal cost) {
        try {
            // 1. 更新使用计数
            apiKey.recordUsage(clientIp);
            
            // 2. 更新Token统计
            apiKey.addTokenUsage(inputTokens, outputTokens);
            
            // 3. 更新费用
            if (cost != null && cost.compareTo(BigDecimal.ZERO) > 0) {
                apiKey.addCost(cost);
                dailyCostCounter.addTodayCost(apiKey.getId(), cost);
            }
            
            // 4. 保存到数据库
            apiKeyRepository.update(apiKey);
            
            log.info("API调用记录成功，密钥ID: {}，输入Token: {}，输出Token: {}，费用: {}元", 
                    apiKey.getId(), inputTokens, outputTokens, cost);
        } catch (Exception e) {
            log.error("记录API使用失败，密钥ID: {}", apiKey.getId(), e);
            // 记录失败不影响主流程，只记录日志
        }
    }
}

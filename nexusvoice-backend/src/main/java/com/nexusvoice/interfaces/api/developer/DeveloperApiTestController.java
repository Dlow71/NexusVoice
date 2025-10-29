package com.nexusvoice.interfaces.api.developer;

import com.nexusvoice.application.developer.service.ApiKeyAuthenticationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.domain.developer.model.DeveloperApiKey;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.context.DeveloperApiContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 开发者API测试控制器
 * 用于测试API密钥功能和提供使用示例
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/developer/test")
@Tag(name = "开发者API测试", description = "开发者API密钥测试相关接口")
public class DeveloperApiTestController {
    
    @Autowired
    private ApiKeyAuthenticationService apiKeyAuthenticationService;
    
    private static final String API_KEY_HEADER = "X-API-Key";
    
    /**
     * 测试API密钥认证
     * 
     * 使用示例：
     * curl -X GET http://localhost:8081/api/v1/developer/test/auth \
     *   -H "X-API-Key: sk-nv-your-api-key-here"
     */
    @Operation(summary = "测试API密钥认证", 
               description = "测试API密钥是否有效，返回密钥的基本信息")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "认证成功"),
        @ApiResponse(responseCode = "401", description = "API密钥无效或已过期"),
        @ApiResponse(responseCode = "403", description = "IP地址不在白名单"),
        @ApiResponse(responseCode = "429", description = "请求频率超限"),
        @ApiResponse(responseCode = "500", description = "认证失败")
    })
    @GetMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> testAuthentication(
            @Parameter(description = "API密钥（在请求头X-API-Key中传递）", required = true)
            @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
            HttpServletRequest request) {
        
        log.info("收到API密钥认证测试请求，客户端IP: {}", getClientIp(request));
        
        // 1. 检查API密钥是否存在
        if (!StringUtils.hasText(apiKey)) {
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_INVALID, 
                    "请在请求头中提供X-API-Key");
        }
        
        // 2. 获取客户端IP
        String clientIp = getClientIp(request);
        
        // 3. 验证API密钥
        DeveloperApiKey developerApiKey = apiKeyAuthenticationService.authenticateAndValidate(apiKey, clientIp);
        // 设置调用上下文
        DeveloperApiContext.setDeveloperApiKeyId(developerApiKey.getId());
        DeveloperApiContext.setAuthType("API_KEY");
        
        try {
            // 4. 检查速率限制
            apiKeyAuthenticationService.checkAndConsumeRateLimit(developerApiKey);
        
        // 5. 返回密钥信息（不包含敏感信息）
        Map<String, Object> result = new HashMap<>();
        result.put("keyId", String.valueOf(developerApiKey.getId()));
        result.put("keyName", developerApiKey.getKeyName());
        result.put("userId", String.valueOf(developerApiKey.getUserId()));
        result.put("scopes", developerApiKey.getScopes());
        result.put("status", developerApiKey.getStatus().name());
        result.put("rateLimitPerMinute", developerApiKey.getRateLimitPerMinute());
        result.put("rateLimitPerDay", developerApiKey.getRateLimitPerDay());
        result.put("totalRequestCount", developerApiKey.getTotalRequestCount());
        result.put("todayRequestCount", developerApiKey.getTodayRequestCount());
        result.put("clientIp", clientIp);
        result.put("message", "API密钥认证成功！");
        
        log.info("API密钥认证测试成功，密钥ID: {}, 用户ID: {}", 
                developerApiKey.getId(), developerApiKey.getUserId());
        
        return Result.success("API密钥认证成功", result);
        } finally {
            DeveloperApiContext.clear();
        }
    }
    
    /**
     * 测试权限检查
     * 
     * 使用示例：
     * curl -X GET http://localhost:8081/api/v1/developer/test/scope/chat \
     *   -H "X-API-Key: sk-nv-your-api-key-here"
     */
    @Operation(summary = "测试权限检查", 
               description = "测试API密钥是否具有指定权限")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "权限检查成功"),
        @ApiResponse(responseCode = "401", description = "API密钥无效"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "500", description = "权限检查失败")
    })
    @GetMapping(value = "/scope/{scope}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> testScope(
            @Parameter(description = "需要检查的权限范围", required = true, 
                      example = "chat")
            @PathVariable("scope") String scope,
            @Parameter(description = "API密钥（在请求头X-API-Key中传递）", required = true)
            @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
            HttpServletRequest request) {
        
        log.info("收到权限检查测试请求，权限: {}", scope);
        
        // 1. 验证API密钥
        if (!StringUtils.hasText(apiKey)) {
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_INVALID, 
                    "请在请求头中提供X-API-Key");
        }
        
        String clientIp = getClientIp(request);
        DeveloperApiKey developerApiKey = apiKeyAuthenticationService.authenticateAndValidate(apiKey, clientIp);
        DeveloperApiContext.setDeveloperApiKeyId(developerApiKey.getId());
        DeveloperApiContext.setAuthType("API_KEY");
        try {
            // 2. 检查权限
            apiKeyAuthenticationService.checkScope(developerApiKey, scope);
        
        // 3. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("keyId", String.valueOf(developerApiKey.getId()));
        result.put("scope", scope);
        result.put("hasPermission", true);
        result.put("allScopes", developerApiKey.getScopes());
        result.put("message", String.format("API密钥具有 %s 权限！", scope));
        
        log.info("权限检查测试成功，密钥ID: {}, 权限: {}", developerApiKey.getId(), scope);
        
        return Result.success("权限检查成功", result);
        } finally {
            DeveloperApiContext.clear();
        }
    }
    
    /**
     * 测试模型访问权限
     * 
     * 使用示例：
     * curl -X GET "http://localhost:8081/api/v1/developer/test/model?modelKey=openai:gpt-4" \
     *   -H "X-API-Key: sk-nv-your-api-key-here"
     */
    @Operation(summary = "测试模型访问权限", 
               description = "测试API密钥是否允许访问指定模型")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "模型访问检查成功"),
        @ApiResponse(responseCode = "401", description = "API密钥无效"),
        @ApiResponse(responseCode = "403", description = "不允许访问该模型"),
        @ApiResponse(responseCode = "500", description = "模型访问检查失败")
    })
    @GetMapping(value = "/model", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> testModelAccess(
            @Parameter(description = "模型标识（如openai:gpt-4）", required = true)
            @RequestParam("modelKey") String modelKey,
            @Parameter(description = "API密钥（在请求头X-API-Key中传递）", required = true)
            @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
            HttpServletRequest request) {
        
        log.info("收到模型访问检查测试请求，模型: {}", modelKey);
        
        // 1. 验证API密钥
        if (!StringUtils.hasText(apiKey)) {
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_INVALID, 
                    "请在请求头中提供X-API-Key");
        }
        
        String clientIp = getClientIp(request);
        DeveloperApiKey developerApiKey = apiKeyAuthenticationService.authenticateAndValidate(apiKey, clientIp);
        DeveloperApiContext.setDeveloperApiKeyId(developerApiKey.getId());
        DeveloperApiContext.setAuthType("API_KEY");
        try {
            // 2. 检查模型访问权限
            apiKeyAuthenticationService.checkModelAccess(developerApiKey, modelKey);
        
        // 3. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("keyId", String.valueOf(developerApiKey.getId()));
        result.put("modelKey", modelKey);
        result.put("hasAccess", true);
        result.put("allowedModels", developerApiKey.getAllowedModels());
        result.put("message", String.format("API密钥允许访问模型: %s", modelKey));
        
        log.info("模型访问检查测试成功，密钥ID: {}, 模型: {}", developerApiKey.getId(), modelKey);
        
        return Result.success("模型访问检查成功", result);
        } finally {
            DeveloperApiContext.clear();
        }
    }
    
    /**
     * 测试完整流程（认证+限流+权限+使用记录）
     * 
     * 使用示例：
     * curl -X POST http://localhost:8081/api/v1/developer/test/full \
     *   -H "X-API-Key: sk-nv-your-api-key-here" \
     *   -H "Content-Type: application/json" \
     *   -d '{"scope":"chat","modelKey":"openai:gpt-4","inputTokens":100,"outputTokens":50}'
     */
    @Operation(summary = "测试完整流程", 
               description = "测试API密钥的完整使用流程（认证、限流、权限检查、使用记录）")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "测试成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "401", description = "API密钥无效"),
        @ApiResponse(responseCode = "403", description = "权限不足"),
        @ApiResponse(responseCode = "429", description = "请求频率超限"),
        @ApiResponse(responseCode = "500", description = "测试失败")
    })
    @PostMapping(value = "/full", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result<Map<String, Object>> testFullFlow(
            @Parameter(description = "API密钥（在请求头X-API-Key中传递）", required = true)
            @RequestHeader(value = API_KEY_HEADER, required = false) String apiKey,
            @Parameter(description = "测试请求参数", required = true)
            @RequestBody TestRequest testRequest,
            HttpServletRequest request) {
        
        log.info("收到完整流程测试请求，权限: {}, 模型: {}", 
                testRequest.getScope(), testRequest.getModelKey());
        
        // 1. 验证API密钥
        if (!StringUtils.hasText(apiKey)) {
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_INVALID, 
                    "请在请求头中提供X-API-Key");
        }
        
        String clientIp = getClientIp(request);
        DeveloperApiKey developerApiKey = apiKeyAuthenticationService.authenticateAndValidate(apiKey, clientIp);
        DeveloperApiContext.setDeveloperApiKeyId(developerApiKey.getId());
        DeveloperApiContext.setAuthType("API_KEY");
        try {
            // 2. 检查速率限制
            apiKeyAuthenticationService.checkAndConsumeRateLimit(developerApiKey);
        
        // 3. 检查权限
        apiKeyAuthenticationService.checkScope(developerApiKey, testRequest.getScope());
        
        // 4. 检查模型访问权限
        if (testRequest.getModelKey() != null) {
            apiKeyAuthenticationService.checkModelAccess(developerApiKey, testRequest.getModelKey());
        }
        
        // 5. 检查Token限额
        apiKeyAuthenticationService.checkTokenLimit(developerApiKey);
        
        // 6. 检查费用限额
        apiKeyAuthenticationService.checkCostLimit(developerApiKey);
        
        // 7. 记录使用情况（模拟）
        long inputTokens = testRequest.getInputTokens() != null ? testRequest.getInputTokens() : 100;
        long outputTokens = testRequest.getOutputTokens() != null ? testRequest.getOutputTokens() : 50;
        BigDecimal cost = new BigDecimal("0.001"); // 模拟费用
        
        apiKeyAuthenticationService.recordUsage(developerApiKey, clientIp, inputTokens, outputTokens, cost);
        
        // 8. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("keyId", String.valueOf(developerApiKey.getId()));
        result.put("step1_auth", "✅ 认证成功");
        result.put("step2_rateLimit", "✅ 速率限制检查通过");
        result.put("step3_scope", "✅ 权限检查通过: " + testRequest.getScope());
        result.put("step4_model", "✅ 模型访问检查通过: " + testRequest.getModelKey());
        result.put("step5_tokenLimit", "✅ Token限额检查通过");
        result.put("step6_costLimit", "✅ 费用限额检查通过");
        result.put("step7_record", "✅ 使用记录已保存");
        result.put("simulatedUsage", Map.of(
                "inputTokens", inputTokens,
                "outputTokens", outputTokens,
                "cost", cost.toString() + " 元"
        ));
        result.put("message", "API密钥完整流程测试成功！所有检查均通过。");
        
        log.info("完整流程测试成功，密钥ID: {}", developerApiKey.getId());
        
        return Result.success("完整流程测试成功", result);
        } finally {
            DeveloperApiContext.clear();
        }
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 测试请求参数
     */
    public static class TestRequest {
        private String scope;
        private String modelKey;
        private Long inputTokens;
        private Long outputTokens;
        
        public String getScope() {
            return scope;
        }
        
        public void setScope(String scope) {
            this.scope = scope;
        }
        
        public String getModelKey() {
            return modelKey;
        }
        
        public void setModelKey(String modelKey) {
            this.modelKey = modelKey;
        }
        
        public Long getInputTokens() {
            return inputTokens;
        }
        
        public void setInputTokens(Long inputTokens) {
            this.inputTokens = inputTokens;
        }
        
        public Long getOutputTokens() {
            return outputTokens;
        }
        
        public void setOutputTokens(Long outputTokens) {
            this.outputTokens = outputTokens;
        }
    }
}

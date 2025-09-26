package com.nexusvoice.enums;

/**
 * 错误码枚举
 * 
 * 错误码规范：
 * - 成功：200
 * - 客户端错误：400-499
 * - 服务端错误：500-599
 * - 业务错误：1000-9999
 * 
 * @author NexusVoice
 * @since 2025-09-22
 */
public enum ErrorCodeEnum {
    
    // ========== 通用错误码 ==========
    SUCCESS(200, "操作成功"),
    
    // ========== 客户端错误 4xx ==========
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    REQUEST_TIMEOUT(408, "请求超时"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    
    // ========== 服务端错误 5xx ==========
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),
    
    // ========== 业务错误码 1xxx ==========
    // 用户相关 10xx
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_PASSWORD_ERROR(1003, "用户密码错误"),
    USER_ACCOUNT_DISABLED(1004, "用户账号已禁用"),
    USER_ACCOUNT_LOCKED(1005, "用户账号已锁定"),
    USER_EMAIL_EXISTS(1006, "邮箱已被注册"),
    USER_PHONE_EXISTS(1007, "手机号已被注册"),
    USER_BANNED(1008, "用户已被封禁"),
    USER_STATUS_ABNORMAL(1009, "用户状态异常"),
    
    // 参数相关 10xx
    PARAM_ERROR(1010, "参数错误"),
    
    // 认证相关 11xx
    TOKEN_INVALID(1101, "令牌无效"),
    TOKEN_EXPIRED(1102, "令牌已过期"),
    TOKEN_MISSING(1103, "令牌缺失"),
    LOGIN_FAILED(1104, "登录失败"),
    LOGOUT_FAILED(1105, "退出登录失败"),
    
    // 权限相关 12xx
    PERMISSION_DENIED(1201, "权限不足"),
    ROLE_NOT_FOUND(1202, "角色不存在"),
    RESOURCE_ACCESS_DENIED(1203, "资源访问被拒绝"),
    
    // 数据相关 13xx
    DATA_NOT_FOUND(1301, "数据不存在"),
    DATA_ALREADY_EXISTS(1302, "数据已存在"),
    DATA_VALIDATION_ERROR(1303, "数据验证失败"),
    DATA_INTEGRITY_ERROR(1304, "数据完整性错误"),
    
    // 文件相关 14xx
    FILE_NOT_FOUND(1401, "文件不存在"),
    FILE_UPLOAD_FAILED(1402, "文件上传失败"),
    FILE_SIZE_EXCEEDED(1403, "文件大小超出限制"),
    FILE_TYPE_NOT_SUPPORTED(1404, "文件类型不支持"),
    
    // AI相关 15xx
    AI_SERVICE_ERROR(1501, "AI服务错误"),
    AI_MODEL_NOT_AVAILABLE(1502, "AI模型不可用"),
    AI_REQUEST_FAILED(1503, "AI请求失败"),
    AI_RESPONSE_INVALID(1504, "AI响应无效"),
    AI_TOKEN_LIMIT_EXCEEDED(1505, "AI令牌数量超出限制"),
    AI_RATE_LIMIT_EXCEEDED(1506, "AI请求频率超出限制"),
    AI_API_KEY_INVALID(1507, "AI API密钥无效"),
    AI_MODEL_CONFIG_ERROR(1508, "AI模型配置错误"),
    
    // 对话相关 20xx
    CONVERSATION_NOT_FOUND(2001, "对话不存在"),
    CONVERSATION_ACCESS_DENIED(2002, "无权访问此对话"),
    CONVERSATION_MESSAGE_LIMIT_EXCEEDED(2003, "对话消息数量超出限制"),
    CONVERSATION_TOKEN_LIMIT_EXCEEDED(2004, "对话令牌数量超出限制"),
    CONVERSATION_STATUS_INVALID(2005, "对话状态无效"),
    MESSAGE_NOT_FOUND(2006, "消息不存在"),
    MESSAGE_CONTENT_EMPTY(2007, "消息内容不能为空"),
    MESSAGE_CONTENT_TOO_LONG(2008, "消息内容过长"),
    
    // WebSocket相关 16xx
    WEBSOCKET_CONNECTION_FAILED(1601, "WebSocket连接失败"),
    WEBSOCKET_MESSAGE_SEND_FAILED(1602, "WebSocket消息发送失败"),
    WEBSOCKET_AUTHENTICATION_FAILED(1603, "WebSocket认证失败"),
    
    // 第三方服务相关 17xx
    THIRD_PARTY_SERVICE_ERROR(1701, "第三方服务错误"),
    THIRD_PARTY_API_LIMIT_EXCEEDED(1702, "第三方API调用次数超限"),
    THIRD_PARTY_AUTHENTICATION_FAILED(1703, "第三方服务认证失败"),
    
    // 配置相关 18xx
    CONFIG_NOT_FOUND(1801, "配置不存在"),
    CONFIG_INVALID(1802, "配置无效"),
    CONFIG_UPDATE_FAILED(1803, "配置更新失败"),
    
    // 缓存相关 19xx
    CACHE_ERROR(1901, "缓存错误"),
    CACHE_KEY_NOT_FOUND(1902, "缓存键不存在"),
    CACHE_OPERATION_FAILED(1903, "缓存操作失败");
    
    /**
     * 错误码
     */
    private final Integer code;
    
    /**
     * 错误信息
     */
    private final String message;
    
    ErrorCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * 根据错误码获取枚举
     */
    public static ErrorCodeEnum getByCode(Integer code) {
        for (ErrorCodeEnum errorCode : values()) {
            if (errorCode.getCode().equals(code)) {
                return errorCode;
            }
        }
        return INTERNAL_SERVER_ERROR;
    }
}

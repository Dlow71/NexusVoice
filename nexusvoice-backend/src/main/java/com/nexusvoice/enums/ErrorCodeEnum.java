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
    SYSTEM_ERROR(500, "系统错误"),
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
    FILE_IS_EMPTY(1405, "文件为空"),
    FILE_NAME_INVALID(1406, "文件名无效"),
    FILE_DOWNLOAD_FAILED(1407, "文件下载失败"),
    
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
    CONVERSATION_TITLE_GENERATION_FAILED(2009, "对话标题生成失败"),
    CONVERSATION_TITLE_TOO_SHORT(2010, "对话内容不足，无法生成标题"),
    
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
    CONFIG_ALREADY_EXISTS(1804, "配置已存在"),
    CONFIG_CREATE_FAILED(1805, "配置创建失败"),
    CONFIG_DELETE_FAILED(1806, "配置删除失败"),
    CONFIG_READONLY(1807, "配置为只读"),
    
    // 缓存相关 19xx
    CACHE_ERROR(1901, "缓存错误"),
    CACHE_KEY_NOT_FOUND(1902, "缓存键不存在"),
    CACHE_OPERATION_FAILED(1903, "缓存操作失败"),
    
    // TTS相关 20xx
    TTS_SERVICE_ERROR(2001, "TTS服务错误"),
    TTS_TEXT_INVALID(2002, "TTS文本无效"),
    TTS_VOICE_TYPE_NOT_SUPPORTED(2003, "TTS语音类型不支持"),
    TTS_AUDIO_FORMAT_NOT_SUPPORTED(2004, "TTS音频格式不支持"),
    TTS_SPEED_RATIO_INVALID(2005, "TTS语速比例无效"),
    TTS_GENERATION_FAILED(2006, "TTS音频生成失败"),
    TTS_CONNECTION_FAILED(2007, "TTS服务连接失败"),
    TTS_TIMEOUT(2008, "TTS处理超时"),
    
    // OAuth相关 30xx
    OAUTH_USER_NOT_FOUND(3001, "OAuth用户不存在"),
    OAUTH_PROVIDER_NOT_SUPPORTED(3002, "不支持的OAuth提供商"),
    OAUTH_EMAIL_ALREADY_BOUND(3003, "该邮箱已绑定其他账户"),
    OAUTH_ACCOUNT_ALREADY_BOUND(3004, "该OAuth账号已被其他用户绑定"),
    OAUTH_CALLBACK_FAILED(3005, "OAuth回调处理失败"),
    OAUTH_TOKEN_EXCHANGE_FAILED(3006, "OAuth令牌交换失败"),
    OAUTH_USER_INFO_FETCH_FAILED(3007, "获取OAuth用户信息失败"),
    OAUTH_BIND_FAILED(3008, "OAuth账号绑定失败"),
    OAUTH_UNBIND_FAILED(3009, "OAuth账号解绑失败"),
    OAUTH_UNBIND_NO_PASSWORD(3010, "解绑失败：请先设置密码"),
    OAUTH_STATE_MISMATCH(3011, "OAuth状态验证失败"),
    OAUTH_ACCESS_DENIED(3012, "OAuth授权被拒绝"),
    
    // 图像生成相关 21xx
    IMAGE_SERVICE_ERROR(2101, "图像生成服务错误"),
    IMAGE_PROMPT_INVALID(2102, "图像描述提示词无效"),
    IMAGE_MODEL_NOT_SUPPORTED(2103, "图像生成模型不支持"),
    IMAGE_SIZE_INVALID(2104, "图像尺寸无效"),
    IMAGE_GENERATION_FAILED(2105, "图像生成失败"),
    IMAGE_CONNECTION_FAILED(2106, "图像生成服务连接失败"),
    IMAGE_TIMEOUT(2107, "图像生成处理超时"),
    IMAGE_BATCH_SIZE_INVALID(2108, "图像批量大小无效"),
    IMAGE_SEED_INVALID(2109, "图像生成种子无效"),
    IMAGE_STEPS_INVALID(2110, "图像生成步数无效"),
    IMAGE_GUIDANCE_SCALE_INVALID(2111, "图像引导比例无效"),
    IMAGE_CFG_INVALID(2112, "图像CFG参数无效"),
    IMAGE_API_KEY_INVALID(2113, "图像生成API密钥无效"),
    
    // 功能实现相关
    FUNCTION_NOT_IMPLEMENTED(2200, "功能暂未实现"),
    
    // Redis相关错误码 40xx
    REDIS_CONNECTION_FAILED(4001, "Redis连接失败"),
    REDIS_OPERATION_FAILED(4002, "Redis操作失败"),
    REDIS_KEY_NOT_FOUND(4003, "Redis键不存在"),
    REDIS_LOCK_ACQUIRE_FAILED(4004, "获取分布式锁失败"),
    REDIS_LOCK_RELEASE_FAILED(4005, "释放分布式锁失败"),
    RATE_LIMIT_EXCEEDED(4006, "请求频率超过限制，请稍后再试"),
    BLOOM_FILTER_ERROR(4007, "布隆过滤器操作失败"),
    REDIS_CACHE_PENETRATION(4008, "缓存穿透保护"),
    REDIS_CACHE_BREAKDOWN(4009, "缓存击穿保护"),
    REDIS_CACHE_AVALANCHE(4010, "缓存雪崩保护"),
    REDIS_TRANSACTION_FAILED(4011, "Redis事务执行失败"),
    REDIS_PIPELINE_FAILED(4012, "Redis管道操作失败"),
    REDIS_SCRIPT_FAILED(4013, "Redis脚本执行失败"),
    REDIS_PUB_SUB_FAILED(4014, "Redis发布订阅失败"),
    REDIS_CLUSTER_ERROR(4015, "Redis集群错误"),
    
    // 消息队列相关 23xx
    MQ_SEND_ERROR(2301, "消息发送失败"),
    MQ_CONSUME_ERROR(2302, "消息消费失败"),
    MQ_CONNECTION_ERROR(2303, "消息队列连接失败"),
    MQ_TIMEOUT_ERROR(2304, "消息处理超时"),
    MQ_RETRY_EXCEEDED(2305, "消息重试次数超限"),
    MQ_TOPIC_NOT_FOUND(2306, "消息主题不存在"),
    MQ_CONSUMER_NOT_FOUND(2307, "消费者不存在"),
    MQ_TRANSACTION_ERROR(2308, "事务消息处理失败"),
    
    // RAG文档处理相关 41xx
    RAG_KNOWLEDGE_BASE_NOT_FOUND(4101, "知识库不存在"),
    RAG_KNOWLEDGE_BASE_ALREADY_EXISTS(4102, "知识库已存在"),
    RAG_KNOWLEDGE_BASE_NAME_DUPLICATE(4103, "知识库名称重复"),
    RAG_KNOWLEDGE_BASE_NOT_EMPTY(4104, "知识库不为空，无法删除"),
    RAG_KNOWLEDGE_BASE_STATUS_INVALID(4105, "知识库状态无效"),
    
    // RAG文件处理相关 42xx
    RAG_FILE_NOT_FOUND(4201, "文档文件不存在"),
    RAG_FILE_UPLOAD_FAILED(4202, "文档上传失败"),
    RAG_FILE_TYPE_NOT_SUPPORTED(4203, "不支持的文档类型"),
    RAG_FILE_SIZE_EXCEEDED(4204, "文档大小超出限制"),
    RAG_FILE_HASH_DUPLICATE(4205, "文档已存在（重复文件）"),
    RAG_FILE_PARSE_FAILED(4206, "文档解析失败"),
    RAG_FILE_ACCESS_DENIED(4207, "无权访问此文档"),
    RAG_FILE_PROCESSING(4208, "文档正在处理中"),
    RAG_FILE_PROCESS_FAILED(4209, "文档处理失败"),
    
    // RAG文档单元相关 43xx
    RAG_DOCUMENT_UNIT_NOT_FOUND(4301, "文档单元不存在"),
    RAG_DOCUMENT_SPLIT_FAILED(4302, "文档分割失败"),
    RAG_DOCUMENT_UNIT_TOO_LARGE(4303, "文档单元过大"),
    RAG_DOCUMENT_UNIT_EMPTY(4304, "文档单元内容为空"),
    
    // RAG向量化相关 44xx
    RAG_VECTORIZE_FAILED(4401, "向量化失败"),
    RAG_VECTORIZE_MODEL_ERROR(4402, "向量化模型错误"),
    RAG_VECTOR_DIMENSION_MISMATCH(4403, "向量维度不匹配"),
    RAG_EMBEDDING_NOT_FOUND(4404, "向量不存在"),
    RAG_EMBEDDING_MODEL_NOT_AVAILABLE(4405, "向量化模型不可用"),
    
    // RAG OCR相关 45xx
    RAG_OCR_FAILED(4501, "OCR识别失败"),
    RAG_OCR_MODEL_ERROR(4502, "OCR模型错误"),
    RAG_OCR_CONFIDENCE_LOW(4503, "OCR识别置信度过低"),
    RAG_OCR_LANGUAGE_NOT_SUPPORTED(4504, "OCR不支持该语言"),
    RAG_OCR_IMAGE_INVALID(4505, "OCR图像无效"),
    
    // RAG搜索相关 46xx
    RAG_SEARCH_FAILED(4601, "文档搜索失败"),
    RAG_SEARCH_NO_RESULTS(4602, "没有找到相关文档"),
    RAG_SEARCH_QUERY_INVALID(4603, "搜索查询无效"),
    RAG_SEARCH_INDEX_ERROR(4604, "搜索索引错误"),
    
    // RAG任务相关 47xx
    RAG_TASK_NOT_FOUND(4701, "处理任务不存在"),
    RAG_TASK_ALREADY_RUNNING(4702, "任务已在执行中"),
    RAG_TASK_CANCELLED(4703, "任务已取消"),
    RAG_TASK_TIMEOUT(4704, "任务处理超时"),
    RAG_TASK_RETRY_EXCEEDED(4705, "任务重试次数超限"),
    
    // RAG版本相关 48xx
    RAG_VERSION_NOT_FOUND(4801, "版本不存在"),
    RAG_VERSION_CREATE_FAILED(4802, "版本创建失败"),
    RAG_VERSION_DUPLICATE(4803, "版本号重复"),
    RAG_VERSION_INVALID(4804, "版本号格式无效"),
    RAG_VERSION_PUBLISH_FAILED(4805, "版本发布失败"),
    RAG_VERSION_NOT_PUBLISHED(4806, "版本未发布"),
    
    // RAG用户安装相关 49xx
    RAG_USER_RAG_NOT_FOUND(4901, "用户RAG不存在"),
    RAG_USER_INSTALL_FAILED(4902, "安装失败"),
    RAG_USER_ALREADY_INSTALLED(4903, "已经安装过该版本"),
    RAG_USER_UNINSTALL_FAILED(4904, "卸载失败"),
    RAG_USER_QUOTA_EXCEEDED(4905, "用户安装数量超限"),
    RAG_USER_ACCESS_DENIED(4906, "无权访问此RAG"),
    
    // 通用业务错误
    BUSINESS_ERROR(9999, "业务处理失败");
    
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

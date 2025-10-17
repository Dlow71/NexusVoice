package com.nexusvoice.domain.config.constant;

/**
 * 系统配置键常量
 * 集中管理所有系统配置项的key
 *
 * @author NexusVoice
 * @since 2025-10-17
 */
public class SystemConfigKey {

    // ==================== 系统基础配置 ====================
    public static final String SYSTEM_NAME = "system.name";
    public static final String SYSTEM_VERSION = "system.version";
    public static final String SYSTEM_DESCRIPTION = "system.description";

    // ==================== AI模型配置 ====================
    /**
     * 默认AI模型（完整格式：provider:model）
     * 默认值：openai:gpt-oss-20b
     */
    public static final String AI_MODEL_DEFAULT = "ai.model.default";
    
    /**
     * 默认模型厂商
     * 默认值：openai
     */
    public static final String AI_MODEL_DEFAULT_PROVIDER = "ai.model.default.provider";
    
    /**
     * 默认模型代码
     * 默认值：gpt-oss-20b
     */
    public static final String AI_MODEL_DEFAULT_CODE = "ai.model.default.code";
    
    /**
     * AI默认温度参数
     * 默认值：0.7
     */
    public static final String AI_TEMPERATURE_DEFAULT = "ai.temperature.default";
    
    /**
     * AI默认最大令牌数
     * 默认值：2000
     */
    public static final String AI_MAX_TOKENS_DEFAULT = "ai.max_tokens.default";
    
    /**
     * 默认系统提示词
     * 默认值：你是一个有用的AI助手
     */
    public static final String AI_SYSTEM_PROMPT_DEFAULT = "ai.system_prompt.default";

    // ==================== 对话配置 ====================
    /**
     * 默认对话标题
     * 默认值：新对话
     */
    public static final String CONVERSATION_TITLE_DEFAULT = "conversation.title.default";
    
    /**
     * 对话历史最大条数
     * 默认值：20
     */
    public static final String CONVERSATION_MAX_HISTORY = "conversation.max_history";
    
    /**
     * 对话超时时间（秒）
     * 默认值：300
     */
    public static final String CONVERSATION_TIMEOUT = "conversation.timeout";
    
    /**
     * 单个对话最大消息数
     * 默认值：100
     */
    public static final String CONVERSATION_MAX_MESSAGES = "conversation.max_messages";
    
    /**
     * 单个对话最大令牌数
     * 默认值：50000
     */
    public static final String CONVERSATION_MAX_TOKENS = "conversation.max_tokens";

    // ==================== 文件上传配置 ====================
    public static final String FILE_UPLOAD_MAX_SIZE = "file.upload.max_size";
    public static final String FILE_UPLOAD_ALLOWED_TYPES = "file.upload.allowed_types";

    // ==================== 安全配置 ====================
    public static final String SECURITY_JWT_EXPIRE_TIME = "security.jwt.expire_time";
    public static final String SECURITY_PASSWORD_MIN_LENGTH = "security.password.min_length";

    // ==================== 缓存配置 ====================
    public static final String CACHE_REDIS_EXPIRE_TIME = "cache.redis.expire_time";
    public static final String CACHE_SYSTEM_CONFIG_EXPIRE_TIME = "cache.system_config.expire_time";

    // ==================== 搜索配置 ====================
    public static final String SEARCH_ENABLED = "search.enabled";
    public static final String SEARCH_PROVIDER = "search.provider";

    // ==================== TTS配置 ====================
    public static final String TTS_VOICE_DEFAULT = "tts.voice.default";
    public static final String TTS_SPEED_DEFAULT = "tts.speed.default";
    public static final String TTS_ENCODING_DEFAULT = "tts.encoding.default";

    private SystemConfigKey() {
        // 工具类，禁止实例化
    }
}

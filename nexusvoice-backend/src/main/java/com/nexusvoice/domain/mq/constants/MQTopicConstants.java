package com.nexusvoice.domain.mq.constants;

/**
 * 消息队列主题常量
 * 命名规范：业务域_功能_动作
 * 
 * @author Dlow
 * @date 2025/10/18
 */
public class MQTopicConstants {
    
    // ==================== 订单相关主题 ====================
    /**
     * 订单创建主题
     */
    public static final String TOPIC_ORDER_CREATE = "order_create_topic";
    
    /**
     * 订单支付主题
     */
    public static final String TOPIC_ORDER_PAYMENT = "order_payment_topic";
    
    /**
     * 订单超时关闭主题（延迟消息）
     */
    public static final String TOPIC_ORDER_TIMEOUT = "order_timeout_topic";
    
    /**
     * 订单状态变更主题
     */
    public static final String TOPIC_ORDER_STATUS_CHANGE = "order_status_change_topic";
    
    /**
     * 订单退款主题
     */
    public static final String TOPIC_ORDER_REFUND = "order_refund_topic";
    
    // ==================== 用户相关主题 ====================
    /**
     * 用户注册主题
     */
    public static final String TOPIC_USER_REGISTER = "user_register_topic";
    
    /**
     * 用户登录主题
     */
    public static final String TOPIC_USER_LOGIN = "user_login_topic";
    
    /**
     * 用户信息变更主题
     */
    public static final String TOPIC_USER_INFO_CHANGE = "user_info_change_topic";
    
    // ==================== 对话相关主题 ====================
    /**
     * 对话创建主题
     */
    public static final String TOPIC_CONVERSATION_CREATE = "conversation_create_topic";
    
    /**
     * 对话消息主题
     */
    public static final String TOPIC_CONVERSATION_MESSAGE = "conversation_message_topic";
    
    /**
     * 对话结束主题
     */
    public static final String TOPIC_CONVERSATION_END = "conversation_end_topic";
    
    // ==================== AI相关主题 ====================
    /**
     * AI模型调用主题
     */
    public static final String TOPIC_AI_MODEL_CALL = "ai_model_call_topic";
    
    /**
     * AI图像生成主题
     */
    public static final String TOPIC_AI_IMAGE_GENERATE = "ai_image_generate_topic";
    
    /**
     * TTS语音合成主题
     */
    public static final String TOPIC_TTS_GENERATE = "tts_generate_topic";
    
    // ==================== 通知相关主题 ====================
    /**
     * 邮件通知主题
     */
    public static final String TOPIC_NOTIFICATION_EMAIL = "notification_email_topic";
    
    /**
     * 短信通知主题
     */
    public static final String TOPIC_NOTIFICATION_SMS = "notification_sms_topic";
    
    /**
     * 推送通知主题
     */
    public static final String TOPIC_NOTIFICATION_PUSH = "notification_push_topic";
    
    // ==================== RAG文档处理相关主题 ====================
    /**
     * RAG文档上传主题
     */
    public static final String TOPIC_RAG_DOCUMENT_UPLOAD = "rag_document_upload_topic";
    
    /**
     * RAG文档解析处理主题（阶段1：结构化解析与原文分割）
     */
    public static final String TOPIC_RAG_DOCUMENT_PROCESS = "rag_document_process_topic";
    
    /**
     * RAG文档向量化主题（阶段2：翻译增强与智能分割）
     */
    public static final String TOPIC_RAG_DOCUMENT_VECTORIZE = "rag_document_vectorize_topic";
    
    /**
     * RAG文档处理失败主题
     */
    public static final String TOPIC_RAG_DOCUMENT_FAILED = "rag_document_failed_topic";
    
    // ==================== 系统相关主题 ====================
    /**
     * 系统配置变更主题
     */
    public static final String TOPIC_SYSTEM_CONFIG_CHANGE = "system_config_change_topic";
    
    /**
     * 系统日志主题
     */
    public static final String TOPIC_SYSTEM_LOG = "system_log_topic";
    
    /**
     * 系统监控主题
     */
    public static final String TOPIC_SYSTEM_MONITOR = "system_monitor_topic";
    
    // ==================== 标签定义 ====================
    /**
     * 高优先级标签
     */
    public static final String TAG_HIGH_PRIORITY = "HIGH";
    
    /**
     * 中优先级标签
     */
    public static final String TAG_MEDIUM_PRIORITY = "MEDIUM";
    
    /**
     * 低优先级标签
     */
    public static final String TAG_LOW_PRIORITY = "LOW";
    
    /**
     * 重试标签
     */
    public static final String TAG_RETRY = "RETRY";
    
    /**
     * 死信标签
     */
    public static final String TAG_DLQ = "DLQ";
}

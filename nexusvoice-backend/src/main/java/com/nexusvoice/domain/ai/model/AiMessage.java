package com.nexusvoice.domain.ai.model;

/**
 * AI消息领域模型
 * 纯净的领域对象，不包含任何技术框架注解
 * 
 * @author NexusVoice
 * @since 2025-10-21
 */
public class AiMessage {

    /**
     * 消息角色
     */
    private MessageRole role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息名称（可选，用于函数调用）
     */
    private String name;

    /**
     * 构造函数
     */
    public AiMessage() {
    }

    public AiMessage(MessageRole role, String content) {
        this.role = role;
        this.content = content;
    }

    public AiMessage(MessageRole role, String content, String name) {
        this.role = role;
        this.content = content;
        this.name = name;
    }

    /**
     * 创建系统消息
     */
    public static AiMessage system(String content) {
        return new AiMessage(MessageRole.SYSTEM, content);
    }

    /**
     * 创建用户消息
     */
    public static AiMessage user(String content) {
        return new AiMessage(MessageRole.USER, content);
    }

    /**
     * 创建助手消息
     */
    public static AiMessage assistant(String content) {
        return new AiMessage(MessageRole.ASSISTANT, content);
    }

    /**
     * 创建函数消息
     */
    public static AiMessage function(String content, String name) {
        return new AiMessage(MessageRole.FUNCTION, content, name);
    }

    // Getter and Setter methods
    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 消息角色枚举
     */
    public enum MessageRole {
        /**
         * 系统消息
         */
        SYSTEM("system"),
        
        /**
         * 用户消息
         */
        USER("user"),
        
        /**
         * 助手消息
         */
        ASSISTANT("assistant"),
        
        /**
         * 函数消息
         */
        FUNCTION("function");

        private final String value;

        MessageRole(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static MessageRole fromValue(String value) {
            for (MessageRole role : values()) {
                if (role.value.equals(value)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unknown message role: " + value);
        }
    }
}

package com.nexusvoice.domain.common;

/**
 * 系统常量类
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
public final class Constants {

    private Constants() {
        // 防止实例化
    }

    /**
     * 系统相关常量
     */
    public static final class System {
        /** 系统名称 */
        public static final String NAME = "NexusVoice";
        /** 系统版本 */
        public static final String VERSION = "1.0.0";
        /** 默认编码 */
        public static final String DEFAULT_CHARSET = "UTF-8";
        /** 默认时区 */
        public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    }

    /**
     * 用户相关常量
     */
    public static final class User {
        /** 默认用户角色 */
        public static final String DEFAULT_ROLE = "USER";
        /** 管理员角色 */
        public static final String ADMIN_ROLE = "ADMIN";
        /** 用户状态：启用 */
        public static final Integer STATUS_ENABLED = 1;
        /** 用户状态：禁用 */
        public static final Integer STATUS_DISABLED = 0;
    }

    /**
     * Agent相关常量
     */
    public static final class Agent {
        /** Agent状态：启用 */
        public static final Integer STATUS_ENABLED = 1;
        /** Agent状态：禁用 */
        public static final Integer STATUS_DISABLED = 0;
        /** 默认系统提示词 */
        public static final String DEFAULT_SYSTEM_PROMPT = "你是一个智能助手，请根据用户的问题提供准确、有用的回答。";
        /** 默认欢迎消息 */
        public static final String DEFAULT_WELCOME_MESSAGE = "你好！我是你的智能助手，有什么可以帮助你的吗？";
    }

    /**
     * 会话相关常量
     */
    public static final class Session {
        /** 会话状态：活跃 */
        public static final Integer STATUS_ACTIVE = 1;
        /** 会话状态：归档 */
        public static final Integer STATUS_ARCHIVED = 0;
        /** 默认会话标题 */
        public static final String DEFAULT_TITLE = "新对话";
    }

    /**
     * 消息相关常量
     */
    public static final class Message {
        /** 消息角色：用户 */
        public static final String ROLE_USER = "user";
        /** 消息角色：助手 */
        public static final String ROLE_ASSISTANT = "assistant";
        /** 消息角色：系统 */
        public static final String ROLE_SYSTEM = "system";
        /** 消息类型：文本 */
        public static final String TYPE_TEXT = "text";
        /** 消息类型：图片 */
        public static final String TYPE_IMAGE = "image";
        /** 消息类型：文件 */
        public static final String TYPE_FILE = "file";
    }

    /**
     * 文件相关常量
     */
    public static final class File {
        /** 最大文件大小：10MB */
        public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
        /** 支持的图片格式 */
        public static final String[] SUPPORTED_IMAGE_FORMATS = {"jpg", "jpeg", "png", "gif", "webp"};
        /** 支持的文档格式 */
        public static final String[] SUPPORTED_DOCUMENT_FORMATS = {"pdf", "txt", "doc", "docx", "md"};
    }

    /**
     * 缓存相关常量
     */
    public static final class Cache {
        /** 用户信息缓存前缀 */
        public static final String USER_INFO_PREFIX = "user:info:";
        /** Agent信息缓存前缀 */
        public static final String AGENT_INFO_PREFIX = "agent:info:";
        /** 会话信息缓存前缀 */
        public static final String SESSION_INFO_PREFIX = "session:info:";
        /** 默认缓存过期时间（秒）：1小时 */
        public static final int DEFAULT_CACHE_EXPIRE = 3600;
    }

    /**
     * 分页相关常量
     */
    public static final class Page {
        /** 默认页码 */
        public static final int DEFAULT_PAGE_NUM = 1;
        /** 默认页面大小 */
        public static final int DEFAULT_PAGE_SIZE = 20;
        /** 最大页面大小 */
        public static final int MAX_PAGE_SIZE = 100;
    }
}

package com.nexusvoice.domain.config.service;

import com.nexusvoice.domain.config.constant.SystemConfigKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 系统配置领域服务
 * 提供便捷的配置获取方法
 * 缓存由SystemConfigCacheService统一管理
 *
 * @author NexusVoice
 * @since 2025-10-17
 * @updated 2025-10-18 集成SystemConfigCacheService
 */
@Slf4j
@Service
public class SystemConfigService {

    @Autowired
    private SystemConfigCacheService cacheService;

    /**
     * 获取配置值（字符串类型）
     *
     * @param configKey 配置键
     * @param defaultValue 默认值（配置不存在时返回）
     * @return 配置值
     */
    public String getString(String configKey, String defaultValue) {
        try {
            String value = cacheService.getConfigValue(configKey);
            if (value != null) {
                return value;
            }
            
            // 配置不存在，返回默认值
            log.debug("配置项不存在，使用默认值，key={}，defaultValue={}", configKey, defaultValue);
            return defaultValue;

        } catch (Exception e) {
            log.error("获取系统配置失败，key={}，将使用默认值", configKey, e);
            return defaultValue;
        }
    }

    /**
     * 获取配置值（整数类型）
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Integer getInt(String configKey, Integer defaultValue) {
        String value = getString(configKey, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置值转换为整数失败，key={}，value={}，使用默认值", configKey, value);
            return defaultValue;
        }
    }

    /**
     * 获取配置值（长整数类型）
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Long getLong(String configKey, Long defaultValue) {
        String value = getString(configKey, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.warn("配置值转换为长整数失败，key={}，value={}，使用默认值", configKey, value);
            return defaultValue;
        }
    }

    /**
     * 获取配置值（双精度浮点数类型）
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Double getDouble(String configKey, Double defaultValue) {
        String value = getString(configKey, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("配置值转换为浮点数失败，key={}，value={}，使用默认值", configKey, value);
            return defaultValue;
        }
    }

    /**
     * 获取配置值（布尔类型）
     *
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Boolean getBoolean(String configKey, Boolean defaultValue) {
        String value = getString(configKey, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    /**
     * 刷新缓存
     */
    public void refreshCache() {
        cacheService.warmUpCache();
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        cacheService.evictAllConfigs();
    }
    
    /**
     * 失效指定配置的缓存
     */
    public void evictConfig(String configKey) {
        cacheService.evictConfig(configKey);
    }

    // ==================== 便捷方法：获取常用配置 ====================

    /**
     * 获取默认AI模型（完整格式：provider:model）
     */
    public String getDefaultAiModel() {
        return getString(SystemConfigKey.AI_MODEL_DEFAULT, "openai:gpt-oss-20b");
    }

    /**
     * 获取默认AI模型厂商
     */
    public String getDefaultAiModelProvider() {
        return getString(SystemConfigKey.AI_MODEL_DEFAULT_PROVIDER, "openai");
    }

    /**
     * 获取默认AI模型代码
     */
    public String getDefaultAiModelCode() {
        return getString(SystemConfigKey.AI_MODEL_DEFAULT_CODE, "gpt-oss-20b");
    }

    /**
     * 获取默认温度参数
     */
    public Double getDefaultTemperature() {
        return getDouble(SystemConfigKey.AI_TEMPERATURE_DEFAULT, 0.7);
    }

    /**
     * 获取默认最大令牌数
     */
    public Integer getDefaultMaxTokens() {
        return getInt(SystemConfigKey.AI_MAX_TOKENS_DEFAULT, 2000);
    }

    /**
     * 获取默认系统提示词
     */
    public String getDefaultSystemPrompt() {
        return getString(SystemConfigKey.AI_SYSTEM_PROMPT_DEFAULT, "你是一个有用的AI助手");
    }

    /**
     * 获取默认对话标题
     */
    public String getDefaultConversationTitle() {
        return getString(SystemConfigKey.CONVERSATION_TITLE_DEFAULT, "新对话");
    }

    /**
     * 获取对话历史最大条数
     */
    public Integer getConversationMaxHistory() {
        return getInt(SystemConfigKey.CONVERSATION_MAX_HISTORY, 20);
    }

    /**
     * 获取单个对话最大消息数
     */
    public Integer getConversationMaxMessages() {
        return getInt(SystemConfigKey.CONVERSATION_MAX_MESSAGES, 100);
    }

    /**
     * 获取单个对话最大令牌数
     */
    public Integer getConversationMaxTokens() {
        return getInt(SystemConfigKey.CONVERSATION_MAX_TOKENS, 50000);
    }

    /**
     * 获取默认TTS语音类型
     */
    public String getDefaultTtsVoice() {
        return getString(SystemConfigKey.TTS_VOICE_DEFAULT, "qiniu_zh_female_wwxkjx");
    }

    /**
     * 获取默认语速
     */
    public Double getDefaultTtsSpeed() {
        return getDouble(SystemConfigKey.TTS_SPEED_DEFAULT, 1.0);
    }

    /**
     * 获取默认音频编码
     */
    public String getDefaultTtsEncoding() {
        return getString(SystemConfigKey.TTS_ENCODING_DEFAULT, "mp3");
    }

    /**
     * 判断搜索功能是否启用
     */
    public Boolean isSearchEnabled() {
        return getBoolean(SystemConfigKey.SEARCH_ENABLED, true);
    }

    /**
     * 获取搜索提供商
     */
    public String getSearchProvider() {
        return getString(SystemConfigKey.SEARCH_PROVIDER, "duckduckgo");
    }
}

package com.nexusvoice.domain.config.service;

import com.nexusvoice.domain.config.model.SystemConfig;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 系统配置领域服务
 * 纯净的业务服务，不包含任何技术实现细节（如缓存、Redis等）
 * 所有技术相关的操作通过仓储接口委托给infrastructure层
 * 
 * @author NexusVoice
 * @since 2025-10-21
 */
@Slf4j
@Service
public class SystemConfigDomainService {

    private final SystemConfigRepository repository;

    /**
     * 构造器注入
     */
    public SystemConfigDomainService(SystemConfigRepository repository) {
        this.repository = repository;
    }

    /**
     * 获取配置值
     * 缓存逻辑在infrastructure层的仓储实现中处理
     * 
     * @param configKey 配置键
     * @return 配置值，不存在返回null
     */
    public String getConfigValue(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            return null;
        }
        
        // 通过仓储接口获取，缓存逻辑在仓储实现中
        Optional<SystemConfig> config = repository.findByKey(configKey);
        
        if (config.isPresent() && config.get().isActive()) {
            return config.get().getConfigValue();
        }
        
        return null;
    }

    /**
     * 获取配置对象
     * 
     * @param configKey 配置键
     * @return 配置对象
     */
    public Optional<SystemConfig> getConfig(String configKey) {
        if (configKey == null || configKey.trim().isEmpty()) {
            return Optional.empty();
        }
        
        Optional<SystemConfig> config = repository.findByKey(configKey);
        
        // 只返回启用的配置
        if (config.isPresent() && config.get().isActive()) {
            return config;
        }
        
        return Optional.empty();
    }

    /**
     * 保存配置
     * 
     * @param config 配置对象
     * @return 保存后的配置对象
     */
    public SystemConfig saveConfig(SystemConfig config) {
        // 验证配置的业务规则
        validateConfig(config);
        
        // 设置审计字段（业务逻辑）
        if (config.isNew()) {
            config.onCreate();
        } else {
            config.onUpdate();
        }
        
        // 保存配置
        SystemConfig saved = repository.save(config);
        
        // 刷新缓存（通过仓储接口，隐藏技术细节）
        repository.refreshCache(saved.getConfigKey());
        
        log.info("保存配置成功，配置键：{}", saved.getConfigKey());
        
        return saved;
    }

    /**
     * 删除配置
     * 
     * @param configKey 配置键
     */
    public void deleteConfig(String configKey) {
        Optional<SystemConfig> config = repository.findByKey(configKey);
        
        if (config.isPresent()) {
            // 检查是否可以删除
            if (config.get().getReadonly()) {
                throw new IllegalStateException("只读配置不能删除：" + configKey);
            }
            
            // 删除配置
            repository.deleteByKey(configKey);
            
            // 刷新缓存
            repository.refreshCache(configKey);
            
            log.info("删除配置成功，配置键：{}", configKey);
        }
    }

    /**
     * 启用配置
     * 
     * @param configKey 配置键
     * @return 是否成功
     */
    public boolean enableConfig(String configKey) {
        Optional<SystemConfig> config = repository.findByKey(configKey);
        
        if (config.isPresent()) {
            SystemConfig cfg = config.get();
            cfg.setEnabled(true);
            cfg.onUpdate();
            
            repository.save(cfg);
            repository.refreshCache(configKey);
            
            log.info("启用配置成功，配置键：{}", configKey);
            return true;
        }
        
        return false;
    }

    /**
     * 禁用配置
     * 
     * @param configKey 配置键
     * @return 是否成功
     */
    public boolean disableConfig(String configKey) {
        Optional<SystemConfig> config = repository.findByKey(configKey);
        
        if (config.isPresent()) {
            SystemConfig cfg = config.get();
            
            // 检查是否可以禁用
            if (cfg.getReadonly()) {
                throw new IllegalStateException("只读配置不能禁用：" + configKey);
            }
            
            cfg.setEnabled(false);
            cfg.onUpdate();
            
            repository.save(cfg);
            repository.refreshCache(configKey);
            
            log.info("禁用配置成功，配置键：{}", configKey);
            return true;
        }
        
        return false;
    }

    /**
     * 验证配置的业务规则
     * 
     * @param config 配置对象
     */
    private void validateConfig(SystemConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("配置对象不能为空");
        }
        
        if (!config.isValidKey()) {
            throw new IllegalArgumentException("配置键无效：" + config.getConfigKey());
        }
        
        if (config.getConfigValue() == null) {
            throw new IllegalArgumentException("配置值不能为空");
        }
        
        // 只读配置的值不能修改
        if (!config.isNew() && config.getReadonly()) {
            Optional<SystemConfig> existing = repository.findByKey(config.getConfigKey());
            if (existing.isPresent() && !existing.get().getConfigValue().equals(config.getConfigValue())) {
                throw new IllegalStateException("只读配置的值不能修改：" + config.getConfigKey());
            }
        }
    }

    // ============ 便捷方法（业务常用的配置获取） ============

    /**
     * 获取默认对话标题
     */
    public String getDefaultConversationTitle() {
        String title = getConfigValue("conversation.default.title");
        return title != null ? title : "新对话";
    }

    /**
     * 获取默认AI模型
     */
    public String getDefaultAiModel() {
        String model = getConfigValue("ai.model.default");
        return model != null ? model : "openai:gpt-oss-20b";
    }

    /**
     * 获取默认AI模型提供商
     */
    public String getDefaultAiModelProvider() {
        String provider = getConfigValue("ai.model.default.provider");
        return provider != null ? provider : "openai";
    }

    /**
     * 获取默认系统提示词
     */
    public String getDefaultSystemPrompt() {
        String prompt = getConfigValue("ai.system.prompt.default");
        return prompt != null ? prompt : "You are a helpful assistant.";
    }

    /**
     * 获取对话最大消息数
     */
    public int getConversationMaxMessages() {
        String value = getConfigValue("conversation.max.messages");
        try {
            return value != null ? Integer.parseInt(value) : 100;
        } catch (NumberFormatException e) {
            log.warn("配置值转换失败，使用默认值，配置键：conversation.max.messages，值：{}", value);
            return 100;
        }
    }

    /**
     * 获取对话最大令牌数
     */
    public int getConversationMaxTokens() {
        String value = getConfigValue("conversation.max.tokens");
        try {
            return value != null ? Integer.parseInt(value) : 50000;
        } catch (NumberFormatException e) {
            log.warn("配置值转换失败，使用默认值，配置键：conversation.max.tokens，值：{}", value);
            return 50000;
        }
    }

    /**
     * 获取默认TTS语音类型
     */
    public String getDefaultTtsVoice() {
        String voice = getConfigValue("tts.default.voice");
        return voice != null ? voice : "qiniu_zh_female_wwxkjx";
    }
}

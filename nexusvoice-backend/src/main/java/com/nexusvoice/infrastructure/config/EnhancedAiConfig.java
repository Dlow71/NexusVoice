package com.nexusvoice.infrastructure.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 增强AI服务配置
 *
 * @author NexusVoice
 * @since 2025-09-27
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "nexusvoice.ai")
public class EnhancedAiConfig {
    
    /**
     * 工具调用配置
     */
    private Tools tools = new Tools();
    
    /**
     * 模型配置
     */
    private Model model = new Model();
    
    @Data
    public static class Tools {
        /**
         * 是否启用工具调用功能
         */
        private boolean enabled = true;
        
        /**
         * 搜索工具配置
         */
        private Search search = new Search();
        
        @Data
        public static class Search {
            /**
             * 是否启用搜索工具
             */
            private boolean enabled = true;
            
            /**
             * 搜索结果最大数量
             */
            private int maxResults = 5;
            
            /**
             * 搜索超时时间（秒）
             */
            private int timeoutSeconds = 30;
        }
    }
    
    @Data
    public static class Model {
        /**
         * 基础模型名称
         */
        private String name = "gpt-4o-mini";
        
        /**
         * 增强模型名称（启用工具时）
         */
        private String enhancedName = "gpt-4o-mini-enhanced";
        
        /**
         * 温度参数
         */
        private double temperature = 0.7;
        
        /**
         * 最大tokens
         */
        private int maxTokens = 4096;
    }
}

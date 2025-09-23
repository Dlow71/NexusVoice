package com.nexusvoice.infrastructure.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus拦截器配置
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件 - 暂时注释掉，等依赖问题解决后再启用
        // PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        // paginationInnerInterceptor.setOverflow(false);
        // paginationInnerInterceptor.setMaxLimit(500L);
        // interceptor.addInnerInterceptor(paginationInnerInterceptor);
        
        return interceptor;
    }
}

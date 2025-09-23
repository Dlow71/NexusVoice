package com.nexusvoice.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus配置类
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Configuration
@MapperScan("com.nexusvoice.infrastructure.database.mapper")
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus拦截器配置
     * 包含分页插件等
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置请求的页面大于最大页后操作，true调回到首页，false继续请求。默认false
        paginationInnerInterceptor.setOverflow(false);
        // 单页分页条数限制，默认无限制
        paginationInnerInterceptor.setMaxLimit(500L);
        
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        
        return interceptor;
    }
}

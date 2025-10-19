package com.nexusvoice.infrastructure.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

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
     * 包含分页插件、乐观锁插件等
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 乐观锁插件（重要：需要先添加）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        // 分页插件 - 使用PostgreSQL方言
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        // 设置请求的页面大于最大页后操作，true调回到首页，false继续请求。默认false
        paginationInnerInterceptor.setOverflow(false);
        // 单页分页条数限制，默认无限制
        paginationInnerInterceptor.setMaxLimit(500L);
        
        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        
        return interceptor;
    }
    
    /**
     * MyBatis配置自定义器
     * 解决PostgreSQL的TIMESTAMPTZ类型到LocalDateTime的转换问题
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // 注册自定义的LocalDateTime TypeHandler
            TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
            
            // 使用自定义TypeHandler处理LocalDateTime，兼容PostgreSQL的TIMESTAMPTZ
            registry.register(LocalDateTime.class, JdbcType.TIMESTAMP, new org.apache.ibatis.type.BaseTypeHandler<LocalDateTime>() {
                @Override
                public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) 
                        throws SQLException {
                    ps.setTimestamp(i, Timestamp.valueOf(parameter));
                }

                @Override
                public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
                    return toLocalDateTime(rs.getTimestamp(columnName));
                }

                @Override
                public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
                    return toLocalDateTime(rs.getTimestamp(columnIndex));
                }

                @Override
                public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
                    return toLocalDateTime(cs.getTimestamp(columnIndex));
                }

                /**
                 * 将Timestamp转换为LocalDateTime
                 * 兼容PostgreSQL的TIMESTAMPTZ类型
                 */
                private LocalDateTime toLocalDateTime(Timestamp timestamp) {
                    if (timestamp == null) {
                        return null;
                    }
                    try {
                        // 直接转换
                        return timestamp.toLocalDateTime();
                    } catch (Exception e) {
                        // 如果失败，通过Instant转换（处理带时区的情况）
                        return timestamp.toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime();
                    }
                }
            });
        };
    }
}

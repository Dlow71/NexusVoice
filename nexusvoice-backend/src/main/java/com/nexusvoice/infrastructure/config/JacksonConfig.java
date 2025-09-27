package com.nexusvoice.infrastructure.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson序列化配置
 * 解决Long类型精度丢失问题和时间序列化问题
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Configuration
public class JacksonConfig {

    /**
     * 自定义Long类型序列化器
     * 将Long类型转换为字符串，避免JavaScript精度丢失
     */
    public static class LongToStringSerializer extends JsonSerializer<Long> {
        @Override
        public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(value.toString());
            } else {
                gen.writeNull();
            }
        }
    }

    /**
     * 自定义BigInteger类型序列化器
     * 将BigInteger类型转换为字符串
     */
    public static class BigIntegerToStringSerializer extends JsonSerializer<BigInteger> {
        @Override
        public void serialize(BigInteger value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(value.toString());
            } else {
                gen.writeNull();
            }
        }
    }

    /**
     * 配置Jackson ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        
        // 创建自定义模块
        SimpleModule customModule = new SimpleModule("CustomSerializerModule");
        
        // 注册Long类型序列化器 - 转换为字符串避免精度丢失
        customModule.addSerializer(Long.class, new LongToStringSerializer());
        customModule.addSerializer(Long.TYPE, new LongToStringSerializer());
        
        // 注册BigInteger类型序列化器
        customModule.addSerializer(BigInteger.class, new BigIntegerToStringSerializer());
        
        // 注册模块
        objectMapper.registerModule(customModule);
        
        // 配置时间序列化
        configureTimeModule(objectMapper);
        
        return objectMapper;
    }

    /**
     * 配置时间模块
     */
    private void configureTimeModule(ObjectMapper objectMapper) {
        JavaTimeModule timeModule = new JavaTimeModule();
        
        // 配置LocalDateTime的序列化和反序列化格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
        
        objectMapper.registerModule(timeModule);
        
        // 禁用将日期写为时间戳
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 配置Jackson2ObjectMapperBuilder
     */
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        return new Jackson2ObjectMapperBuilder()
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .serializers(new ToStringSerializer(Long.class))
                .serializers(new ToStringSerializer(Long.TYPE))
                .serializers(new ToStringSerializer(BigInteger.class));
    }
}

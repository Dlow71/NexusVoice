package com.nexusvoice.interfaces.api.test;

import com.nexusvoice.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Jackson序列化测试控制器
 * 用于测试Long类型和时间类型的序列化配置
 * 
 * @author NexusVoice
 * @since 2025-09-27
 */
@Tag(name = "Jackson测试", description = "测试Jackson序列化配置")
@RestController
@RequestMapping("/api/v1/test/jackson")
public class JacksonTestController {

    /**
     * 测试Long类型序列化
     * 验证Long类型是否正确序列化为字符串
     */
    @Operation(summary = "测试Long类型序列化", description = "返回包含各种Long类型数据的响应，验证是否正确序列化为字符串")
    @GetMapping("/long")
    public Result<Map<String, Object>> testLongSerialization() {
        Map<String, Object> data = new HashMap<>();
        
        // 测试各种Long值
        data.put("snowflakeId", 1844747947267072000L); // 雪花ID
        data.put("userId", 123456789012345678L); // 用户ID
        data.put("timestamp", System.currentTimeMillis()); // 时间戳
        data.put("maxLong", Long.MAX_VALUE); // 最大Long值
        data.put("minLong", Long.MIN_VALUE); // 最小Long值
        data.put("nullLong", (Long) null); // null值
        
        // 测试BigInteger
        data.put("bigInteger", new BigInteger("123456789012345678901234567890"));
        
        return Result.success(data);
    }

    /**
     * 测试时间类型序列化
     * 验证LocalDateTime是否正确序列化为指定格式
     */
    @Operation(summary = "测试时间类型序列化", description = "返回包含时间数据的响应，验证时间格式")
    @GetMapping("/datetime")
    public Result<Map<String, Object>> testDateTimeSerialization() {
        Map<String, Object> data = new HashMap<>();
        
        data.put("currentTime", LocalDateTime.now());
        data.put("specificTime", LocalDateTime.of(2025, 9, 27, 21, 30, 0));
        data.put("nullTime", (LocalDateTime) null);
        
        return Result.success(data);
    }

    /**
     * 测试复合数据类型
     * 验证包含Long和时间的复杂对象序列化
     */
    @Operation(summary = "测试复合数据类型", description = "返回包含Long和时间的复杂对象")
    @GetMapping("/complex")
    public Result<TestDataObject> testComplexSerialization() {
        TestDataObject data = new TestDataObject();
        data.setId(1844747947267072000L);
        data.setUserId(123456789012345678L);
        data.setName("测试对象");
        data.setCreatedAt(LocalDateTime.now());
        data.setUpdatedAt(LocalDateTime.of(2025, 9, 27, 21, 30, 0));
        data.setVersion(1L);
        data.setBigNumber(new BigInteger("999999999999999999999999999999"));
        
        return Result.success(data);
    }

    /**
     * 测试数据对象
     */
    public static class TestDataObject {
        private Long id;
        private Long userId;
        private String name;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long version;
        private BigInteger bigNumber;

        // Getter and Setter methods
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        public BigInteger getBigNumber() {
            return bigNumber;
        }

        public void setBigNumber(BigInteger bigNumber) {
            this.bigNumber = bigNumber;
        }
    }
}

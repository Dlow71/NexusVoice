# Jackson 全局序列化配置指南

## 概述

为了解决前端JavaScript精度丢失问题，我们配置了全局的Jackson序列化器，自动将所有Long类型转换为字符串返回给前端。

## 解决的问题

### 1. JavaScript精度丢失问题
JavaScript的Number类型最大安全整数是 `2^53 - 1` (即 `9007199254740991`)，而Java的Long类型范围是 `-2^63` 到 `2^63 - 1`。当Long值超过JavaScript的安全范围时，会出现精度丢失。

**示例问题**：
```javascript
// Java Long值: 1844747947267072000
// JavaScript接收到: 1844747947267072000 (可能变成 1844747947267072100)
```

### 2. 手动注解的繁琐性
之前需要在每个Long字段上添加注解：
```java
@JsonSerialize(using = ToStringSerializer.class)
private Long userId;
```

## 解决方案

### 1. 全局Jackson配置
创建了 `JacksonConfig` 配置类，自动处理所有Long类型的序列化：

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        
        SimpleModule customModule = new SimpleModule("CustomSerializerModule");
        
        // 自动将所有Long类型序列化为字符串
        customModule.addSerializer(Long.class, new LongToStringSerializer());
        customModule.addSerializer(Long.TYPE, new LongToStringSerializer());
        customModule.addSerializer(BigInteger.class, new BigIntegerToStringSerializer());
        
        objectMapper.registerModule(customModule);
        return objectMapper;
    }
}
```

### 2. 自动处理的类型
- `Long` 和 `long` → 字符串
- `BigInteger` → 字符串
- `LocalDateTime` → `yyyy-MM-dd HH:mm:ss` 格式

## 使用效果

### 配置前
```json
{
  "code": 200,
  "data": {
    "id": 1844747947267072000,  // 可能精度丢失
    "userId": 123456789012345678,
    "createdAt": "2025-09-27T21:30:00"
  }
}
```

### 配置后
```json
{
  "code": 200,
  "data": {
    "id": "1844747947267072000",  // 字符串，无精度丢失
    "userId": "123456789012345678",
    "createdAt": "2025-09-27 21:30:00"
  }
}
```

## 测试验证

### 1. 测试接口
我们提供了测试接口来验证配置是否正常工作：

```bash
# 测试Long类型序列化
GET /api/v1/test/jackson/long

# 测试时间类型序列化
GET /api/v1/test/jackson/datetime

# 测试复合数据类型
GET /api/v1/test/jackson/complex
```

### 2. 预期结果
所有Long类型字段都应该以字符串形式返回，时间格式为 `yyyy-MM-dd HH:mm:ss`。

## 前端处理建议

### 1. JavaScript处理
```javascript
// 接收Long类型数据
const response = await fetch('/api/v1/system/config/1');
const data = await response.json();

// ID现在是字符串，可以安全使用
const configId = data.data.id; // "1844747947267072000"

// 如果需要数值计算，使用BigInt
const idAsBigInt = BigInt(data.data.id);
```

### 2. TypeScript类型定义
```typescript
interface SystemConfig {
  id: string;           // Long类型在前端用string接收
  userId: string;       // Long类型在前端用string接收
  configKey: string;
  configValue: string;
  createdAt: string;    // 时间格式: "yyyy-MM-dd HH:mm:ss"
  updatedAt: string;
}
```

## 注意事项

### 1. 兼容性
- 配置对所有Controller返回的JSON响应生效
- 不影响接收请求时的反序列化
- 与现有代码完全兼容

### 2. 性能影响
- 序列化性能影响微乎其微
- 字符串传输略微增加网络负载
- 前端处理字符串比处理大数值更安全

### 3. 数据库操作
- 数据库中仍然存储Long类型
- MyBatis-Plus等ORM框架不受影响
- 只影响HTTP响应的JSON序列化

## 移除旧注解

配置生效后，可以移除所有手动添加的序列化注解：

```java
// 删除这些注解
@JsonSerialize(using = ToStringSerializer.class)
private Long userId;

// 简化为
private Long userId;
```

## 故障排除

### 1. 配置不生效
检查 `JacksonConfig` 是否被Spring扫描到：
```java
@Configuration  // 确保有此注解
@ComponentScan  // 确保包路径正确
```

### 2. 部分字段仍是数值
检查是否有局部的Jackson配置覆盖了全局配置。

### 3. 时间格式问题
确保 `JavaTimeModule` 正确注册并配置了格式化器。

## 总结

通过全局Jackson配置，我们实现了：
- ✅ 自动将所有Long类型序列化为字符串
- ✅ 解决JavaScript精度丢失问题
- ✅ 无需手动添加注解
- ✅ 统一的时间格式化
- ✅ 向后兼容现有代码

这是一个一劳永逸的解决方案，新增的Long类型字段会自动应用此配置。

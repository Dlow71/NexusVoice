# SystemConfig API 调用文档

## 概述

SystemConfig API 提供了系统配置管理的完整功能，支持配置的增删改查、分页查询、批量操作等。所有接口都遵循 RESTful 设计规范。

## 基础信息

- **Base URL**: `http://localhost:8081/api/v1/system/config`
- **Content-Type**: `application/json`
- **认证方式**: JWT Token (在 Authorization 头中传递)

## API 接口列表

### 1. 创建系统配置

**接口地址**: `POST /api/v1/system/config`

**请求参数**:
```json
{
  "configKey": "ai.model.default",
  "configValue": "gpt-4",
  "description": "默认AI模型",
  "configGroup": "ai",
  "enabled": true,
  "readonly": false,
  "sortOrder": 10,
  "remark": "OpenAI默认模型"
}
```

**参数说明**:
| 参数名 | 类型 | 必填 | 说明 | 示例 |
|--------|------|------|------|------|
| configKey | String | 是 | 配置键，最大100字符 | "ai.model.default" |
| configValue | String | 是 | 配置值，最大1000字符 | "gpt-4" |
| description | String | 是 | 配置描述，最大200字符 | "默认AI模型" |
| configGroup | String | 否 | 配置分组，最大50字符 | "ai" |
| enabled | Boolean | 否 | 是否启用，默认true | true |
| readonly | Boolean | 否 | 是否只读，默认false | false |
| sortOrder | Integer | 否 | 排序，默认0 | 10 |
| remark | String | 否 | 备注，最大500字符 | "OpenAI默认模型" |

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "configKey": "ai.model.default",
    "configValue": "gpt-4",
    "description": "默认AI模型",
    "configGroup": "ai",
    "enabled": true,
    "readonly": false,
    "sortOrder": 10,
    "remark": "OpenAI默认模型",
    "createdAt": "2025-09-27T18:51:00",
    "updatedAt": "2025-09-27T18:51:00"
  }
}
```

### 2. 更新系统配置

**接口地址**: `PUT /api/v1/system/config`

**请求参数**:
```json
{
  "id": 1001,
  "configKey": "ai.model.default",
  "configValue": "gpt-4-turbo",
  "description": "默认AI模型",
  "configGroup": "ai",
  "enabled": true,
  "readonly": false,
  "sortOrder": 10,
  "remark": "OpenAI最新模型"
}
```

**参数说明**: 与创建接口相同，但需要包含 `id` 字段

**响应示例**: 与创建接口响应格式相同

### 3. 删除系统配置

**接口地址**: `DELETE /api/v1/system/config/{id}`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 配置ID |

**请求示例**: `DELETE /api/v1/system/config/1001`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

### 4. 根据ID查询系统配置

**接口地址**: `GET /api/v1/system/config/{id}`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 配置ID |

**请求示例**: `GET /api/v1/system/config/1001`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1001,
    "configKey": "ai.model.default",
    "configValue": "gpt-4",
    "description": "默认AI模型",
    "configGroup": "ai",
    "enabled": true,
    "readonly": false,
    "sortOrder": 10,
    "remark": "OpenAI默认模型",
    "createdAt": "2025-09-27T18:51:00",
    "updatedAt": "2025-09-27T18:51:00"
  }
}
```

### 5. 根据配置键查询系统配置

**接口地址**: `GET /api/v1/system/config/key/{configKey}`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| configKey | String | 是 | 配置键 |

**请求示例**: `GET /api/v1/system/config/key/ai.model.default`

**响应示例**: 与根据ID查询相同

### 6. 分页查询系统配置

**接口地址**: `GET /api/v1/system/config/page`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 | 默认值 |
|--------|------|------|------|--------|
| configKey | String | 否 | 配置键（模糊查询） | - |
| configGroup | String | 否 | 配置分组 | - |
| enabled | Boolean | 否 | 是否启用 | - |
| page | Integer | 否 | 页码 | 1 |
| size | Integer | 否 | 每页大小 | 10 |

**请求示例**: `GET /api/v1/system/config/page?configGroup=ai&enabled=true&page=1&size=10`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [
      {
        "id": 4,
        "configKey": "ai.model.default",
        "configValue": "gpt-3.5-turbo",
        "description": "默认AI模型",
        "configGroup": "ai",
        "enabled": true,
        "readonly": false,
        "sortOrder": 10,
        "remark": "OpenAI默认模型",
        "createdAt": "2025-09-27T18:51:00",
        "updatedAt": "2025-09-27T18:51:00"
      }
    ],
    "total": 3,
    "page": 1,
    "size": 10,
    "pages": 1
  }
}
```

### 7. 根据分组查询配置列表

**接口地址**: `GET /api/v1/system/config/group/{configGroup}`

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| configGroup | String | 是 | 配置分组 |

**请求示例**: `GET /api/v1/system/config/group/ai`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 4,
      "configKey": "ai.model.default",
      "configValue": "gpt-3.5-turbo",
      "description": "默认AI模型",
      "configGroup": "ai",
      "enabled": true,
      "readonly": false,
      "sortOrder": 10,
      "remark": "OpenAI默认模型",
      "createdAt": "2025-09-27T18:51:00",
      "updatedAt": "2025-09-27T18:51:00"
    }
  ]
}
```

### 8. 查询所有启用的配置

**接口地址**: `GET /api/v1/system/config/enabled`

**请求示例**: `GET /api/v1/system/config/enabled`

**响应示例**: 返回所有启用状态的配置列表，格式与分组查询相同

### 9. 批量更新配置状态

**接口地址**: `PUT /api/v1/system/config/batch/status`

**查询参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | List<Long> | 是 | 配置ID列表 |
| enabled | Boolean | 是 | 是否启用 |

**请求示例**: `PUT /api/v1/system/config/batch/status?ids=1,2,3&enabled=false`

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

## 默认配置数据

系统预置了以下配置分组和配置项：

### system 分组
- `system.name`: 系统名称
- `system.version`: 系统版本（只读）
- `system.description`: 系统描述

### ai 分组
- `ai.model.default`: 默认AI模型
- `ai.temperature`: AI温度参数
- `ai.max_tokens`: AI最大令牌数

### conversation 分组
- `conversation.max_history`: 对话历史最大条数
- `conversation.timeout`: 对话超时时间（秒）

### file 分组
- `file.upload.max_size`: 文件上传最大大小（字节）
- `file.upload.allowed_types`: 允许上传的文件类型

### security 分组
- `security.jwt.expire_time`: JWT过期时间（秒）
- `security.password.min_length`: 密码最小长度

### cache 分组
- `cache.redis.expire_time`: Redis缓存过期时间（秒）

### search 分组
- `search.enabled`: 是否启用搜索功能
- `search.provider`: 搜索提供商

## 错误码说明

| 错误码 | 说明 |
|--------|------|
| 1801 | 配置不存在 |
| 1802 | 配置无效 |
| 1803 | 配置更新失败 |
| 1804 | 配置已存在 |
| 1805 | 配置创建失败 |
| 1806 | 配置删除失败 |
| 1807 | 配置为只读 |

## 使用示例

### cURL 示例

```bash
# 创建配置
curl -X POST http://localhost:8081/api/v1/system/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "configKey": "test.config",
    "configValue": "test_value",
    "description": "测试配置",
    "configGroup": "test"
  }'

# 查询配置
curl -X GET http://localhost:8081/api/v1/system/config/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"

# 分页查询
curl -X GET "http://localhost:8081/api/v1/system/config/page?configGroup=ai&page=1&size=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### JavaScript 示例

```javascript
// 创建配置
const createConfig = async () => {
  const response = await fetch('/api/v1/system/config', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      configKey: 'test.config',
      configValue: 'test_value',
      description: '测试配置',
      configGroup: 'test'
    })
  });
  return await response.json();
};

// 查询配置
const getConfig = async (id) => {
  const response = await fetch(`/api/v1/system/config/${id}`, {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  return await response.json();
};
```

## 注意事项

1. **权限控制**: 所有接口都需要有效的JWT Token
2. **只读配置**: 标记为只读的配置不能被修改或删除
3. **配置键唯一性**: 配置键在系统中必须唯一
4. **数据验证**: 请求参数会进行严格的数据验证
5. **事务保证**: 关键操作使用数据库事务保证数据一致性
6. **日志记录**: 所有操作都会记录详细的日志信息

## 联系方式

如有问题或建议，请联系开发团队。

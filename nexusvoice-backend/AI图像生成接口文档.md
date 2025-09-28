# AI图像生成接口文档

## 📋 概述

NexusVoice AI图像生成服务基于硅基流动API，支持多种先进的图像生成模型。本服务可以根据文本描述生成高质量的图像，并自动上传到七牛云CDN提供永久访问链接。

### 🎯 核心特性

- **多模型支持**：Qwen系列、Kolors等4种专业模型
- **智能参数**：根据模型自动推荐最佳参数
- **批量生成**：支持一次生成多张图像
- **CDN集成**：自动上传到七牛云，返回永久URL
- **完整验证**：全面的参数验证和错误处理

### 🌐 基础信息

- **Base URL**: `http://localhost:8080/api/v1/image`
- **请求格式**: `application/json`
- **响应格式**: `application/json`

---

## 🎨 接口列表

### 1. 图像生成接口

#### `POST /generate`

根据提示词生成单张或多张图像。

##### 请求参数

| 参数名 | 类型 | 必填 | 描述 | 示例值 |
|--------|------|------|------|--------|
| model | String | 是 | 图像生成模型 | `"Qwen/Qwen-Image"` |
| prompt | String | 是 | 图像描述提示词 | `"一只可爱的小猫"` |
| negativePrompt | String | 否 | 负向提示词 | `"模糊、低质量"` |
| imageSize | String | 否 | 图像尺寸 | `"1024x1024"` |
| batchSize | Integer | 否 | 批量大小(1-4) | `1` |
| seed | Long | 否 | 随机种子 | `12345` |
| numInferenceSteps | Integer | 否 | 推理步数(1-100) | `20` |
| guidanceScale | Double | 否 | 引导比例(Kolors专用) | `7.5` |
| cfg | Double | 否 | CFG参数(Qwen专用) | `4.0` |

##### 支持的模型

| 模型名称 | 描述 | 支持参数 | 不支持参数 |
|----------|------|----------|-----------|
| `Qwen/Qwen-Image-Edit-2509` | Qwen图像编辑模型2509 | cfg, 多图像输入 | guidanceScale |
| `Qwen/Qwen-Image-Edit` | Qwen图像编辑模型 | cfg | guidanceScale |
| `Qwen/Qwen-Image` | Qwen图像生成模型 | cfg | guidanceScale |
| `Kwai-Kolors/Kolors` | Kolors图像生成模型 | guidanceScale, batchSize | cfg |

##### 支持的尺寸

**Kolors模型：**
- `1024x1024` (推荐)
- `960x1280`
- `768x1024`
- `720x1440`
- `720x1280`

**Qwen模型：**
- `1328x1328` (推荐)
- `1664x928`
- `928x1664`
- `1472x1140`
- `1140x1472`
- `1584x1056`
- `1056x1584`

##### 请求示例

```json
{
  "model": "Qwen/Qwen-Image",
  "prompt": "一只橘猫坐在阳光明媚的窗台上，背景是绿色的植物，高质量，详细，4K",
  "negativePrompt": "模糊、低质量、变形、噪点",
  "imageSize": "1328x1328",
  "numInferenceSteps": 20,
  "cfg": 4.0,
  "seed": 42
}
```

##### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "imageUrls": [
      "https://cdn.nexusvoice.com/images/generated_image_1695552000000.png"
    ],
    "prompt": "一只橘猫坐在阳光明媚的窗台上，背景是绿色的植物，高质量，详细，4K",
    "negativePrompt": "模糊、低质量、变形、噪点",
    "model": "Qwen/Qwen-Image",
    "imageSize": "1328x1328",
    "imageCount": 1,
    "usedSeed": 42,
    "numInferenceSteps": 20,
    "cfg": 4.0,
    "generationTime": 3500,
    "imageInfos": [
      {
        "url": "https://cdn.nexusvoice.com/images/generated_image_1695552000000.png",
        "fileName": "generated_image_1695552000000.png",
        "width": 1328,
        "height": 1328,
        "format": "PNG",
        "index": 0
      }
    ]
  }
}
```

---

### 2. 批量生成接口

#### `POST /generate/batch`

批量生成多张图像，主要用于Kolors模型。

##### 请求参数

与单张生成接口相同，但`batchSize`参数更重要：

```json
{
  "model": "Kwai-Kolors/Kolors",
  "prompt": "美丽的风景画，山川河流，油画风格",
  "imageSize": "1024x1024",
  "batchSize": 3,
  "guidanceScale": 7.5
  // 注意：Kolors模型不支持cfg参数，系统会自动忽略
}
```

##### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "imageUrls": [
      "https://cdn.nexusvoice.com/images/batch_1_1695552000000.png",
      "https://cdn.nexusvoice.com/images/batch_2_1695552000000.png",
      "https://cdn.nexusvoice.com/images/batch_3_1695552000000.png"
    ],
    "imageCount": 3,
    "generationTime": 8500
  }
}
```

---

### 3. 获取支持模型

#### `GET /models`

获取当前支持的所有图像生成模型列表。

##### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    "Qwen/Qwen-Image-Edit-2509",
    "Qwen/Qwen-Image-Edit",
    "Qwen/Qwen-Image",
    "Kwai-Kolors/Kolors"
  ]
}
```

---

### 4. 获取模型推荐参数

#### `GET /models/{modelName}/recommended-params`

获取指定模型的推荐参数配置。

##### 路径参数

| 参数名 | 类型 | 描述 | 示例 |
|--------|------|------|------|
| modelName | String | 模型名称 | `Qwen/Qwen-Image` |

##### 请求示例

```
GET /models/Qwen%2FQwen-Image/recommended-params
```

##### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "model": "Qwen/Qwen-Image",
    "imageSize": "1328x1328",
    "numInferenceSteps": 20,
    "cfg": 4.0,
    "batchSize": 1
  }
}
```

---

### 5. 健康检查

#### `GET /health`

检查图像生成服务的健康状态和可用性。

##### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "timestamp": 1695552000000,
    "status": "UP",
    "message": "服务正常",
    "supportedModels": [
      "Qwen/Qwen-Image-Edit-2509",
      "Qwen/Qwen-Image-Edit",
      "Qwen/Qwen-Image",
      "Kwai-Kolors/Kolors"
    ],
    "modelCount": 4
  }
}
```

---

### 6. 验证API密钥

#### `POST /validate-api-key`

验证硅基流动API密钥是否有效。

##### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| apiKey | String | 是 | 硅基流动API密钥 |

##### 请求示例

```json
{
  "apiKey": "sk-your-api-key-here"
}
```

##### 响应示例

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "timestamp": 1695552000000,
    "valid": true,
    "message": "API密钥有效"
  }
}
```

---

## 🔧 前端对接指南

### 1. 基础配置

```javascript
// API配置
const API_BASE_URL = 'http://localhost:8080/api/v1/image';

// 请求头配置
const headers = {
  'Content-Type': 'application/json',
  // 如果需要认证，添加Authorization头
  // 'Authorization': 'Bearer your-token-here'
};
```

### 2. 图像生成完整示例

```javascript
/**
 * 图像生成函数
 */
async function generateImage(params) {
  try {
    const response = await fetch(`${API_BASE_URL}/generate`, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify({
        model: params.model || 'Qwen/Qwen-Image',
        prompt: params.prompt,
        negativePrompt: params.negativePrompt || '',
        imageSize: params.imageSize || '1024x1024',
        numInferenceSteps: params.steps || 20,
        cfg: params.cfg || 4.0,
        seed: params.seed
      })
    });
    
    const result = await response.json();
    
    if (result.code === 200) {
      return {
        success: true,
        images: result.data.imageUrls,
        info: result.data
      };
    } else {
      return {
        success: false,
        error: result.message
      };
    }
  } catch (error) {
    return {
      success: false,
      error: '网络错误: ' + error.message
    };
  }
}

// 使用示例
const result = await generateImage({
  model: 'Qwen/Qwen-Image',
  prompt: '一只可爱的小猫咪',
  imageSize: '1024x1024',
  steps: 20,
  cfg: 4.0
});

if (result.success) {
  console.log('生成成功:', result.images[0]);
  // 显示图像
  document.getElementById('generated-image').src = result.images[0];
} else {
  console.error('生成失败:', result.error);
}
```

### 3. React Hook示例

```jsx
import { useState } from 'react';

export const useImageGeneration = () => {
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  
  const generateImage = async (params) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(`${API_BASE_URL}/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(params)
      });
      
      const data = await response.json();
      
      if (data.code === 200) {
        setResult(data.data);
      } else {
        setError(data.message);
      }
    } catch (err) {
      setError('生成失败: ' + err.message);
    } finally {
      setLoading(false);
    }
  };
  
  return { generateImage, loading, result, error };
};
```

### 4. Vue 3 Composition API示例

```javascript
import { ref } from 'vue';

export function useImageGeneration() {
  const loading = ref(false);
  const result = ref(null);
  const error = ref(null);
  
  const generateImage = async (params) => {
    loading.value = true;
    error.value = null;
    
    try {
      const response = await fetch(`${API_BASE_URL}/generate`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(params)
      });
      
      const data = await response.json();
      
      if (data.code === 200) {
        result.value = data.data;
      } else {
        error.value = data.message;
      }
    } catch (err) {
      error.value = '生成失败: ' + err.message;
    } finally {
      loading.value = false;
    }
  };
  
  return { generateImage, loading, result, error };
}
```

---

## 📝 错误处理

### 常见错误码

| 错误码 | 描述 | 解决方案 |
|--------|------|----------|
| 2101 | 图像生成服务错误 | 检查服务状态和配置 |
| 2102 | 图像描述提示词无效 | 检查prompt参数 |
| 2103 | 图像生成模型不支持 | 使用支持的模型名称 |
| 2104 | 图像尺寸无效 | 使用支持的尺寸规格 |
| 2105 | 图像生成失败 | 检查网络和API状态 |
| 2113 | API密钥无效 | 检查硅基流动API密钥 |

### 错误处理示例

```javascript
function handleImageError(error) {
  switch (error.code) {
    case 2102:
      return '请输入有效的图像描述';
    case 2103:
      return '请选择支持的图像模型';
    case 2104:
      return '请选择支持的图像尺寸';
    case 2105:
      return '图像生成失败，请稍后重试';
    case 2113:
      return 'API密钥无效，请检查配置';
    default:
      return '未知错误: ' + error.message;
  }
}
```

---

---

## ⚠️ 重要注意事项

### 模型参数兼容性

**Kwai-Kolors/Kolors模型特性：**
- ✅ 支持：`guidanceScale`、`batchSize`
- ❌ 不支持：`cfg`参数
- 🔄 **自动处理**：如果为Kolors模型设置了cfg参数，系统会自动忽略，不会报错

**Qwen系列模型特性：**
- ✅ 支持：`cfg`参数
- ❌ 不支持：`guidanceScale`、`batchSize`
- 🔄 **自动处理**：如果为Qwen模型设置了guidanceScale，系统会自动忽略

### 前端开发建议

```javascript
// 推荐：根据模型动态设置参数
function buildImageParams(model, baseParams) {
  const params = { ...baseParams };
  
  if (model.startsWith('Kwai-Kolors/')) {
    // Kolors模型：移除cfg，保留guidanceScale
    delete params.cfg;
    params.guidanceScale = params.guidanceScale || 7.5;
  } else if (model.startsWith('Qwen/')) {
    // Qwen模型：移除guidanceScale，保留cfg
    delete params.guidanceScale;
    params.cfg = params.cfg || 4.0;
  }
  
  return params;
}
```

---

## 🎯 最佳实践

### 1. 提示词优化

```javascript
// 好的提示词示例
const goodPrompts = {
  portrait: "高质量肖像照，专业摄影，柔和光线，4K分辨率",
  landscape: "壮丽自然风景，高清摄影，黄金时刻光线，超高清",
  art: "数字艺术作品，概念艺术风格，细节丰富，专业渲染"
};

// 负向提示词
const negativePrompts = "模糊，低质量，变形，噪点，扭曲，不清晰";
```

### 2. 参数选择建议

```javascript
// 不同用途的推荐参数
const presets = {
  // 高质量单图
  highQuality: {
    model: "Qwen/Qwen-Image",
    imageSize: "1328x1328",
    numInferenceSteps: 30,
    cfg: 4.0
  },
  
  // 快速生成
  fast: {
    model: "Kwai-Kolors/Kolors", 
    imageSize: "1024x1024",
    numInferenceSteps: 15,
    guidanceScale: 5.0
  },
  
  // 批量生成
  batch: {
    model: "Kwai-Kolors/Kolors",
    imageSize: "1024x1024", 
    batchSize: 3,
    guidanceScale: 7.5
  }
};
```

### 3. 加载状态管理

```javascript
// 完整的状态管理示例
class ImageGenerator {
  constructor() {
    this.status = 'idle'; // idle, loading, success, error
    this.progress = 0;
  }
  
  async generate(params) {
    this.status = 'loading';
    this.progress = 0;
    
    // 模拟进度更新
    const progressInterval = setInterval(() => {
      this.progress = Math.min(this.progress + 10, 90);
    }, 500);
    
    try {
      const result = await this.callAPI(params);
      this.progress = 100;
      this.status = 'success';
      return result;
    } catch (error) {
      this.status = 'error';
      throw error;
    } finally {
      clearInterval(progressInterval);
    }
  }
}
```

---

## 📞 技术支持

如有问题或需要支持，请联系开发团队或查看项目文档。

**注意**：所有生成的图像都会自动上传到七牛云CDN，返回的URL是永久可访问的CDN地址，适合直接在前端展示使用。

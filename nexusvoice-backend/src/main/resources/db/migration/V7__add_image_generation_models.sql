-- V7: 添加图像生成模型配置
-- Author: NexusVoice Team
-- Date: 2025-01-24

-- =====================================================
-- 1. 插入图像生成模型配置
-- =====================================================

-- 硅基流动 Kolors 图像生成模型
INSERT INTO ai_models (
    provider_code,
    model_code,
    model_type,
    model_name,
    description,
    model_class,
    default_base_url,
    default_temperature,
    default_max_tokens,
    default_timeout_seconds,
    context_window,
    input_token_price,
    output_token_price,
    config_json,
    status,
    priority,
    capabilities,
    input_types,
    output_types,
    created_at,
    updated_at
) VALUES (
    'siliconflow',
    'kolors',
    'image',
    'Kwai-Kolors/Kolors',
    'Kwai Kolors 图像生成模型，支持高质量AI图像生成',
    'SiliconFlowImageAdapter',
    'https://api.siliconflow.cn/v1',
    NULL,
    NULL,
    120,
    NULL,
    0.002,
    0,
    '{
        "image_sizes": ["1024x1024", "768x768", "512x512", "1280x720", "720x1280"],
        "default_image_size": "1024x1024",
        "default_steps": 20,
        "default_cfg": 4.0,
        "supports_batch": true,
        "max_batch_size": 4,
        "supports_negative_prompt": true,
        "supports_seed": true,
        "cost_per_image": 0.002
    }',
    1,
    20,
    ARRAY['image_generation']::TEXT[],
    ARRAY['text']::TEXT[],
    ARRAY['image']::TEXT[],
    NOW(),
    NOW()
);

-- =====================================================
-- 2. 插入API密钥配置（示例，需要替换为实际密钥）
-- =====================================================

-- 注意：以下API密钥为示例，实际使用时需要替换为真实的硅基流动API密钥
-- 可以通过管理界面或直接更新数据库来配置实际的API密钥

INSERT INTO ai_api_keys (
    provider_code,
    model_code,
    api_key,
    api_secret,
    base_url,
    proxy_url,
    weight,
    rate_limit,
    concurrent_limit,
    status,
    fail_count,
    last_success_time,
    health_check_time,
    total_requests,
    total_tokens_used,
    total_cost,
    last_used_at,
    monthly_requests,
    monthly_tokens_used,
    monthly_cost,
    monthly_reset_date,
    daily_quota_limit,
    monthly_quota_limit,
    daily_tokens_used,
    created_at,
    updated_at
) VALUES (
    'siliconflow',
    'kolors',
    'sk-your-siliconflow-api-key-here',
    NULL,
    'https://api.siliconflow.cn/v1',
    NULL,
    100,
    60,
    5,
    1,
    0,
    NOW(),
    NOW(),
    0,
    0,
    0.00,
    NULL,
    0,
    0,
    0.00,
    DATE_FORMAT(NOW(), '%Y-%m-01'),
    NULL,
    NULL,
    0,
    NOW(),
    NOW()
);

-- =====================================================
-- 3. 添加注释说明
-- =====================================================

-- 图像生成模型说明：
-- 1. model_type = 'image' 标识这是图像生成模型
-- 2. model_name 存储实际调用API时使用的模型名称（如 'Kwai-Kolors/Kolors'）
-- 3. input_token_price 表示每张图片的生成成本（元）
-- 4. config_json 包含图像生成的详细配置：
--    - image_sizes: 支持的图像尺寸列表
--    - default_image_size: 默认图像尺寸
--    - default_steps: 默认推理步数
--    - default_cfg: 默认CFG参数
--    - supports_batch: 是否支持批量生成
--    - max_batch_size: 最大批量生成数量
--    - cost_per_image: 每张图片的成本
-- 5. capabilities 标识模型能力，image_generation 表示图像生成能力
-- 6. input_types 为 'text'，output_types 为 'image'

-- API密钥配置说明：
-- 1. 需要替换 api_key 为实际的硅基流动API密钥（以 'sk-' 开头）
-- 2. weight 为密钥权重，用于负载均衡
-- 3. rate_limit 为每分钟请求限制
-- 4. concurrent_limit 为并发请求限制
-- 5. status = 1 表示密钥可用

-- 费用计算说明：
-- 图像生成按图片数量计费，每次调用会根据生成的图片数量计算费用
-- 费用 = 图片数量 × cost_per_image
-- 例如：批量生成4张图片，费用 = 4 × 0.002 = 0.008元

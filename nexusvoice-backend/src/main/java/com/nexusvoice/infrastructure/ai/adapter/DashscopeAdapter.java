package com.nexusvoice.infrastructure.ai.adapter;

import org.springframework.stereotype.Component;

/**
 * DashScope 适配器
 * 说明：通义千问提供了 OpenAI 兼容的 compatible-mode 端点，
 * 在当前阶段直接复用 OpenAI 兼容适配器逻辑，避免协议未实现导致的运行错误。
 */
@Component("dashscopeAdapter")
public class DashscopeAdapter extends OpenAiCompatibleAdapter {
    // 复用父类全部逻辑，无需额外实现
}


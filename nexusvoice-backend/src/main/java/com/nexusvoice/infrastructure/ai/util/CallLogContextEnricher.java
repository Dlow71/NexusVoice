package com.nexusvoice.infrastructure.ai.util;

import com.nexusvoice.domain.ai.model.AiApiCallLog;
import com.nexusvoice.infrastructure.config.UserContext;
import com.nexusvoice.infrastructure.context.DeveloperApiContext;

/**
 * 统一补充调用日志的上下文字段（developerApiKeyId, authType）
 */
public class CallLogContextEnricher {

    public static void enrich(AiApiCallLog log) {
        try {
            Long devKeyId = DeveloperApiContext.getDeveloperApiKeyId();
            if (devKeyId != null) {
                log.setDeveloperApiKeyId(devKeyId);
                log.setAuthType("API_KEY");
            } else if (UserContext.isAuthenticated()) {
                log.setAuthType("JWT");
            }
        } catch (Throwable ignored) {
        }
    }
}


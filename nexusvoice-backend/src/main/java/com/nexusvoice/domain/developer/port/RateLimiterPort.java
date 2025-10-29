package com.nexusvoice.domain.developer.port;

import com.nexusvoice.exception.BizException;

/**
 * 速率限制端口（Port）
 * 应用/领域依赖抽象，基础设施提供实现
 */
public interface RateLimiterPort {

    /**
     * 消耗每分钟配额（超过配额抛出BizException）
     */
    void checkAndConsumePerMinute(Long keyId, Integer perMinuteLimit) throws BizException;

    /**
     * 消耗每日配额（超过配额抛出BizException）
     */
    void checkAndConsumePerDay(Long keyId, Integer perDayLimit) throws BizException;
}


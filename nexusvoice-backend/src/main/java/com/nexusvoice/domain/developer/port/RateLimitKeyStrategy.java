package com.nexusvoice.domain.developer.port;

/**
 * 限流/计费 键空间策略（Port）
 * 通过策略可参数化键名前缀与维度，便于按scope/route等做精细化限流
 */
public interface RateLimitKeyStrategy {

    String minuteKey(Long keyId);

    String dayKey(Long keyId);

    String dailyCostKey(Long keyId);
}


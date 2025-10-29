package com.nexusvoice.domain.developer.port;

import java.math.BigDecimal;

/**
 * 每日费用统计端口（Port）
 */
public interface DailyCostCounterPort {

    /**
     * 获取当日累计费用
     */
    BigDecimal getTodayCost(Long keyId);

    /**
     * 增加当日费用
     */
    void addTodayCost(Long keyId, BigDecimal amount);
}


package com.nexusvoice.infrastructure.adapter.redis;

import com.nexusvoice.domain.developer.port.DailyCostCounterPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class RedisDailyCostCounterAdapter implements DailyCostCounterPort {

    private static final String DAILY_COST_KEY_PREFIX = "developer:api:daily_cost:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private com.nexusvoice.domain.developer.port.RateLimitKeyStrategy keyStrategy;

    @Override
    public BigDecimal getTodayCost(Long keyId) {
        String key = keyStrategy.dailyCostKey(keyId) + ":" + LocalDate.now();
        String costStr = stringRedisTemplate.opsForValue().get(key);
        try {
            return costStr != null ? new BigDecimal(costStr) : BigDecimal.ZERO;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public void addTodayCost(Long keyId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) return;
        String key = keyStrategy.dailyCostKey(keyId) + ":" + LocalDate.now();
        stringRedisTemplate.opsForValue().increment(key, amount.doubleValue());
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        Date expireDate = Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant());
        stringRedisTemplate.expireAt(key, expireDate);
    }
}

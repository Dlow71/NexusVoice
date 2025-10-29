package com.nexusvoice.infrastructure.adapter.redis;

import com.nexusvoice.domain.developer.port.RateLimiterPort;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class RedisRateLimiterAdapter implements RateLimiterPort {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private com.nexusvoice.domain.developer.port.RateLimitKeyStrategy keyStrategy;

    @Override
    public void checkAndConsumePerMinute(Long keyId, Integer perMinuteLimit) throws BizException {
        if (perMinuteLimit == null) return;
        String minuteKey = keyStrategy.minuteKey(keyId);
        Long currentMinuteCount = stringRedisTemplate.opsForValue().increment(minuteKey);
        if (currentMinuteCount != null && currentMinuteCount == 1L) {
            stringRedisTemplate.expire(minuteKey, 1, TimeUnit.MINUTES);
        }
        if (currentMinuteCount != null && currentMinuteCount > perMinuteLimit) {
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_RATE_LIMIT_EXCEEDED, "API每分钟调用次数超限");
        }
    }

    @Override
    public void checkAndConsumePerDay(Long keyId, Integer perDayLimit) throws BizException {
        if (perDayLimit == null) return;
        String dayKey = keyStrategy.dayKey(keyId) + ":" + LocalDate.now();
        Long currentDayCount = stringRedisTemplate.opsForValue().increment(dayKey);
        if (currentDayCount != null && currentDayCount == 1L) {
            LocalDateTime tomorrow = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            Date expireDate = Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant());
            stringRedisTemplate.expireAt(dayKey, expireDate);
        }
        if (currentDayCount != null && currentDayCount > perDayLimit) {
            throw BizException.of(ErrorCodeEnum.DEVELOPER_API_KEY_DAILY_LIMIT_EXCEEDED, "API每日调用次数超限");
        }
    }
}

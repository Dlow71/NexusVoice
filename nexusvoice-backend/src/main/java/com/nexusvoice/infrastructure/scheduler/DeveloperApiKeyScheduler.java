package com.nexusvoice.infrastructure.scheduler;

import com.nexusvoice.domain.developer.model.DeveloperApiKey;
import com.nexusvoice.domain.developer.repository.DeveloperApiKeyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 开发者API密钥每日统计重置任务
 * 每日0点后执行：重置今日请求次数与今日Token数
 */
@Slf4j
@Component
public class DeveloperApiKeyScheduler {

    private final DeveloperApiKeyRepository apiKeyRepository;

    public DeveloperApiKeyScheduler(DeveloperApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * 每日 00:05 触发，重置今日统计
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void resetDailyStats() {
        try {
            List<DeveloperApiKey> keys = apiKeyRepository.findKeysNeedDailyReset();
            int count = 0;
            for (DeveloperApiKey key : keys) {
                key.resetDailyStats();
                apiKeyRepository.update(key);
                count++;
            }
            log.info("开发者API密钥每日统计重置完成，共处理 {} 条", count);
        } catch (Exception e) {
            log.error("开发者API密钥每日统计重置失败", e);
        }
    }
}


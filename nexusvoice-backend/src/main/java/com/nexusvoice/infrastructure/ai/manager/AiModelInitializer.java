package com.nexusvoice.infrastructure.ai.manager;

import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI模型统一初始化协调器
 * 负责统一加载所有AI模型，避免重复查询数据库
 * 
 * <p>优化说明：
 * - 启动时只查询数据库一次，然后按类型分发给各Manager
 * - 定时刷新任务统一调度，避免各Manager独立查询
 * - 减少数据库压力，提升系统启动速度
 * </p>
 *
 * @author NexusVoice
 * @since 2025-10-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelInitializer {
    
    private final AiModelRepository modelRepository;
    private final DynamicAiModelBeanManager modelBeanManager;
    private final DynamicAiEmbeddingBeanManager embeddingBeanManager;
    private final DynamicAiRerankBeanManager rerankBeanManager;
    
    /**
     * 系统启动时统一初始化所有AI模型服务
     * 只查询一次数据库，然后按类型分发
     */
    @PostConstruct
    public void init() {
        log.info("==================== 开始统一初始化AI模型服务 ====================");
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 只查询一次数据库，获取所有启用的模型
            List<AiModel> allEnabledModels = modelRepository.findAllEnabled();
            log.info("从数据库加载了{}个启用的模型", allEnabledModels.size());
            
            // 2. 按模型类型分组
            Map<String, List<AiModel>> modelsByType = allEnabledModels.stream()
                    .collect(Collectors.groupingBy(AiModel::getModelType));
            
            // 3. 统计各类型模型数量
            int chatCount = modelsByType.getOrDefault("chat", List.of()).size();
            int imageCount = modelsByType.getOrDefault("image", List.of()).size();
            int asrCount = modelsByType.getOrDefault("asr", List.of()).size();
            int ttsCount = modelsByType.getOrDefault("tts", List.of()).size();
            int videoCount = modelsByType.getOrDefault("video", List.of()).size();
            int embeddingCount = modelsByType.getOrDefault("embedding", List.of()).size();
            int rerankCount = modelsByType.getOrDefault("rerank", List.of()).size();
            
            log.info("模型类型分布 - CHAT: {}, IMAGE: {}, ASR: {}, TTS: {}, VIDEO: {}, EMBEDDING: {}, RERANK: {}", 
                    chatCount, imageCount, asrCount, ttsCount, videoCount, embeddingCount, rerankCount);
            
            // 4. 分发给各个Manager（无需再查询数据库）
            modelBeanManager.loadModels(
                    modelsByType.getOrDefault("chat", List.of()),
                    modelsByType.getOrDefault("image", List.of()),
                    modelsByType.getOrDefault("asr", List.of()),
                    modelsByType.getOrDefault("tts", List.of()),
                    modelsByType.getOrDefault("video", List.of())
            );
            
            embeddingBeanManager.loadModels(
                    modelsByType.getOrDefault("embedding", List.of())
            );
            
            rerankBeanManager.loadModels(
                    modelsByType.getOrDefault("rerank", List.of())
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("==================== AI模型服务初始化完成，耗时{}ms ====================", duration);
            
        } catch (Exception e) {
            log.error("AI模型服务初始化失败", e);
            throw new RuntimeException("AI模型服务初始化失败", e);
        }
    }
    
    /**
     * 定时刷新任务（每30分钟执行一次）
     * 统一调度所有Manager的刷新，避免重复查询
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 1800000)
    public void scheduledRefresh() {
        log.info("==================== 开始执行AI模型定时刷新任务 ====================");
        long startTime = System.currentTimeMillis();
        
        try {
            // 统一查询一次
            List<AiModel> allEnabledModels = modelRepository.findAllEnabled();
            log.info("定时刷新：从数据库加载了{}个启用的模型", allEnabledModels.size());
            
            // 按类型分组
            Map<String, List<AiModel>> modelsByType = allEnabledModels.stream()
                    .collect(Collectors.groupingBy(AiModel::getModelType));
            
            // 分发刷新
            modelBeanManager.loadModels(
                    modelsByType.getOrDefault("chat", List.of()),
                    modelsByType.getOrDefault("image", List.of()),
                    modelsByType.getOrDefault("asr", List.of()),
                    modelsByType.getOrDefault("tts", List.of()),
                    modelsByType.getOrDefault("video", List.of())
            );
            
            embeddingBeanManager.loadModels(
                    modelsByType.getOrDefault("embedding", List.of())
            );
            
            rerankBeanManager.loadModels(
                    modelsByType.getOrDefault("rerank", List.of())
            );
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("==================== AI模型定时刷新完成，耗时{}ms ====================", duration);
            
        } catch (Exception e) {
            log.error("AI模型定时刷新失败", e);
        }
    }
    
    /**
     * 手动刷新所有模型服务
     * 提供给外部调用的刷新接口
     */
    public void refreshAll() {
        log.info("手动触发AI模型刷新");
        scheduledRefresh();
    }
}

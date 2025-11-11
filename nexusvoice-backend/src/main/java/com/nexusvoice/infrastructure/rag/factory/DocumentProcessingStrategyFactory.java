package com.nexusvoice.infrastructure.rag.factory;

import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.service.DocumentProcessingStrategy;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档处理策略工厂
 * 根据文件类型选择合适的处理策略
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentProcessingStrategyFactory {
    
    private final List<DocumentProcessingStrategy> strategies;
    
    /**
     * 根据文件详情获取处理策略
     * 
     * @param fileDetail 文件详情
     * @return 处理策略
     * @throws BizException 如果没有找到合适的策略
     */
    public DocumentProcessingStrategy getStrategy(FileDetail fileDetail) {
        if (fileDetail == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文件详情不能为空");
        }
        
        log.debug("为文件选择处理策略 - 文件ID: {}, 文件类型: {}", 
                fileDetail.getId(), fileDetail.getFileType());
        
        for (DocumentProcessingStrategy strategy : strategies) {
            if (strategy.supports(fileDetail)) {
                log.info("已选择处理策略: {} - 文件ID: {}", 
                        strategy.getStrategyName(), fileDetail.getId());
                return strategy;
            }
        }
        
        throw new BizException(
                ErrorCodeEnum.SYSTEM_ERROR, 
                String.format("未找到支持文件类型 %s 的处理策略", fileDetail.getFileType())
        );
    }
    
    /**
     * 获取所有已注册的策略
     * 
     * @return 策略列表
     */
    public List<DocumentProcessingStrategy> getAllStrategies() {
        return List.copyOf(strategies);
    }
    
    /**
     * 获取所有支持的文件扩展名
     * 
     * @return 扩展名列表
     */
    public List<String> getSupportedExtensions() {
        return strategies.stream()
                .flatMap(strategy -> strategy.getSupportedExtensions().stream())
                .distinct()
                .toList();
    }
}

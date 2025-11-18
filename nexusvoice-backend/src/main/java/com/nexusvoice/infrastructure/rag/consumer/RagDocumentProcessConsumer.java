package com.nexusvoice.infrastructure.rag.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nexusvoice.domain.mq.constants.MQTopicConstants;
import com.nexusvoice.domain.mq.model.Message;
import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.enums.ProcessStatus;
import com.nexusvoice.domain.rag.model.vo.ProcessedSegment;
import com.nexusvoice.domain.rag.model.vo.SegmentSplitConfig;
import com.nexusvoice.domain.rag.repository.FileDetailRepository;
import com.nexusvoice.domain.rag.service.DocumentProcessingStrategy;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.mq.consumer.AbstractMessageListener;
import com.nexusvoice.domain.mq.repository.MessageRepository;
import com.nexusvoice.domain.mq.model.Message;
import com.nexusvoice.infrastructure.rag.factory.DocumentProcessingStrategyFactory;
import com.nexusvoice.infrastructure.rag.service.DocumentUnitSaveService;
import com.nexusvoice.infrastructure.rag.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * RAG文档处理消费者
 * 负责阶段1：结构化解析与原文分割
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQTopicConstants.TOPIC_RAG_DOCUMENT_PROCESS,
        consumerGroup = "rag_document_process_consumer_group",
        selectorExpression = "*",
        maxReconsumeTimes = 3
)
public class RagDocumentProcessConsumer extends AbstractMessageListener<Map<String, Object>> {
    
    private final FileDetailRepository fileDetailRepository;
    private final DocumentProcessingStrategyFactory strategyFactory;
    private final FileStorageService fileStorageService;
    private final DocumentUnitSaveService documentUnitSaveService;
    private final MessageRepository messageRepository;
    
    @Override
    protected String getTopic() {
        return MQTopicConstants.TOPIC_RAG_DOCUMENT_PROCESS;
    }
    
    @Override
    protected String getConsumerGroup() {
        return "rag_document_process_consumer_group";
    }
    
    @Override
    protected TypeReference<Message<Map<String, Object>>> getMessageType() {
        return new TypeReference<Message<Map<String, Object>>>() {};
    }
    
    @Override
    protected void handleBusiness(Message<Map<String, Object>> message) throws Exception {
        Map<String, Object> payload = message.getPayload();
        
        // 1. 获取文件ID
        Long fileId = extractFileId(payload);
        log.info("开始处理RAG文档 - 文件ID: {}", fileId);
        
        // 2. 查询文件详情
        FileDetail fileDetail = fileDetailRepository.findById(fileId)
                .orElseThrow(() -> new BizException(
                        ErrorCodeEnum.DATA_NOT_FOUND, 
                        "文件不存在: " + fileId
                ));
        
        // 3. 检查文件状态
        if (fileDetail.isProcessCompleted()) {
            log.warn("文件已处理，跳过 - 文件ID: {}", fileId);
            return;
        }
        
        // 标记为处理中
        fileDetail.startProcessing();
        
        // 4. 获取文件内容
        byte[] fileContent = loadFileContent(fileDetail);
        
        // 5. 选择处理策略
        DocumentProcessingStrategy strategy = strategyFactory.getStrategy(fileDetail);
        log.info("使用处理策略: {} - 文件ID: {}", strategy.getStrategyName(), fileId);
        
        // 6. 执行阶段1：结构化解析与原文分割
        SegmentSplitConfig splitConfig = extractSplitConfig(payload);
        List<ProcessedSegment> segments = strategy.parseAndSplit(fileDetail, fileContent, splitConfig);
        log.info("阶段1完成 - 文件ID: {}, 分割段落数: {}", fileId, segments.size());
        
        // 7. 保存分割结果到DocumentUnit表
        saveSegments(fileDetail, segments);
        
        // 8. 发送向量化任务，更新状态为向量化中
        fileDetailRepository.updateStatus(fileId, ProcessStatus.VECTORIZING);
        try {
            Message<java.util.Map<String, Object>> vecMsg = Message.createNormalMessage(
                    MQTopicConstants.TOPIC_RAG_DOCUMENT_VECTORIZE,
                    java.util.Map.of("fileId", fileId)
            );
            messageRepository.send(vecMsg);
            log.info("已发送向量化任务 - 文件ID: {}", fileId);
        } catch (Exception e) {
            log.error("发送向量化任务失败 - 文件ID: {}", fileId, e);
            throw new BizException(ErrorCodeEnum.MQ_SEND_ERROR, "发送向量化任务失败");
        }
        
        log.info("RAG文档阶段1完成（解析+分割） - 文件ID: {}, 段落数: {}", fileId, segments.size());
    }
    
    /**
     * 提取文件ID
     */
    private Long extractFileId(Map<String, Object> payload) {
        Object fileIdObj = payload.get("fileId");
        if (fileIdObj == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "消息中缺少fileId参数");
        }
        
        if (fileIdObj instanceof Number) {
            return ((Number) fileIdObj).longValue();
        }
        
        try {
            return Long.parseLong(fileIdObj.toString());
        } catch (NumberFormatException e) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "fileId格式错误: " + fileIdObj);
        }
    }
    
    /**
     * 提取分段配置
     */
    private SegmentSplitConfig extractSplitConfig(Map<String, Object> payload) {
        // 从消息中读取配置，如果没有则使用默认配置
        Integer maxLength = (Integer) payload.get("maxLength");
        Integer minLength = (Integer) payload.get("minLength");
        Integer overlapSize = (Integer) payload.get("overlapSize");
        
        if (maxLength != null && minLength != null && overlapSize != null) {
            return new SegmentSplitConfig(maxLength, minLength, overlapSize);
        }
        
        return SegmentSplitConfig.defaultConfig();
    }
    
    /**
     * 加载文件内容
     */
    private byte[] loadFileContent(FileDetail fileDetail) {
        return fileStorageService.loadFileContent(fileDetail);
    }
    
    /**
     * 保存分割后的段落
     */
    private void saveSegments(FileDetail fileDetail, List<ProcessedSegment> segments) {
        documentUnitSaveService.saveSegments(fileDetail, segments);
    }
    
    @Override
    protected void handleFailure(Message<Map<String, Object>> message, Exception e) {
        super.handleFailure(message, e);
        
        // 记录失败日志到数据库
        Map<String, Object> payload = message.getPayload();
        Long fileId = extractFileId(payload);
        
        log.error("RAG文档处理失败 - 文件ID: {}, 错误信息: {}", fileId, e.getMessage(), e);
        
        try {
            fileDetailRepository.updateStatus(fileId, ProcessStatus.FAILED);
            fileDetailRepository.updateError(fileId, "PROCESS_FAILED", e.getMessage());
        } catch (Exception ex) {
            log.error("更新文件状态为失败时出错 - 文件ID: {}", fileId, ex);
        }
    }
    
    @Override
    protected void handleSuccess(Message<Map<String, Object>> message) {
        super.handleSuccess(message);
        
        Map<String, Object> payload = message.getPayload();
        Long fileId = extractFileId(payload);
        
        log.info("RAG文档处理成功 - 文件ID: {}", fileId);
    }
}

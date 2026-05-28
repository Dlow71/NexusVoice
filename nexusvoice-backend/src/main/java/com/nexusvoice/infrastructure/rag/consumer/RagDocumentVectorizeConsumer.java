package com.nexusvoice.infrastructure.rag.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nexusvoice.domain.mq.constants.MQTopicConstants;
import com.nexusvoice.domain.mq.model.Message;
import com.nexusvoice.domain.rag.model.enums.ProcessStatus;
import com.nexusvoice.domain.rag.repository.FileDetailRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.mq.consumer.AbstractMessageListener;
import com.nexusvoice.infrastructure.rag.service.DocumentVectorizationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RAG 文档向量化消费者
 * 负责阶段2：对解析后的文档单元执行向量化，并更新文件状态
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "rocketmq.listeners.rag-document-vectorize",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RocketMQMessageListener(
        topic = MQTopicConstants.TOPIC_RAG_DOCUMENT_VECTORIZE,
        consumerGroup = "rag_document_vectorize_consumer_group",
        selectorExpression = "*",
        maxReconsumeTimes = 3
)
public class RagDocumentVectorizeConsumer extends AbstractMessageListener<Map<String, Object>> {

    private final DocumentVectorizationServiceImpl documentVectorizationService;
    private final FileDetailRepository fileDetailRepository;

    @Override
    protected String getTopic() {
        return MQTopicConstants.TOPIC_RAG_DOCUMENT_VECTORIZE;
    }

    @Override
    protected String getConsumerGroup() {
        return "rag_document_vectorize_consumer_group";
    }

    @Override
    protected TypeReference<Message<Map<String, Object>>> getMessageType() {
        return new TypeReference<Message<Map<String, Object>>>() {};
    }

    @Override
    protected void handleBusiness(Message<Map<String, Object>> message) throws Exception {
        Map<String, Object> payload = message.getPayload();
        Long fileId = extractFileId(payload);
        log.info("开始执行向量化 - 文件ID: {}", fileId);

        // 执行向量化
        int count = documentVectorizationService.vectorizeFileDocuments(fileId);
        log.info("向量化完成 - 文件ID: {}, 成功数量: {}", fileId, count);

        // 更新文件状态为完成
        fileDetailRepository.updateStatus(fileId, ProcessStatus.COMPLETED);
    }

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
}

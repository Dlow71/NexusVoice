package com.nexusvoice.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.domain.rag.model.entity.VectorStore;
import com.nexusvoice.infrastructure.persistence.po.VectorStorePO;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * VectorStore实体与VectorStorePO转换器
 * 负责领域模型与持久化模型之间的转换
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Component
public class VectorStorePOConverter {

    private final ObjectMapper objectMapper;

    public VectorStorePOConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将领域实体转换为持久化对象
     */
    public VectorStorePO toPO(VectorStore entity) {
        if (entity == null) {
            return null;
        }
        
        VectorStorePO po = new VectorStorePO();
        
        // 注意：VectorStore使用UUID作为ID
        po.setId(entity.getId());
        po.setCreatedAt(entity.getCreatedAt());
        po.setUpdatedAt(entity.getUpdatedAt());
        po.setDeleted(entity.getDeleted());
        
        // 业务字段
        po.setDocumentUnitId(entity.getDocumentUnitId());
        po.setEmbeddingModel(entity.getEmbeddingModel());
        po.setEmbeddingDimension(entity.getEmbeddingDimension());
        po.setEmbedding(entity.getEmbedding());
        
        // 将Map转换为JSON字符串
        if (entity.getMetadata() != null) {
            try {
                po.setMetadata(objectMapper.writeValueAsString(entity.getMetadata()));
            } catch (Exception e) {
                po.setMetadata(null);
            }
        }
        
        return po;
    }

    /**
     * 将持久化对象转换为领域实体
     */
    public VectorStore toDomain(VectorStorePO po) {
        if (po == null) {
            return null;
        }
        
        VectorStore entity = new VectorStore();
        
        // 注意：VectorStore使用UUID作为ID
        entity.setId(po.getId());
        entity.setCreatedAt(po.getCreatedAt());
        entity.setUpdatedAt(po.getUpdatedAt());
        entity.setDeleted(po.getDeleted());
        
        // 业务字段
        entity.setDocumentUnitId(po.getDocumentUnitId());
        entity.setEmbeddingModel(po.getEmbeddingModel());
        entity.setEmbeddingDimension(po.getEmbeddingDimension());
        entity.setEmbedding(po.getEmbedding());
        
        // 将JSON字符串转换为Map
        if (po.getMetadata() != null && !po.getMetadata().trim().isEmpty()) {
            try {
                Map<String, Object> metadata = objectMapper.readValue(
                    po.getMetadata(), 
                    new TypeReference<Map<String, Object>>() {}
                );
                entity.setMetadata(metadata);
            } catch (Exception e) {
                entity.setMetadata(null);
            }
        }
        
        return entity;
    }
}

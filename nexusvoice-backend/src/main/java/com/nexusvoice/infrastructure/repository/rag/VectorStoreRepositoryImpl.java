package com.nexusvoice.infrastructure.repository.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rag.model.entity.VectorStore;
import com.nexusvoice.domain.rag.repository.VectorStoreRepository;
import com.nexusvoice.infrastructure.persistence.converter.VectorStorePOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.VectorStorePOMapper;
import com.nexusvoice.infrastructure.persistence.po.VectorStorePO;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 向量存储仓储实现类
 * 
 * @author NexusVoice
 * @since 2025-10-23
 */
@Repository
public class VectorStoreRepositoryImpl implements VectorStoreRepository {

    private final VectorStorePOMapper mapper;
    private final VectorStorePOConverter converter;

    public VectorStoreRepositoryImpl(VectorStorePOMapper mapper, VectorStorePOConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public VectorStore save(VectorStore vectorStore) {
        VectorStorePO po = converter.toPO(vectorStore);
        
        if (po.getId() == null || po.getId().isEmpty()) {
            // 生成新的UUID
            po.setId(UUID.randomUUID().toString());
            mapper.insert(po);
            vectorStore.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return vectorStore;
    }

    @Override
    public List<VectorStore> saveAll(List<VectorStore> vectorStores) {
        for (VectorStore vectorStore : vectorStores) {
            save(vectorStore);
        }
        return vectorStores;
    }

    @Override
    public Optional<VectorStore> findById(String id) {
        VectorStorePO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public Optional<VectorStore> findByDocumentUnitId(Long documentUnitId) {
        LambdaQueryWrapper<VectorStorePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VectorStorePO::getDocumentUnitId, documentUnitId)
               .eq(VectorStorePO::getDeleted, 0)
               .orderByDesc(VectorStorePO::getCreatedAt)
               .last("LIMIT 1");
        
        VectorStorePO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }

    @Override
    public List<VectorStore> findByDocumentUnitIds(List<Long> documentUnitIds) {
        if (documentUnitIds == null || documentUnitIds.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<VectorStorePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VectorStorePO::getDocumentUnitId, documentUnitIds)
               .eq(VectorStorePO::getDeleted, 0)
               .orderByDesc(VectorStorePO::getCreatedAt);
        
        List<VectorStorePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<VectorStore> findSimilar(List<Float> queryEmbedding, int limit) {
        // 将向量转换为PostgreSQL格式的字符串
        String embeddingStr = vectorToString(queryEmbedding);
        
        List<VectorStorePO> poList = mapper.searchSimilar(embeddingStr, limit);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<VectorStore> findSimilarInFile(Long fileId, List<Float> queryEmbedding, int limit) {
        String embeddingStr = vectorToString(queryEmbedding);
        List<VectorStorePO> poList = mapper.searchSimilarInFile(embeddingStr, fileId, limit);
        return poList.stream().map(converter::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<VectorStore> findSimilarInKnowledgeBase(Long knowledgeBaseId, List<Float> queryEmbedding, int limit) {
        String embeddingStr = vectorToString(queryEmbedding);
        
        List<VectorStorePO> poList = mapper.searchSimilarInKnowledgeBase(embeddingStr, knowledgeBaseId, limit);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<VectorStore> findSimilarWithThreshold(List<Float> queryEmbedding, double similarityThreshold, int limit) {
        // 需要自定义SQL查询，使用余弦相似度阈值过滤
        // 这里简化处理，直接调用基础相似度搜索
        return findSimilar(queryEmbedding, limit);
    }

    @Override
    public VectorStore update(VectorStore vectorStore) {
        VectorStorePO po = converter.toPO(vectorStore);
        mapper.updateById(po);
        return vectorStore;
    }

    @Override
    public List<VectorStore> findByModel(String embeddingModel, int limit) {
        LambdaQueryWrapper<VectorStorePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VectorStorePO::getEmbeddingModel, embeddingModel)
               .eq(VectorStorePO::getDeleted, 0)
               .orderByDesc(VectorStorePO::getCreatedAt)
               .last("LIMIT " + limit);
        
        List<VectorStorePO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(Long id) {
        // 该仓储实体主键为UUID字符串，避免误删，这里不支持通过Long删除
        return false;
    }

    @Override
    public boolean deleteByDocumentUnitId(Long documentUnitId) {
        LambdaQueryWrapper<VectorStorePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VectorStorePO::getDocumentUnitId, documentUnitId)
               .eq(VectorStorePO::getDeleted, 0);
        
        VectorStorePO updatePo = new VectorStorePO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper) > 0;
    }

    @Override
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        // 假设ids是documentUnitIds
        LambdaQueryWrapper<VectorStorePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(VectorStorePO::getDocumentUnitId, ids)
               .eq(VectorStorePO::getDeleted, 0);
        
        VectorStorePO updatePo = new VectorStorePO();
        updatePo.setDeleted(1);
        return mapper.update(updatePo, wrapper);
    }

    @Override
    public long count() {
        LambdaQueryWrapper<VectorStorePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VectorStorePO::getDeleted, 0);
        return mapper.selectCount(wrapper);
    }

    @Override
    public long countByModel(String embeddingModel) {
        LambdaQueryWrapper<VectorStorePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VectorStorePO::getEmbeddingModel, embeddingModel)
               .eq(VectorStorePO::getDeleted, 0);
        return mapper.selectCount(wrapper);
    }

    @Override
    public boolean existsByDocumentUnitId(Long documentUnitId) {
        LambdaQueryWrapper<VectorStorePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VectorStorePO::getDocumentUnitId, documentUnitId)
               .eq(VectorStorePO::getDeleted, 0)
               .last("LIMIT 1");
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public int cleanupOrphanedEmbeddings() {
        // 清理文档单元已删除的向量
        // 需要JOIN查询document_units表，这里简化处理
        // 实际应该使用自定义SQL: DELETE FROM vector_store WHERE document_unit_id NOT IN (SELECT id FROM document_units WHERE deleted=0)
        // 暂时返回0，实际需要实现自定义SQL
        return 0;
    }

    /**
     * 将向量转换为PostgreSQL vector格式的字符串
     * 格式：[0.1,0.2,0.3,...]
     */
    private String vectorToString(List<Float> vector) {
        if (vector == null || vector.isEmpty()) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}

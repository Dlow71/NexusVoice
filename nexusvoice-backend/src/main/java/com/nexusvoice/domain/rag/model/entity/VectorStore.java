package com.nexusvoice.domain.rag.model.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 向量存储领域实体
 * 对应 vector_store 表
 * 注意：此实体使用UUID作为主键，不继承BaseDomainEntity
 * 
 * @author NexusVoice
 * @since 2025-10-22
 */
public class VectorStore {
    
    /**
     * 主键ID（UUID）
     */
    private String id;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 逻辑删除标志：0-未删除，1-已删除
     */
    private Integer deleted;
    
    /**
     * 关联文档单元ID
     */
    private Long documentUnitId;
    
    /**
     * 使用的向量模型
     */
    private String embeddingModel;
    
    /**
     * 向量维度
     */
    private Integer embeddingDimension;
    
    /**
     * 向量数据
     */
    private List<Float> embedding;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    // 构造函数
    public VectorStore() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.deleted = 0;
    }
    
    public VectorStore(Long documentUnitId, String embeddingModel, List<Float> embedding) {
        this();
        this.documentUnitId = documentUnitId;
        this.embeddingModel = embeddingModel;
        this.embedding = embedding;
        if (embedding != null) {
            this.embeddingDimension = embedding.size();
        }
    }
    
    // 业务方法
    
    /**
     * 计算与另一个向量的余弦相似度
     */
    public double cosineSimilarity(List<Float> otherEmbedding) {
        if (this.embedding == null || otherEmbedding == null) {
            return 0.0;
        }
        
        if (this.embedding.size() != otherEmbedding.size()) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < this.embedding.size(); i++) {
            dotProduct += this.embedding.get(i) * otherEmbedding.get(i);
            normA += Math.pow(this.embedding.get(i), 2);
            normB += Math.pow(otherEmbedding.get(i), 2);
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * 计算与另一个向量的欧氏距离
     */
    public double euclideanDistance(List<Float> otherEmbedding) {
        if (this.embedding == null || otherEmbedding == null) {
            return Double.MAX_VALUE;
        }
        
        if (this.embedding.size() != otherEmbedding.size()) {
            throw new IllegalArgumentException("向量维度不匹配");
        }
        
        double sum = 0.0;
        for (int i = 0; i < this.embedding.size(); i++) {
            double diff = this.embedding.get(i) - otherEmbedding.get(i);
            sum += diff * diff;
        }
        
        return Math.sqrt(sum);
    }
    
    /**
     * 归一化向量
     */
    public void normalize() {
        if (this.embedding == null || this.embedding.isEmpty()) {
            return;
        }
        
        double norm = 0.0;
        for (Float value : this.embedding) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < this.embedding.size(); i++) {
                this.embedding.set(i, (float) (this.embedding.get(i) / norm));
            }
        }
    }
    
    /**
     * 验证向量维度
     */
    public boolean validateDimension() {
        if (this.embedding == null) {
            return false;
        }
        return this.embedding.size() == this.embeddingDimension;
    }
    
    /**
     * 是否为有效向量
     */
    public boolean isValid() {
        return this.embedding != null 
            && !this.embedding.isEmpty() 
            && this.embeddingDimension != null 
            && this.embeddingDimension > 0
            && validateDimension();
    }
    
    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
    }
    
    /**
     * 获取元数据值
     */
    public Object getMetadataValue(String key) {
        if (this.metadata == null) {
            return null;
        }
        return this.metadata.get(key);
    }
    
    /**
     * 是否包含特定的元数据键
     */
    public boolean hasMetadataKey(String key) {
        return this.metadata != null && this.metadata.containsKey(key);
    }
    
    /**
     * 获取向量的L2范数
     */
    public double getL2Norm() {
        if (this.embedding == null || this.embedding.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (Float value : this.embedding) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }
    
    // 领域规则验证
    
    /**
     * 验证是否可以用于检索
     */
    public boolean canBeUsedForRetrieval() {
        return isValid() && !isDeleted();
    }
    
    /**
     * 验证向量模型是否匹配
     */
    public boolean isModelCompatible(String targetModel) {
        return this.embeddingModel != null && this.embeddingModel.equals(targetModel);
    }
    
    /**
     * 创建新向量存储
     */
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 更新向量存储
     */
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 标记为已删除
     */
    public void markDeleted() {
        this.deleted = 1;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return this.deleted != null && this.deleted == 1;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Integer getDeleted() {
        return deleted;
    }
    
    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
    
    public Long getDocumentUnitId() {
        return documentUnitId;
    }
    
    public void setDocumentUnitId(Long documentUnitId) {
        this.documentUnitId = documentUnitId;
    }
    
    public String getEmbeddingModel() {
        return embeddingModel;
    }
    
    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }
    
    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }
    
    public List<Float> getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
        if (embedding != null) {
            this.embeddingDimension = embedding.size();
        }
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

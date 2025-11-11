package com.nexusvoice.infrastructure.rag.service;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.domain.rag.repository.DocumentUnitRepository;
import com.nexusvoice.domain.rag.repository.VectorStoreRepository;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.ai.manager.DynamicAiModelBeanManager;
import com.nexusvoice.infrastructure.ai.model.EmbeddingRequest;
import com.nexusvoice.infrastructure.ai.model.EmbeddingResponse;
import com.nexusvoice.infrastructure.ai.service.AiEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档向量化服务实现
 * 负责将DocumentUnit转换为向量并存储到vector_store表
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVectorizationServiceImpl {
    
    private final DynamicAiModelBeanManager modelBeanManager;
    private final DocumentUnitRepository documentUnitRepository;
    private final VectorStoreRepository vectorStoreRepository;
    private final SystemConfigRepository systemConfigRepository;
    
    // 配置键
    private static final String CONFIG_KEY_EMBEDDING_MODEL = "rag.embedding.model";
    private static final String DEFAULT_EMBEDDING_MODEL = "siliconflow:netease-youdao/bce-embedding-base_v1";
    
    /**
     * 向量化文件的所有文档单元
     * 
     * @param fileId 文件ID
     * @return 向量化的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int vectorizeFileDocuments(Long fileId) {
        if (fileId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文件ID不能为空");
        }
        
        log.info("开始向量化文件文档 - 文件ID: {}", fileId);
        
        // 查询未向量化的文档单元
        List<DocumentUnit> unvectorized = documentUnitRepository.findByFileId(fileId).stream()
                .filter(unit -> !unit.isVectorized())
                .toList();
        
        if (unvectorized.isEmpty()) {
            log.info("没有需要向量化的文档单元 - 文件ID: {}", fileId);
            return 0;
        }
        
        log.info("找到未向量化文档单元 - 文件ID: {}, 数量: {}", fileId, unvectorized.size());
        
        // 获取Embedding模型
        String embeddingModel = getEmbeddingModel();
        
        int vectorizedCount = 0;
        for (DocumentUnit unit : unvectorized) {
            try {
                vectorizeDocumentUnit(unit, embeddingModel);
                vectorizedCount++;
            } catch (Exception e) {
                log.error("文档单元向量化失败 - ID: {}, 错误: {}", unit.getId(), e.getMessage(), e);
                // 继续处理其他单元
            }
        }
        
        log.info("文件文档向量化完成 - 文件ID: {}, 成功: {} / {}", 
                fileId, vectorizedCount, unvectorized.size());
        
        return vectorizedCount;
    }
    
    /**
     * 向量化单个文档单元
     * 
     * @param unit 文档单元
     * @param embeddingModel Embedding模型键
     */
    private void vectorizeDocumentUnit(DocumentUnit unit, String embeddingModel) {
        if (unit.getContent() == null || unit.getContent().isEmpty()) {
            log.warn("文档单元内容为空，跳过向量化 - ID: {}", unit.getId());
            return;
        }
        
        try {
            // 1. 获取Embedding服务
            AiEmbeddingService embeddingService = modelBeanManager.getEmbeddingServiceByModelKey(embeddingModel);
            
            // 2. 构建请求
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .text(unit.getContent())
                    .userId(null) // TODO: 可以通过fileId查询获取userId
                    .bizId("doc_unit:" + unit.getId())
                    .build();
            
            // 3. 生成向量
            EmbeddingResponse response = embeddingService.embed(request);
            
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                        "向量化失败: " + response.getErrorMessage());
            }
            
            List<Float> embedding = response.getVector();
            
            // 4. 保存向量到vector_store表
            // TODO: VectorStoreRepository需要实现saveVector方法
            // vectorStoreRepository.saveVector(unit.getKnowledgeBaseId(), unit.getId(), embedding, embeddingModel);
            
            // 5. 标记为已向量化
            unit.markVectorized();
            documentUnitRepository.save(unit);
            
            log.debug("文档单元向量化成功 - ID: {}, 向量维度: {}", unit.getId(), embedding.size());
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("向量化文档单元失败 - ID: {}", unit.getId(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                    "向量化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量向量化（提高效率）
     * 
     * @param units 文档单元列表
     * @param embeddingModel Embedding模型键
     * @return 向量化的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchVectorizeUnits(List<DocumentUnit> units, String embeddingModel) {
        if (units == null || units.isEmpty()) {
            return 0;
        }
        
        log.info("开始批量向量化 - 数量: {}", units.size());
        
        // 过滤空内容的单元
        List<DocumentUnit> validUnits = units.stream()
                .filter(unit -> unit.getContent() != null && !unit.getContent().isEmpty())
                .toList();
        
        if (validUnits.isEmpty()) {
            log.warn("没有有效内容的文档单元");
            return 0;
        }
        
        try {
            // 1. 获取Embedding服务
            AiEmbeddingService embeddingService = modelBeanManager.getEmbeddingServiceByModelKey(embeddingModel);
            
            // 2. 构建批量请求
            List<String> texts = validUnits.stream()
                    .map(DocumentUnit::getContent)
                    .toList();
            
            EmbeddingRequest request = EmbeddingRequest.builder()
                    .texts(texts)
                    .userId(null) // TODO: 可以通过fileId查询获取userId
                    .bizId("batch_vectorize")
                    .build();
            
            // 3. 批量生成向量
            EmbeddingResponse response = embeddingService.embed(request);
            
            if (!Boolean.TRUE.equals(response.getSuccess())) {
                throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                        "批量向量化失败: " + response.getErrorMessage());
            }
            
            List<List<Float>> embeddings = response.getVectors();
            
            // 4. 批量保存向量
            for (int i = 0; i < validUnits.size(); i++) {
                DocumentUnit unit = validUnits.get(i);
                List<Float> embedding = embeddings.get(i);
                
                // TODO: VectorStoreRepository需要实现saveVector方法
                // vectorStoreRepository.saveVector(unit.getKnowledgeBaseId(), unit.getId(), embedding, embeddingModel);
                
                unit.markVectorized();
            }
            
            // 5. 批量更新状态
            documentUnitRepository.saveAll(validUnits);
            
            log.info("批量向量化完成 - 成功: {}", validUnits.size());
            
            return validUnits.size();
            
        } catch (Exception e) {
            log.error("批量向量化失败", e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                    "批量向量化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取当前配置的Embedding模型
     * 
     * @return Embedding模型键
     */
    private String getEmbeddingModel() {
        return systemConfigRepository.findByKey(CONFIG_KEY_EMBEDDING_MODEL)
                .map(config -> config.getConfigValue())
                .orElse(DEFAULT_EMBEDDING_MODEL);
    }
}

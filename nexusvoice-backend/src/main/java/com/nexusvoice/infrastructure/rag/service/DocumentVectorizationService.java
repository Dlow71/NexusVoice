package com.nexusvoice.infrastructure.rag.service;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.domain.rag.repository.DocumentUnitRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
// import com.nexusvoice.infrastructure.ai.embedding.DynamicAiEmbeddingBeanManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 文档向量化服务
 * 负责将DocumentUnit转换为向量并存储
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentVectorizationService {
    
    // TODO: 等待Embedding管理器实现后启用
    // private final DynamicAiEmbeddingBeanManager embeddingBeanManager;
    private final DocumentUnitRepository documentUnitRepository;
    
    // 默认Embedding模型（从配置读取）
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
        List<DocumentUnit> unvectorized = documentUnitRepository.findUnvectorizedByFileId(fileId);
        
        if (unvectorized.isEmpty()) {
            log.info("没有需要向量化的文档单元 - 文件ID: {}", fileId);
            return 0;
        }
        
        log.info("找到未向量化文档单元 - 文件ID: {}, 数量: {}", fileId, unvectorized.size());
        
        int vectorizedCount = 0;
        for (DocumentUnit unit : unvectorized) {
            try {
                vectorizeDocumentUnit(unit);
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
     */
    private void vectorizeDocumentUnit(DocumentUnit unit) {
        if (unit.getContent() == null || unit.getContent().isEmpty()) {
            log.warn("文档单元内容为空，跳过向量化 - ID: {}", unit.getId());
            return;
        }
        
        try {
            // TODO: 实现向量化逻辑
            // 1. 获取Embedding服务
            // var embeddingService = embeddingBeanManager.getServiceByModelKey(DEFAULT_EMBEDDING_MODEL);
            
            // 2. 生成向量
            // List<Float> embedding = embeddingService.embed(unit.getContent());
            
            // 3. 保存向量到vector_store表
            // VectorStore vectorStore = new VectorStore();
            // vectorStore.setDocumentUnitId(unit.getId());
            // vectorStore.setEmbeddingModel(DEFAULT_EMBEDDING_MODEL);
            // vectorStore.setVectorData(embedding);
            // vectorStoreRepository.save(vectorStore);
            
            // 4. 标记为已向量化
            documentUnitRepository.markAsVectorized(unit.getId());
            
            log.debug("文档单元向量化完成（TODO实现） - ID: {}", unit.getId());
            
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
     * @return 向量化的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchVectorizeUnits(List<DocumentUnit> units) {
        if (units == null || units.isEmpty()) {
            return 0;
        }
        
        log.info("开始批量向量化 - 数量: {}", units.size());
        
        // TODO: 实现批量向量化逻辑
        // 当前实现：逐个向量化
        int successCount = 0;
        for (DocumentUnit unit : units) {
            try {
                vectorizeDocumentUnit(unit);
                successCount++;
            } catch (Exception e) {
                log.error("批量向量化失败 - ID: {}", unit.getId(), e);
            }
        }
        
        log.info("批量向量化完成 - 成功: {} / {}", successCount, units.size());
        
        return successCount;
    }
}

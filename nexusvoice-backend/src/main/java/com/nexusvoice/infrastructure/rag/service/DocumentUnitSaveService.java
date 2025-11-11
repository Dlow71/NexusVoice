package com.nexusvoice.infrastructure.rag.service;

import com.nexusvoice.domain.rag.model.entity.DocumentUnit;
import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.rag.model.vo.ProcessedSegment;
import com.nexusvoice.domain.rag.repository.DocumentUnitRepository;
import com.nexusvoice.domain.rag.repository.FileDetailRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档单元保存服务
 * 负责将ProcessedSegment转换为DocumentUnit并批量保存
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUnitSaveService {
    
    private final DocumentUnitRepository documentUnitRepository;
    private final FileDetailRepository fileDetailRepository;
    
    /**
     * 批量保存段落
     * 
     * @param fileDetail 文件详情
     * @param segments 处理后的段落列表
     * @return 保存的DocumentUnit数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int saveSegments(FileDetail fileDetail, List<ProcessedSegment> segments) {
        if (fileDetail == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文件详情不能为空");
        }
        
        if (segments == null || segments.isEmpty()) {
            log.warn("段落列表为空，跳过保存 - 文件ID: {}", fileDetail.getId());
            return 0;
        }
        
        log.info("开始保存文档单元 - 文件ID: {}, 段落数: {}", 
                fileDetail.getId(), segments.size());
        
        try {
            // 1. 为段落设置order（如果没有）
            ensureSegmentOrder(segments);
            
            // 2. 转换为DocumentUnit实体列表
            List<DocumentUnit> documentUnits = convertToDocumentUnits(fileDetail, segments);
            
            // 3. 批量保存到数据库
            List<DocumentUnit> savedUnits = documentUnitRepository.saveAll(documentUnits);
            
            // 4. 更新FileDetail的文档单元数量
            updateFileDetailStats(fileDetail, savedUnits);
            
            log.info("文档单元保存成功 - 文件ID: {}, 保存数量: {}", 
                    fileDetail.getId(), savedUnits.size());
            
            return savedUnits.size();
            
        } catch (Exception e) {
            log.error("保存文档单元失败 - 文件ID: {}", fileDetail.getId(), e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                    "保存文档单元失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 确保每个段落都有order
     */
    private void ensureSegmentOrder(List<ProcessedSegment> segments) {
        for (int i = 0; i < segments.size(); i++) {
            if (segments.get(i).getOrder() == null) {
                segments.get(i).setOrder(i);
            }
        }
    }
    
    /**
     * 转换为DocumentUnit实体列表
     */
    private List<DocumentUnit> convertToDocumentUnits(FileDetail fileDetail, List<ProcessedSegment> segments) {
        List<DocumentUnit> documentUnits = new ArrayList<>(segments.size());
        
        for (ProcessedSegment segment : segments) {
            // 使用ProcessedSegment的toDocumentUnit方法转换
            DocumentUnit unit = segment.toDocumentUnit(fileDetail.getId(), 1); // 默认页码为1
            
            // 设置额外字段
            unit.setFileId(fileDetail.getId());
            unit.setChunkIndex(segment.getOrder());
            unit.setParagraphIndex(segment.getOrder());
            
            // 设置单元类型
            if (segment.getType() != null) {
                unit.setUnitType(segment.getType().name());
            }
            
            // 初始化向量化状态
            unit.setIsVector(false);
            unit.setIsOcr(false);
            
            // 设置字符数
            unit.setCharCount(segment.getContent() != null ? segment.length() : 0);
            
            // TODO: 计算Token数（需要Tokenizer服务）
            // unit.setTokenCount(calculateTokenCount(segment.getContent()));
            
            documentUnits.add(unit);
        }
        
        log.debug("转换为DocumentUnit完成 - 数量: {}", documentUnits.size());
        return documentUnits;
    }
    
    /**
     * 更新FileDetail的统计信息
     */
    private void updateFileDetailStats(FileDetail fileDetail, List<DocumentUnit> savedUnits) {
        // 统计总字符数
        long totalCharCount = savedUnits.stream()
                .mapToLong(unit -> unit.getCharCount() != null ? unit.getCharCount() : 0)
                .sum();
        
        // 统计总Token数
        long totalTokenCount = savedUnits.stream()
                .mapToLong(unit -> unit.getTokenCount() != null ? unit.getTokenCount() : 0)
                .sum();
        
        log.info("更新文件统计 - 文件ID: {}, 文档单元数: {}, 总字符数: {}, 总Token数: {}", 
                fileDetail.getId(), savedUnits.size(), totalCharCount, totalTokenCount);
        
        // TODO: 如果FileDetail有这些字段，更新它们
        // fileDetail.setDocumentUnitCount(savedUnits.size());
        // fileDetail.setTotalCharCount(totalCharCount);
        // fileDetail.setTotalTokenCount(totalTokenCount);
        // fileDetailRepository.save(fileDetail);
    }
    
    /**
     * 删除文件的所有文档单元
     * 
     * @param fileId 文件ID
     * @return 删除的数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteByFileId(Long fileId) {
        if (fileId == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文件ID不能为空");
        }
        
        log.info("删除文件的所有文档单元 - 文件ID: {}", fileId);
        
        int deletedCount = documentUnitRepository.deleteByFileId(fileId);
        
        log.info("文档单元删除完成 - 文件ID: {}, 删除数量: {}", fileId, deletedCount);
        
        return deletedCount;
    }
    
    /**
     * 统计文件的文档单元数量
     * 
     * @param fileId 文件ID
     * @return 文档单元数量
     */
    public int countByFileId(Long fileId) {
        if (fileId == null) {
            return 0;
        }
        
        return documentUnitRepository.countByFileId(fileId);
    }
}

package com.nexusvoice.infrastructure.rag.service;

import com.nexusvoice.domain.rag.model.entity.FileDetail;
import com.nexusvoice.domain.storage.repository.StorageRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.repository.storage.StorageStrategyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * 文件存储服务
 * 负责从存储服务（MinIO/七牛云）加载文件内容
 * 
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    
    private final StorageStrategyManager storageStrategyManager;
    
    /**
     * 加载文件内容
     * 
     * @param fileDetail 文件详情
     * @return 文件字节数组
     */
    public byte[] loadFileContent(FileDetail fileDetail) {
        if (fileDetail == null) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文件详情不能为空");
        }
        
        String filePath = fileDetail.getFilePath();
        if (filePath == null || filePath.isEmpty()) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, "文件路径为空");
        }
        
        log.info("开始加载文件内容 - 文件ID: {}, 文件名: {}, 存储路径: {}", 
                fileDetail.getId(), fileDetail.getOriginalName(), filePath);
        
        try {
            // 1. 获取存储服务（根据文件的存储类型，如果没有则使用默认）
            StorageRepository storageRepository = getStorageRepository(fileDetail);
            
            // 2. 下载文件
            try (InputStream inputStream = storageRepository.download(filePath)) {
                if (inputStream == null) {
                    throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                            "文件下载失败：输入流为空 - 文件路径: " + filePath);
                }
                
                // 3. 读取字节
                byte[] fileContent = readAllBytes(inputStream);
                
                log.info("文件内容加载成功 - 文件ID: {}, 大小: {} 字节", 
                        fileDetail.getId(), fileContent.length);
                
                return fileContent;
                
            } catch (Exception e) {
                throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                        "读取文件流失败: " + e.getMessage(), e);
            }
            
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("加载文件内容失败 - 文件ID: {}, 文件路径: {}", 
                    fileDetail.getId(), filePath, e);
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, 
                    "加载文件内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取存储服务
     */
    private StorageRepository getStorageRepository(FileDetail fileDetail) {
        // StorageStrategyManager内部已处理备用存储逻辑
        // getDefaultRepository()会自动尝试备用存储
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        
        if (repository == null) {
            throw new BizException(ErrorCodeEnum.SYSTEM_ERROR, "未配置存储服务");
        }
        
        log.debug("使用存储服务: {}", repository.getProvider());
        return repository;
    }
    
    /**
     * 从InputStream读取所有字节
     */
    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192]; // 8KB缓冲区
        int bytesRead;
        
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        
        buffer.flush();
        return buffer.toByteArray();
    }
    
    /**
     * 检查文件是否存在
     * 
     * @param fileDetail 文件详情
     * @return 是否存在
     */
    public boolean fileExists(FileDetail fileDetail) {
        if (fileDetail == null || fileDetail.getFilePath() == null) {
            return false;
        }
        
        try {
            StorageRepository repository = getStorageRepository(fileDetail);
            return repository.exists(fileDetail.getFilePath());
        } catch (Exception e) {
            log.error("检查文件存在性失败 - 文件ID: {}", fileDetail.getId(), e);
            return false;
        }
    }
}

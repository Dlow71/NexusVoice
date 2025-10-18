package com.nexusvoice.infrastructure.repository.storage;

import com.nexusvoice.domain.storage.model.StorageConfig;
import com.nexusvoice.domain.storage.model.UploadResult;
import com.nexusvoice.domain.storage.repository.StorageRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 抽象存储仓储实现
 * 使用模板方法模式定义存储操作的基本流程
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Slf4j
public abstract class AbstractStorageRepository<T extends StorageConfig> implements StorageRepository {
    
    protected T config;
    
    protected AbstractStorageRepository(T config) {
        this.config = config;
    }
    
    @Override
    public UploadResult upload(MultipartFile file, FileTypeEnum fileType) {
        return upload(file, fileType, null);
    }
    
    @Override
    public UploadResult upload(byte[] data, String fileName, FileTypeEnum fileType) {
        try (InputStream inputStream = new ByteArrayInputStream(data)) {
            return upload(inputStream, fileName, data.length, fileType);
        } catch (IOException e) {
            log.error("上传文件失败，文件名：{}，错误：{}", fileName, e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.FILE_UPLOAD_FAILED, "文件上传失败：" + e.getMessage());
        }
    }
    
    @Override
    public UploadResult upload(MultipartFile file, FileTypeEnum fileType, Map<String, String> metadata) {
        // 参数校验
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.FILE_IS_EMPTY, "文件不能为空");
        }
        
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.FILE_NAME_INVALID, "文件名不能为空");
        }
        
        long startTime = System.currentTimeMillis();
        log.info("开始上传文件，文件名：{}，大小：{}，类型：{}，存储提供商：{}", 
                originalFileName, file.getSize(), fileType, getProvider());
        
        try {
            // 1. 生成文件Key
            String fileKey = generateFileKey(originalFileName, fileType);
            
            // 2. 准备元数据
            Map<String, String> finalMetadata = prepareMetadata(file, fileType, metadata);
            
            // 3. 执行上传（由子类实现）
            UploadResult result = doUpload(file.getInputStream(), fileKey, file.getSize(), 
                    file.getContentType(), finalMetadata);
            
            // 4. 填充结果信息
            result.setOriginalFileName(originalFileName);
            result.setFileType(fileType);
            result.setMimeType(file.getContentType());
            result.setUploadDuration(System.currentTimeMillis() - startTime);
            
            if (result.getSuccess()) {
                log.info("文件上传成功，文件Key：{}，URL：{}，耗时：{}ms", 
                        result.getFileKey(), result.getFileUrl(), result.getUploadDuration());
            } else {
                log.error("文件上传失败，文件名：{}，错误：{}", originalFileName, result.getErrorMessage());
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("文件上传异常，文件名：{}，错误：{}", originalFileName, e.getMessage(), e);
            return UploadResult.failure(e.getMessage(), getProvider());
        }
    }
    
    @Override
    public UploadResult upload(InputStream inputStream, String fileName, long fileSize, FileTypeEnum fileType) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("originalFileName", fileName);
        metadata.put("fileSize", String.valueOf(fileSize));
        
        String fileKey = generateFileKey(fileName, fileType);
        
        try {
            UploadResult result = doUpload(inputStream, fileKey, fileSize, null, metadata);
            result.setOriginalFileName(fileName);
            result.setFileType(fileType);
            return result;
        } catch (Exception e) {
            log.error("文件上传失败，文件名：{}，错误：{}", fileName, e.getMessage(), e);
            return UploadResult.failure(e.getMessage(), getProvider());
        }
    }
    
    @Override
    public List<String> batchDelete(List<String> fileKeys) {
        List<String> deletedKeys = new ArrayList<>();
        for (String fileKey : fileKeys) {
            try {
                if (delete(fileKey)) {
                    deletedKeys.add(fileKey);
                }
            } catch (Exception e) {
                log.error("删除文件失败，fileKey：{}，错误：{}", fileKey, e.getMessage());
            }
        }
        return deletedKeys;
    }
    
    @Override
    public boolean move(String sourceKey, String targetKey) {
        // 默认实现：复制后删除
        if (copy(sourceKey, targetKey)) {
            return delete(sourceKey);
        }
        return false;
    }
    
    @Override
    public StorageConfig getConfig() {
        return config;
    }
    
    @Override
    public void updateConfig(StorageConfig config) {
        if (config != null && config.getClass().isAssignableFrom(this.config.getClass())) {
            this.config = (T) config;
            // 重新初始化
            initialize();
        } else {
            throw new IllegalArgumentException("配置类型不匹配");
        }
    }
    
    /**
     * 生成文件Key
     *
     * @param originalFileName 原始文件名
     * @param fileType 文件类型
     * @return 文件Key
     */
    protected String generateFileKey(String originalFileName, FileTypeEnum fileType) {
        String directory = config.getDirectory(fileType.getCode());
        String extension = "";
        
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        return directory + uniqueId + extension;
    }
    
    /**
     * 准备元数据
     *
     * @param file 文件
     * @param fileType 文件类型
     * @param customMetadata 自定义元数据
     * @return 合并后的元数据
     */
    protected Map<String, String> prepareMetadata(MultipartFile file, FileTypeEnum fileType, 
                                                  Map<String, String> customMetadata) {
        Map<String, String> metadata = new HashMap<>();
        
        // 基本元数据
        metadata.put("originalFileName", file.getOriginalFilename());
        metadata.put("fileType", fileType.getCode());
        metadata.put("fileSize", String.valueOf(file.getSize()));
        metadata.put("contentType", file.getContentType());
        metadata.put("uploadTime", String.valueOf(System.currentTimeMillis()));
        
        // 合并自定义元数据
        if (customMetadata != null) {
            metadata.putAll(customMetadata);
        }
        
        return metadata;
    }
    
    /**
     * 执行实际的上传操作（由子类实现）
     *
     * @param inputStream 文件输入流
     * @param fileKey 文件Key
     * @param fileSize 文件大小
     * @param contentType 内容类型
     * @param metadata 元数据
     * @return 上传结果
     * @throws Exception 上传异常
     */
    protected abstract UploadResult doUpload(InputStream inputStream, String fileKey, 
                                            long fileSize, String contentType, 
                                            Map<String, String> metadata) throws Exception;
}

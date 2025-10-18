package com.nexusvoice.application.file.service;

import com.nexusvoice.domain.storage.enums.StorageProvider;
import com.nexusvoice.domain.storage.model.UploadResult;
import com.nexusvoice.domain.storage.repository.StorageRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.infrastructure.repository.storage.StorageStrategyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

/**
 * 统一文件上传服务
 * 提供统一的文件上传接口，支持多种存储提供商
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Slf4j
@Service
public class UnifiedFileUploadService {
    
    @Autowired
    private StorageStrategyManager storageStrategyManager;
    
    /**
     * 使用默认存储上传文件（自动识别文件类型）
     *
     * @param file 文件
     * @return 文件访问URL
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.FILE_IS_EMPTY, "文件不能为空");
        }
        
        String originalFilename = file.getOriginalFilename();
        FileTypeEnum fileType = FileTypeEnum.getFileTypeByFileName(originalFilename);
        
        return upload(file, fileType);
    }
    
    /**
     * 使用默认存储上传文件（指定文件类型）
     *
     * @param file 文件
     * @param fileType 文件类型
     * @return 文件访问URL
     */
    public String upload(MultipartFile file, FileTypeEnum fileType) {
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        UploadResult result = repository.upload(file, fileType);
        
        if (!result.getSuccess()) {
            log.error("文件上传失败：{}", result.getErrorMessage());
            throw BizException.of(ErrorCodeEnum.FILE_UPLOAD_FAILED, 
                    "文件上传失败：" + result.getErrorMessage());
        }
        
        return result.getFileUrl();
    }
    
    /**
     * 使用指定存储类型上传文件
     *
     * @param file 文件
     * @param storageProvider 存储提供商
     * @return 文件访问URL
     */
    public String upload(MultipartFile file, StorageProvider storageProvider) {
        String originalFilename = file.getOriginalFilename();
        FileTypeEnum fileType = FileTypeEnum.getFileTypeByFileName(originalFilename);
        
        return upload(file, fileType, storageProvider);
    }
    
    /**
     * 使用指定存储类型上传文件
     *
     * @param file 文件
     * @param fileType 文件类型
     * @param storageProvider 存储提供商
     * @return 文件访问URL
     */
    public String upload(MultipartFile file, FileTypeEnum fileType, StorageProvider storageProvider) {
        StorageRepository repository = storageStrategyManager.getRepository(storageProvider);
        UploadResult result = repository.upload(file, fileType);
        
        if (!result.getSuccess()) {
            log.error("文件上传失败：{}", result.getErrorMessage());
            throw BizException.of(ErrorCodeEnum.FILE_UPLOAD_FAILED, 
                    "文件上传失败：" + result.getErrorMessage());
        }
        
        return result.getFileUrl();
    }
    
    /**
     * 上传文件（带元数据）
     *
     * @param file 文件
     * @param fileType 文件类型
     * @param metadata 元数据
     * @return 文件访问URL
     */
    public String upload(MultipartFile file, FileTypeEnum fileType, Map<String, String> metadata) {
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        UploadResult result = repository.upload(file, fileType, metadata);
        
        if (!result.getSuccess()) {
            log.error("文件上传失败：{}", result.getErrorMessage());
            throw BizException.of(ErrorCodeEnum.FILE_UPLOAD_FAILED, 
                    "文件上传失败：" + result.getErrorMessage());
        }
        
        return result.getFileUrl();
    }
    
    /**
     * 上传文件（返回完整结果）
     *
     * @param file 文件
     * @param fileType 文件类型
     * @return 上传结果
     */
    public UploadResult uploadWithResult(MultipartFile file, FileTypeEnum fileType) {
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        UploadResult result = repository.upload(file, fileType);
        
        if (!result.getSuccess()) {
            log.error("文件上传失败：{}", result.getErrorMessage());
            throw BizException.of(ErrorCodeEnum.FILE_UPLOAD_FAILED, 
                    "文件上传失败：" + result.getErrorMessage());
        }
        
        return result;
    }
    
    /**
     * 删除文件
     *
     * @param fileKey 文件Key
     * @return 是否删除成功
     */
    public boolean delete(String fileKey) {
        try {
            StorageRepository repository = storageStrategyManager.getDefaultRepository();
            return repository.delete(fileKey);
        } catch (Exception e) {
            log.error("删除文件失败，fileKey：{}，错误：{}", fileKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查文件是否存在
     *
     * @param fileKey 文件Key
     * @return 是否存在
     */
    public boolean exists(String fileKey) {
        try {
            StorageRepository repository = storageStrategyManager.getDefaultRepository();
            return repository.exists(fileKey);
        } catch (Exception e) {
            log.error("检查文件是否存在失败，fileKey：{}，错误：{}", fileKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取文件访问URL
     *
     * @param fileKey 文件Key
     * @return 文件访问URL
     */
    public String getFileUrl(String fileKey) {
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        return repository.getFileUrl(fileKey);
    }
    
    /**
     * 获取文件预签名URL
     *
     * @param fileKey 文件Key
     * @param expireSeconds 过期时间（秒）
     * @return 预签名URL
     */
    public String getPresignedUrl(String fileKey, int expireSeconds) {
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        return repository.getPresignedUrl(fileKey, expireSeconds);
    }
    
    /**
     * 下载文件
     *
     * @param fileKey 文件Key
     * @return 文件输入流
     */
    public InputStream download(String fileKey) {
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        return repository.download(fileKey);
    }
    
    /**
     * 获取文件信息
     *
     * @param fileKey 文件Key
     * @return 文件信息
     */
    public Map<String, Object> getFileInfo(String fileKey) {
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        return repository.getFileInfo(fileKey);
    }
    
    /**
     * 复制文件
     *
     * @param sourceKey 源文件Key
     * @param targetKey 目标文件Key
     * @return 是否复制成功
     */
    public boolean copy(String sourceKey, String targetKey) {
        try {
            StorageRepository repository = storageStrategyManager.getDefaultRepository();
            return repository.copy(sourceKey, targetKey);
        } catch (Exception e) {
            log.error("复制文件失败，源：{}，目标：{}，错误：{}", 
                    sourceKey, targetKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 移动文件
     *
     * @param sourceKey 源文件Key
     * @param targetKey 目标文件Key
     * @return 是否移动成功
     */
    public boolean move(String sourceKey, String targetKey) {
        try {
            StorageRepository repository = storageStrategyManager.getDefaultRepository();
            return repository.move(sourceKey, targetKey);
        } catch (Exception e) {
            log.error("移动文件失败，源：{}，目标：{}，错误：{}", 
                    sourceKey, targetKey, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取当前存储提供商
     *
     * @return 存储提供商
     */
    public StorageProvider getCurrentProvider() {
        StorageRepository repository = storageStrategyManager.getDefaultRepository();
        return repository.getProvider();
    }
    
    /**
     * 获取所有存储提供商状态
     *
     * @return 提供商状态映射
     */
    public Map<StorageProvider, Boolean> getAllProviderStatus() {
        return storageStrategyManager.getAllProviderStatus();
    }
    
    /**
     * 重新加载存储配置
     */
    public void reloadConfiguration() {
        storageStrategyManager.reloadConfiguration();
        log.info("存储配置重新加载完成");
    }
}

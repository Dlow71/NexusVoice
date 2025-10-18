package com.nexusvoice.infrastructure.repository.storage;

import com.nexusvoice.domain.storage.enums.StorageProvider;
import com.nexusvoice.domain.storage.model.QiniuStorageConfig;
import com.nexusvoice.domain.storage.model.UploadResult;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.*;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.qiniu.util.Json;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 七牛云存储仓储实现
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Slf4j
public class QiniuStorageRepositoryImpl extends AbstractStorageRepository<QiniuStorageConfig> {
    
    private Auth auth;
    private UploadManager uploadManager;
    private BucketManager bucketManager;
    private Configuration qiniuConfig;
    
    public QiniuStorageRepositoryImpl(QiniuStorageConfig config) {
        super(config);
        initialize();
    }
    
    @Override
    public void initialize() {
        if (!config.isValid()) {
            log.error("七牛云配置无效：{}", config.getDescription());
            return;
        }
        
        try {
            // 创建认证对象
            auth = Auth.create(config.getAccessKey(), config.getSecretKey());
            
            // 配置存储区域
            Zone zone = getZone(config.getRegion());
            qiniuConfig = new Configuration(zone);
            qiniuConfig.connectTimeout = config.getConnectTimeout();
            qiniuConfig.readTimeout = config.getReadTimeout();
            qiniuConfig.writeTimeout = config.getWriteTimeout();
            qiniuConfig.retryMax = config.getMaxRetries();
            
            // 创建上传管理器
            uploadManager = new UploadManager(qiniuConfig);
            
            // 创建存储空间管理器
            bucketManager = new BucketManager(auth, qiniuConfig);
            
            log.info("七牛云存储服务初始化成功：{}", config.getDescription());
        } catch (Exception e) {
            log.error("七牛云存储服务初始化失败", e);
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, "七牛云存储服务初始化失败：" + e.getMessage());
        }
    }
    
    @Override
    protected UploadResult doUpload(InputStream inputStream, String fileKey, long fileSize, 
                                   String contentType, Map<String, String> metadata) throws Exception {
        // 生成上传凭证
        String upToken = auth.uploadToken(config.getBucket(), fileKey, 
                config.getUploadTokenExpires(), null);
        
        try {
            // 执行上传
            Response response = uploadManager.put(inputStream, fileKey, upToken, null, contentType);
            
            if (!response.isOK()) {
                log.error("七牛云上传失败，响应码：{}，错误：{}", response.statusCode, response.error);
                throw new RuntimeException("上传失败：" + response.error);
            }
            
            // 解析响应
            DefaultPutRet putRet = Json.decode(response.bodyString(), DefaultPutRet.class);
            
            // 构建文件URL
            String fileUrl = config.getFileUrl(putRet.key);
            
            // 构建上传结果
            UploadResult result = UploadResult.success(fileUrl, putRet.key, fileSize, getProvider());
            result.setEtag(putRet.hash);
            result.setFilePath(putRet.key);
            result.setBucket(config.getBucket());
            // 转换metadata类型
            Map<String, String> objectMetadata = new HashMap<>();
            if (metadata != null) {
                objectMetadata.putAll(metadata);
            }
            result.setMetadata(objectMetadata);
            
            return result;
            
        } catch (QiniuException e) {
            log.error("七牛云上传异常，fileKey：{}，错误码：{}，错误：{}", 
                    fileKey, e.code(), e.response != null ? e.response.toString() : e.getMessage());
            throw e;
        }
    }
    
    @Override
    public boolean delete(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return false;
        }
        
        try {
            Response response = bucketManager.delete(config.getBucket(), fileKey);
            boolean success = response.isOK();
            
            if (success) {
                log.info("七牛云文件删除成功，fileKey：{}", fileKey);
            } else {
                log.warn("七牛云文件删除失败，fileKey：{}，响应：{}", fileKey, response.toString());
            }
            
            return success;
        } catch (QiniuException e) {
            log.error("七牛云文件删除异常，fileKey：{}，错误：{}", fileKey, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public String getFileUrl(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        
        String baseUrl = config.getFileUrl(fileKey);
        
        // 如果是私有空间，生成带签名的URL
        if ("private".equals(config.getAccessControl())) {
            long expireInSeconds = 3600; // 默认1小时
            return auth.privateDownloadUrl(baseUrl, expireInSeconds);
        }
        
        return baseUrl;
    }
    
    @Override
    public String getPresignedUrl(String fileKey, int expireSeconds) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        
        String baseUrl = config.getFileUrl(fileKey);
        return auth.privateDownloadUrl(baseUrl, expireSeconds);
    }
    
    @Override
    public boolean exists(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return false;
        }
        
        try {
            FileInfo fileInfo = bucketManager.stat(config.getBucket(), fileKey);
            return fileInfo != null;
        } catch (QiniuException e) {
            if (e.code() == 612) { // 文件不存在
                return false;
            }
            log.error("检查文件是否存在失败，fileKey：{}，错误：{}", fileKey, e.getMessage());
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getFileInfo(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        
        try {
            FileInfo fileInfo = bucketManager.stat(config.getBucket(), fileKey);
            
            Map<String, Object> info = new HashMap<>();
            info.put("fileKey", fileKey);
            info.put("size", fileInfo.fsize);
            info.put("mimeType", fileInfo.mimeType);
            info.put("hash", fileInfo.hash);
            info.put("putTime", fileInfo.putTime);
            info.put("type", fileInfo.type);
            info.put("status", fileInfo.status);
            
            return info;
        } catch (QiniuException e) {
            log.error("获取文件信息失败，fileKey：{}，错误：{}", fileKey, e.getMessage());
            return null;
        }
    }
    
    @Override
    public InputStream download(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        
        try {
            String downloadUrl = getFileUrl(fileKey);
            return new java.net.URL(downloadUrl).openStream();
        } catch (Exception e) {
            log.error("下载文件失败，fileKey：{}，错误：{}", fileKey, e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.FILE_DOWNLOAD_FAILED, "文件下载失败：" + e.getMessage());
        }
    }
    
    @Override
    public boolean copy(String sourceKey, String targetKey) {
        if (sourceKey == null || targetKey == null) {
            return false;
        }
        
        try {
            Response response = bucketManager.copy(config.getBucket(), sourceKey, 
                    config.getBucket(), targetKey);
            boolean success = response.isOK();
            
            if (success) {
                log.info("七牛云文件复制成功，源：{}，目标：{}", sourceKey, targetKey);
            } else {
                log.warn("七牛云文件复制失败，源：{}，目标：{}，响应：{}", 
                        sourceKey, targetKey, response.toString());
            }
            
            return success;
        } catch (QiniuException e) {
            log.error("七牛云文件复制异常，源：{}，目标：{}，错误：{}", 
                    sourceKey, targetKey, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<String> listFiles(String prefix, int maxKeys) {
        List<String> fileKeys = new ArrayList<>();

        // 列举空间文件
        BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(
                config.getBucket(), prefix, maxKeys, null);

        while (fileListIterator.hasNext()) {
            FileInfo[] items = fileListIterator.next();
            for (FileInfo item : items) {
                fileKeys.add(item.key);
            }

            if (fileKeys.size() >= maxKeys) {
                break;
            }
        }

        log.debug("列出七牛云文件，前缀：{}，数量：{}", prefix, fileKeys.size());

        return fileKeys;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            // 尝试获取存储空间信息来验证服务是否可用
            bucketManager.getBucketInfo(config.getBucket());
            return true;
        } catch (Exception e) {
            log.warn("七牛云存储服务不可用：{}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public StorageProvider getProvider() {
        return StorageProvider.QINIU;
    }
    
    @Override
    public void destroy() {
        // 七牛云SDK不需要显式释放资源
        log.info("七牛云存储服务已销毁");
    }
    
    /**
     * 根据区域字符串获取Zone对象
     */
    private Zone getZone(String region) {
        if (region == null || region.isEmpty() || "auto".equalsIgnoreCase(region)) {
            return Zone.autoZone();
        }
        
        switch (region.toLowerCase()) {
            case "huadong":
            case "z0":
                return Zone.zone0(); // 华东
            case "huabei":
            case "z1":
                return Zone.zone1(); // 华北
            case "huanan":
            case "z2":
                return Zone.zone2(); // 华南
            case "beimei":
            case "na0":
                return Zone.zoneNa0(); // 北美
            case "dongnanya":
            case "as0":
                return Zone.zoneAs0(); // 东南亚
            default:
                log.warn("未知的七牛云区域：{}，使用自动选择", region);
                return Zone.autoZone();
        }
    }
}

package com.nexusvoice.infrastructure.repository.storage;

import com.nexusvoice.domain.storage.enums.StorageProvider;
import com.nexusvoice.domain.storage.model.MinioStorageConfig;
import com.nexusvoice.domain.storage.model.UploadResult;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * MinIO存储仓储实现
 *
 * @author NexusVoice Team
 * @since 2025-10-18
 */
@Slf4j
public class MinioStorageRepositoryImpl extends AbstractStorageRepository<MinioStorageConfig> {
    
    private MinioClient minioClient;
    
    public MinioStorageRepositoryImpl(MinioStorageConfig config) {
        super(config);
        initialize();
    }
    
    @Override
    public void initialize() {
        if (!config.isValid()) {
            log.error("MinIO配置无效：{}", config.getDescription());
            return;
        }
        
        try {
            // 创建MinIO客户端
            MinioClient.Builder builder = MinioClient.builder()
                    .endpoint(config.getDomain())
                    .credentials(config.getAccessKey(), config.getSecretKey());
            
            // 设置区域
            if (config.getRegion() != null && !config.getRegion().isEmpty()) {
                builder.region(config.getRegion());
            }
            
            minioClient = builder.build();
            
            // 自动创建Bucket
            if (config.getAutoCreateBucket()) {
                createBucketIfNotExists();
            }
            
            // 验证Bucket是否存在
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(config.getBucket())
                    .build());
            
            if (!bucketExists) {
                throw new RuntimeException("MinIO存储桶不存在：" + config.getBucket());
            }
            
            log.info("MinIO存储服务初始化成功：{}", config.getDescription());
            
        } catch (Exception e) {
            log.error("MinIO存储服务初始化失败", e);
            throw BizException.of(ErrorCodeEnum.SYSTEM_ERROR, "MinIO存储服务初始化失败：" + e.getMessage());
        }
    }
    
    @Override
    protected UploadResult doUpload(InputStream inputStream, String fileKey, long fileSize,
                                   String contentType, Map<String, String> metadata) throws Exception {
        try {
            // 构建上传参数
            PutObjectArgs.Builder builder = PutObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(fileKey)
                    .stream(inputStream, fileSize, config.getPartSize());
            
            // 设置内容类型
            if (contentType != null && !contentType.isEmpty()) {
                builder.contentType(contentType);
            }
            
            // 设置用户元数据
            if (metadata != null && !metadata.isEmpty()) {
                Map<String, String> userMetadata = new HashMap<>();
                metadata.forEach((k, v) -> userMetadata.put("X-Amz-Meta-" + k, v));
                builder.userMetadata(userMetadata);
            }
            
            // 执行上传
            ObjectWriteResponse response = minioClient.putObject(builder.build());
            
            // 构建文件访问URL
            String fileUrl = config.getFileUrl(fileKey);
            
            // 构建上传结果
            UploadResult result = UploadResult.success(fileUrl, fileKey, fileSize, getProvider());
            result.setEtag(response.etag());
            result.setFilePath(fileKey);
            result.setBucket(config.getBucket());
            result.setDomain(config.getActualPublicDomain());
            result.setRegion(config.getRegion());
            result.setMetadata(metadata);
            
            if (response.versionId() != null) {
                result.getMetadata().put("versionId", response.versionId());
            }
            
            log.debug("MinIO文件上传成功，fileKey：{}，ETag：{}", fileKey, response.etag());
            
            return result;
            
        } catch (Exception e) {
            log.error("MinIO文件上传失败，fileKey：{}，错误：{}", fileKey, e.getMessage(), e);
            throw e;
        }
    }
    
    @Override
    public boolean delete(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return false;
        }
        
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(fileKey)
                    .build());
            
            log.info("MinIO文件删除成功，fileKey：{}", fileKey);
            return true;
            
        } catch (Exception e) {
            log.error("MinIO文件删除失败，fileKey：{}，错误：{}", fileKey, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<String> batchDelete(List<String> fileKeys) {
        if (fileKeys == null || fileKeys.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // 构建删除对象列表
            List<DeleteObject> deleteObjects = fileKeys.stream()
                    .map(DeleteObject::new)
                    .collect(Collectors.toList());
            
            // 执行批量删除
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(config.getBucket())
                            .objects(deleteObjects)
                            .build());
            
            // 收集删除失败的文件
            Set<String> failedKeys = new HashSet<>();
            for (Result<DeleteError> result : results) {
                DeleteError error = result.get();
                failedKeys.add(error.objectName());
                log.error("MinIO批量删除失败，fileKey：{}，错误：{}", 
                        error.objectName(), error.message());
            }
            
            // 返回删除成功的文件
            return fileKeys.stream()
                    .filter(key -> !failedKeys.contains(key))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("MinIO批量删除异常，错误：{}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public String getFileUrl(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        
        // 如果是公开访问，直接返回URL
        if ("public-read".equals(config.getBucketPolicy())) {
            return config.getFileUrl(fileKey);
        }
        
        // 否则生成预签名URL
        return getPresignedUrl(fileKey, config.getPresignedUrlExpiry());
    }
    
    @Override
    public String getPresignedUrl(String fileKey, int expireSeconds) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(config.getBucket())
                            .object(fileKey)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build());
            
            log.debug("生成MinIO预签名URL，fileKey：{}，过期时间：{}秒", fileKey, expireSeconds);
            return url;
            
        } catch (Exception e) {
            log.error("生成MinIO预签名URL失败，fileKey：{}，错误：{}", fileKey, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public boolean exists(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return false;
        }
        
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(fileKey)
                    .build());
            return true;
            
        } catch (ErrorResponseException e) {
            if (e.errorResponse().code().equals("NoSuchKey")) {
                return false;
            }
            log.error("检查MinIO文件是否存在失败，fileKey：{}，错误：{}", fileKey, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("检查MinIO文件是否存在异常，fileKey：{}，错误：{}", fileKey, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public Map<String, Object> getFileInfo(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(fileKey)
                    .build());
            
            Map<String, Object> info = new HashMap<>();
            info.put("fileKey", fileKey);
            info.put("size", stat.size());
            info.put("etag", stat.etag());
            info.put("contentType", stat.contentType());
            info.put("lastModified", stat.lastModified());
            
            // 添加用户元数据
            if (stat.userMetadata() != null) {
                info.put("userMetadata", stat.userMetadata());
            }
            
            return info;
            
        } catch (Exception e) {
            log.error("获取MinIO文件信息失败，fileKey：{}，错误：{}", fileKey, e.getMessage(), e);
            return null;
        }
    }
    
    @Override
    public InputStream download(String fileKey) {
        if (fileKey == null || fileKey.isEmpty()) {
            return null;
        }
        
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(fileKey)
                    .build());
            
        } catch (Exception e) {
            log.error("下载MinIO文件失败，fileKey：{}，错误：{}", fileKey, e.getMessage(), e);
            throw BizException.of(ErrorCodeEnum.FILE_DOWNLOAD_FAILED, "文件下载失败：" + e.getMessage());
        }
    }
    
    @Override
    public boolean copy(String sourceKey, String targetKey) {
        if (sourceKey == null || targetKey == null) {
            return false;
        }
        
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(config.getBucket())
                    .object(targetKey)
                    .source(CopySource.builder()
                            .bucket(config.getBucket())
                            .object(sourceKey)
                            .build())
                    .build());
            
            log.info("MinIO文件复制成功，源：{}，目标：{}", sourceKey, targetKey);
            return true;
            
        } catch (Exception e) {
            log.error("MinIO文件复制失败，源：{}，目标：{}，错误：{}", 
                    sourceKey, targetKey, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<String> listFiles(String prefix, int maxKeys) {
        List<String> fileKeys = new ArrayList<>();
        
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(config.getBucket())
                            .prefix(prefix)
                            .maxKeys(maxKeys)
                            .build());
            
            for (Result<Item> result : results) {
                Item item = result.get();
                fileKeys.add(item.objectName());
                
                if (fileKeys.size() >= maxKeys) {
                    break;
                }
            }
            
            log.debug("列出MinIO文件，前缀：{}，数量：{}", prefix, fileKeys.size());
            
        } catch (Exception e) {
            log.error("列出MinIO文件失败，前缀：{}，错误：{}", prefix, e.getMessage(), e);
        }
        
        return fileKeys;
    }
    
    @Override
    public boolean isAvailable() {
        try {
            minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(config.getBucket())
                    .build());
            return true;
        } catch (Exception e) {
            log.warn("MinIO存储服务不可用：{}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public StorageProvider getProvider() {
        return StorageProvider.MINIO;
    }
    
    @Override
    public void destroy() {
        // MinIO客户端不需要显式释放资源
        log.info("MinIO存储服务已销毁");
    }
    
    /**
     * 如果Bucket不存在则创建
     */
    private void createBucketIfNotExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(config.getBucket())
                .build());
        
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(config.getBucket())
                    .region(config.getRegion())
                    .build());
            
            log.info("创建MinIO存储桶成功：{}", config.getBucket());
            
            // 设置Bucket策略
            if ("public-read".equals(config.getBucketPolicy())) {
                setPublicReadPolicy();
            }
        }
    }
    
    /**
     * 设置Bucket为公开读取
     */
    private void setPublicReadPolicy() throws Exception {
        String policy = "{\n" +
                "    \"Version\": \"2012-10-17\",\n" +
                "    \"Statement\": [\n" +
                "        {\n" +
                "            \"Effect\": \"Allow\",\n" +
                "            \"Principal\": \"*\",\n" +
                "            \"Action\": [\"s3:GetObject\"],\n" +
                "            \"Resource\": [\"arn:aws:s3:::" + config.getBucket() + "/*\"]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        
        minioClient.setBucketPolicy(SetBucketPolicyArgs.builder()
                .bucket(config.getBucket())
                .config(policy)
                .build());
        
        log.info("设置MinIO存储桶为公开读取：{}", config.getBucket());
    }
}

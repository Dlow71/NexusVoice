package com.nexusvoice.application.file.service;

import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.infrastructure.config.QiniuConfig;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * @Author AJ
 * @Date 2025-09-27 00:05
 * @Description 通用文件上传服务
 */
@Slf4j
@Service
public class FileUploadService {
    
    private final QiniuConfig qiniuConfig;
    private final UploadManager uploadManager;
    private final Auth auth;
    
    public FileUploadService(QiniuConfig qiniuConfig) {
        this.qiniuConfig = qiniuConfig;
        // 复用上传客户端，减少初始化开销
        Configuration cfg = new Configuration(Zone.autoZone());
        this.uploadManager = new UploadManager(cfg);
        this.auth = Auth.create(qiniuConfig.getAccessKey(), qiniuConfig.getSecretKey());
    }
    
    /**
     * 上传文件（自动识别文件类型）
     * @param file 文件
     * @return 文件访问URL
     * @throws IOException IO异常
     */
    public String upload(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("文件是空的");
        }
        
        String originalFilename = file.getOriginalFilename();
        FileTypeEnum fileType = FileTypeEnum.getFileTypeByFileName(originalFilename);
        
        return upload(file, fileType);
    }
    
    /**
     * 上传文件（指定文件类型）
     * @param file 文件
     * @param fileType 文件类型
     * @return 文件访问URL
     * @throws IOException IO异常
     */
    public String upload(MultipartFile file, FileTypeEnum fileType) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("文件是空的");
        }
        
        log.info("开始上传文件，文件名：{}，文件类型：{}", file.getOriginalFilename(), fileType.getDescription());
        
        // 创建上传token
        String upToken = auth.uploadToken(qiniuConfig.getBucket());
        
        String originalFilename = file.getOriginalFilename();
        // 构造文件目录和文件名
        assert originalFilename != null;
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String directory = getDirectoryByFileType(fileType);
        String fileKey = directory + UUID.randomUUID() + suffix;
        
        log.info("文件将上传到目录：{}，文件key：{}", directory, fileKey);
        
        // 上传文件
        Response response = uploadManager.put(file.getInputStream(), fileKey, upToken, null, null);
        
        if (!response.isOK()) {
            log.error("文件上传失败，响应：{}", response.toString());
            throw new RuntimeException("文件上传失败：" + response.toString());
        }
        
        String fileUrl = qiniuConfig.getDomain() + "/" + fileKey;
        log.info("文件上传成功，访问URL：{}", fileUrl);
        
        // 返回文件url
        return fileUrl;
    }
    
    /**
     * 根据文件类型获取上传目录
     * @param fileType 文件类型
     * @return 上传目录
     */
    private String getDirectoryByFileType(FileTypeEnum fileType) {
        // 优先使用新的按类型配置的目录
        if (qiniuConfig.getDirectories() != null && !qiniuConfig.getDirectories().isEmpty()) {
            String directory = qiniuConfig.getDirectories().get(fileType.getCode());
            if (directory != null && !directory.isEmpty()) {
                // 确保目录以/结尾
                return directory.endsWith("/") ? directory : directory + "/";
            }
        }
        
        // 如果没有配置目录，使用文件类型作为目录
        return fileType.getCode() + "/";
    }
}

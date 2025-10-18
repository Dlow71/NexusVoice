package com.nexusvoice.application.file.service;

import com.nexusvoice.enums.FileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @Author AJ
 * @Date 2025-09-27 00:05
 * @Description 通用文件上传服务（兼容旧接口，内部委托给UnifiedFileUploadService）
 */
@Slf4j
@Service
public class FileUploadService {
    
    @Autowired
    private UnifiedFileUploadService unifiedFileUploadService;
    
    /**
     * 上传文件（自动识别文件类型）
     * @param file 文件
     * @return 文件访问URL
     * @throws IOException IO异常
     */
    public String upload(MultipartFile file) throws IOException {
        try {
            return unifiedFileUploadService.upload(file);
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage(), e);
            throw new IOException("文件上传失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 上传文件（指定文件类型）
     * @param file 文件
     * @param fileType 文件类型
     * @return 文件访问URL
     * @throws IOException IO异常
     */
    public String upload(MultipartFile file, FileTypeEnum fileType) throws IOException {
        try {
            return unifiedFileUploadService.upload(file, fileType);
        } catch (Exception e) {
            log.error("文件上传失败：{}", e.getMessage(), e);
            throw new IOException("文件上传失败：" + e.getMessage(), e);
        }
    }
}

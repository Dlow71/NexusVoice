package com.nexusvoice.interfaces.api.file;

import com.nexusvoice.application.file.service.FileUploadService;
import com.nexusvoice.application.file.service.UnifiedFileUploadService;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 * 提供统一的文件上传功能，支持图片、音频、文档等
 * 
 * @Author AJ
 * @Date 2025-09-27 00:05
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
@Tag(name = "文件管理", description = "文件上传和管理")
public class FileController {
    
    private final FileUploadService fileUploadService;
    
    @Autowired(required = false)
    private UnifiedFileUploadService unifiedFileUploadService;
    
    public FileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }
    
    /**
     * 上传图片
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping("/upload/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            log.info("接收到图片上传请求，文件名：{}", file.getOriginalFilename());
            String fileUrl = fileUploadService.upload(file, FileTypeEnum.IMAGE);
            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.error("图片上传失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("图片上传异常", e);
            return Result.error("图片上传异常：" + e.getMessage());
        }
    }
    
    /**
     * 上传音频
     * @param file 音频文件
     * @return 上传结果
     */
    @PostMapping("/upload/audio")
    public Result<String> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            log.info("接收到音频上传请求，文件名：{}", file.getOriginalFilename());
            String fileUrl = fileUploadService.upload(file, FileTypeEnum.AUDIO);
            return Result.success(fileUrl);
        } catch (IOException e) {
            log.error("音频上传失败", e);
            return Result.error("音频上传失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("音频上传异常", e);
            return Result.error("音频上传异常：" + e.getMessage());
        }
    }
    
    // ==================== 新增：消息附件上传接口（返回完整元数据） ====================
    
    /**
     * 上传单张消息图片（返回完整元数据）
     * 用于聊天时上传图片附件
     */
    @PostMapping("/message/upload-image")
    @Operation(summary = "上传消息图片", description = "上传单张聊天图片，返回完整的CDN URL和元数据")
    public Result<Map<String, Object>> uploadMessageImage(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {
        
        log.info("开始上传消息图片，文件名：{}, 大小：{} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        // 验证文件
        if (file.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.FILE_IS_EMPTY, "图片文件不能为空");
        }
        
        // 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isImageFile(originalFilename)) {
            throw BizException.of(ErrorCodeEnum.FILE_TYPE_NOT_SUPPORTED, 
                    "不支持的图片格式，请上传 jpg、png、gif、webp 格式的图片");
        }
        
        // 验证文件大小（10MB限制）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw BizException.of(ErrorCodeEnum.FILE_SIZE_EXCEEDED, 
                    "图片文件大小不能超过 10MB");
        }
        
        try {
            // 使用统一上传服务
            String fileUrl = unifiedFileUploadService != null 
                ? unifiedFileUploadService.upload(file, FileTypeEnum.IMAGE)
                : fileUploadService.upload(file, FileTypeEnum.IMAGE);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("name", originalFilename);
            result.put("size", file.getSize());
            result.put("mimeType", file.getContentType());
            
            log.info("消息图片上传成功，CDN URL：{}", fileUrl);
            return Result.success(result);
            
        } catch (BizException e) {
            log.error("上传消息图片失败：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("上传消息图片时发生未知错误", e);
            throw BizException.of(ErrorCodeEnum.FILE_UPLOAD_FAILED, 
                    "图片上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 批量上传消息图片
     */
    @PostMapping("/message/upload-images")
    @Operation(summary = "批量上传消息图片", description = "一次上传多张聊天图片，返回CDN URL列表")
    public Result<List<Map<String, Object>>> uploadMessageImages(
            @Parameter(description = "图片文件列表（最多5张）", required = true)
            @RequestParam("files") List<MultipartFile> files) {
        
        log.info("开始批量上传消息图片，数量：{}", files.size());
        
        if (files.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "请选择要上传的图片");
        }
        if (files.size() > 5) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "最多只能同时上传5张图片");
        }
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            
            if (file.isEmpty()) {
                log.warn("第 {} 张图片为空，跳过", i + 1);
                continue;
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !isImageFile(originalFilename)) {
                log.warn("第 {} 张图片格式不支持，文件名：{}，跳过", i + 1, originalFilename);
                continue;
            }
            
            if (file.getSize() > 10 * 1024 * 1024) {
                log.warn("第 {} 张图片超过10MB，文件名：{}，跳过", i + 1, originalFilename);
                continue;
            }
            
            try {
                String fileUrl = unifiedFileUploadService != null 
                    ? unifiedFileUploadService.upload(file, FileTypeEnum.IMAGE)
                    : fileUploadService.upload(file, FileTypeEnum.IMAGE);
                
                Map<String, Object> result = new HashMap<>();
                result.put("url", fileUrl);
                result.put("name", originalFilename);
                result.put("size", file.getSize());
                result.put("mimeType", file.getContentType());
                
                results.add(result);
                log.info("第 {} 张图片上传成功，CDN URL：{}", i + 1, fileUrl);
                
            } catch (Exception e) {
                log.error("第 {} 张图片上传失败：{}", i + 1, e.getMessage());
            }
        }
        
        if (results.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.FILE_UPLOAD_FAILED, 
                    "所有图片上传失败，请检查文件格式和大小");
        }
        
        log.info("批量上传完成，成功：{} 张，失败：{} 张", 
                results.size(), files.size() - results.size());
        return Result.success(results);
    }
    
    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String filename) {
        String lowerCaseName = filename.toLowerCase();
        return lowerCaseName.endsWith(".jpg") ||
               lowerCaseName.endsWith(".jpeg") ||
               lowerCaseName.endsWith(".png") ||
               lowerCaseName.endsWith(".gif") ||
               lowerCaseName.endsWith(".webp") ||
               lowerCaseName.endsWith(".bmp");
    }
}

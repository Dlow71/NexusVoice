package com.nexusvoice.interfaces.api.file;

import com.nexusvoice.application.file.service.FileUploadService;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.FileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @Author AJ
 * @Date 2025-09-27 00:05
 * @Description 文件上传控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/file")
public class FileController {
    
    private final FileUploadService fileUploadService;
    
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
}

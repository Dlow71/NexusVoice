package com.nexusvoice.application.image.assembler;

import com.nexusvoice.application.image.dto.ImageGenerationRequestDTO;
import com.nexusvoice.application.image.dto.ImageGenerationResponseDTO;
import com.nexusvoice.domain.image.constant.ImageModelEnum;
import com.nexusvoice.domain.image.constant.ImageSizeEnum;
import com.nexusvoice.domain.image.model.ImageGenerationRequest;
import com.nexusvoice.domain.image.model.ImageGenerationResult;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 图像生成DTO转换器
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
@Component
public class ImageGenerationAssembler {

    /**
     * 将请求DTO转换为领域模型
     * 
     * @param requestDTO 请求DTO
     * @return 领域模型
     */
    public ImageGenerationRequest toDomainModel(ImageGenerationRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "图像生成请求不能为空");
        }

        ImageGenerationRequest request = new ImageGenerationRequest();

        // 转换模型枚举
        try {
            request.setModel(ImageModelEnum.getByModelName(requestDTO.getModel()));
        } catch (IllegalArgumentException e) {
            throw BizException.of(ErrorCodeEnum.IMAGE_MODEL_NOT_SUPPORTED, e.getMessage());
        }

        // 设置提示词
        request.setPrompt(requestDTO.getPrompt());
        request.setNegativePrompt(requestDTO.getNegativePrompt());

        // 转换图像尺寸枚举
        if (requestDTO.getImageSize() != null) {
            try {
                request.setImageSize(ImageSizeEnum.getBySize(requestDTO.getImageSize()));
            } catch (IllegalArgumentException e) {
                throw BizException.of(ErrorCodeEnum.IMAGE_SIZE_INVALID, e.getMessage());
            }
        } else {
            // 使用模型默认尺寸
            request.setImageSize(ImageSizeEnum.getDefaultSizeForModel(request.getModel()));
        }

        // 设置其他参数，使用默认值如果为null
        request.setBatchSize(requestDTO.getBatchSize() != null ? requestDTO.getBatchSize() : 1);
        request.setSeed(requestDTO.getSeed());
        request.setNumInferenceSteps(requestDTO.getNumInferenceSteps() != null ? requestDTO.getNumInferenceSteps() : 20);
        
        // 根据模型特性设置特定参数
        if (request.getModel().supportsGuidanceScale()) {
            // Kolors模型支持guidanceScale参数
            request.setGuidanceScale(requestDTO.getGuidanceScale() != null ? requestDTO.getGuidanceScale() : 7.5);
        }
        
        if (request.getModel().supportsCFG()) {
            // 只有支持CFG的模型才设置cfg参数
            request.setCfg(requestDTO.getCfg() != null ? requestDTO.getCfg() : 4.0);
        }

        // 设置输入图像
        request.setInputImage(requestDTO.getInputImage());
        request.setInputImage2(requestDTO.getInputImage2());
        request.setInputImage3(requestDTO.getInputImage3());

        // 验证领域模型
        String validationError = request.validate();
        if (validationError != null) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, validationError);
        }

        return request;
    }

    /**
     * 将领域结果转换为响应DTO
     * 
     * @param result 领域结果
     * @param originalRequest 原始请求DTO
     * @return 响应DTO
     */
    public ImageGenerationResponseDTO toResponseDTO(ImageGenerationResult result, ImageGenerationRequestDTO originalRequest) {
        if (result == null) {
            throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, "图像生成结果为空");
        }

        ImageGenerationResponseDTO responseDTO = new ImageGenerationResponseDTO();

        // 设置基本信息
        responseDTO.setImageUrls(result.getImageUrls());
        responseDTO.setImageCount(result.getImageCount());
        responseDTO.setUsedSeed(result.getUsedSeed());
        responseDTO.setGenerationTime(result.getGenerationTime());

        // 从原始请求设置信息
        if (originalRequest != null) {
            responseDTO.setPrompt(originalRequest.getPrompt());
            responseDTO.setNegativePrompt(originalRequest.getNegativePrompt());
            responseDTO.setModel(originalRequest.getModel());
            responseDTO.setImageSize(originalRequest.getImageSize() != null ? originalRequest.getImageSize() : "1024x1024");
            responseDTO.setNumInferenceSteps(originalRequest.getNumInferenceSteps());
            responseDTO.setGuidanceScale(originalRequest.getGuidanceScale());
            responseDTO.setCfg(originalRequest.getCfg());
        }

        // 构建图像详细信息
        if (result.getImageUrls() != null && !result.getImageUrls().isEmpty()) {
            List<ImageGenerationResponseDTO.ImageInfo> imageInfos = new ArrayList<>();
            
            for (int i = 0; i < result.getImageUrls().size(); i++) {
                String imageUrl = result.getImageUrls().get(i);
                
                ImageGenerationResponseDTO.ImageInfo imageInfo = new ImageGenerationResponseDTO.ImageInfo();
                imageInfo.setUrl(imageUrl);
                imageInfo.setIndex(i);
                
                // 从尺寸字符串解析宽高
                if (responseDTO.getImageSize() != null) {
                    String[] dimensions = responseDTO.getImageSize().split("x");
                    if (dimensions.length == 2) {
                        try {
                            imageInfo.setWidth(Integer.parseInt(dimensions[0]));
                            imageInfo.setHeight(Integer.parseInt(dimensions[1]));
                        } catch (NumberFormatException e) {
                            // 忽略解析错误
                        }
                    }
                }
                
                // 从URL推断文件名和格式
                if (imageUrl != null) {
                    String fileName = extractFileNameFromUrl(imageUrl);
                    imageInfo.setFileName(fileName);
                    imageInfo.setFormat(extractFormatFromFileName(fileName));
                }
                
                imageInfos.add(imageInfo);
            }
            
            responseDTO.setImageInfos(imageInfos);
        }

        return responseDTO;
    }

    /**
     * 从URL中提取文件名
     * 
     * @param url 图像URL
     * @return 文件名
     */
    private String extractFileNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown";
        }
        
        // 移除查询参数
        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            url = url.substring(0, queryIndex);
        }
        
        // 提取最后的文件名部分
        int lastSlashIndex = url.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }
        
        return "unknown";
    }

    /**
     * 从文件名中提取格式
     * 
     * @param fileName 文件名
     * @return 格式（大写）
     */
    private String extractFormatFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "PNG";
        }
        
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toUpperCase();
        }
        
        return "PNG"; // 默认格式
    }
}

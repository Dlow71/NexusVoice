package com.nexusvoice.application.image.service;

import com.nexusvoice.application.image.assembler.ImageGenerationAssembler;
import com.nexusvoice.application.image.dto.ImageGenerationRequestDTO;
import com.nexusvoice.application.image.dto.ImageGenerationResponseDTO;
import com.nexusvoice.domain.image.model.ImageGenerationRequest;
import com.nexusvoice.domain.image.model.ImageGenerationResult;
import com.nexusvoice.domain.image.repository.ImageGenerationRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图像生成应用服务
 * 负责图像生成业务流程编排和处理
 * 
 * @author NexusVoice Team
 * @since 2025-09-28
 */
@Slf4j
@Service
public class ImageGenerationService {

    @Resource
    private ImageGenerationRepository imageGenerationRepository;

    @Resource
    private ImageGenerationAssembler imageGenerationAssembler;

    /**
     * 生成图像
     * 
     * @param requestDTO 图像生成请求DTO
     * @return 图像生成响应DTO
     */
    public ImageGenerationResponseDTO generateImage(ImageGenerationRequestDTO requestDTO) {
        log.info("开始处理图像生成请求，模型: {}, 提示词: {}", 
                requestDTO.getModel(), requestDTO.getPrompt());

        // 参数验证
        validateRequest(requestDTO);

        try {
            // 转换为领域模型
            ImageGenerationRequest domainRequest = imageGenerationAssembler.toDomainModel(requestDTO);
            
            // 调用领域服务生成图像
            ImageGenerationResult result = imageGenerationRepository.generateImage(domainRequest);
            
            // 验证生成结果
            validateResult(result);
            
            // 转换为响应DTO
            ImageGenerationResponseDTO responseDTO = imageGenerationAssembler.toResponseDTO(result, requestDTO);
            
            log.info("图像生成完成，生成数量: {}, 耗时: {}ms, 第一张图像URL: {}", 
                    responseDTO.getImageCount(), 
                    responseDTO.getGenerationTime(),
                    responseDTO.getFirstImageUrl());
            
            return responseDTO;
            
        } catch (BizException e) {
            log.error("图像生成业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("图像生成过程中发生未知异常", e);
            throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, 
                                 "图像生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量生成图像
     * 
     * @param requestDTO 图像生成请求DTO（支持批量）
     * @return 图像生成响应DTO
     */
    public ImageGenerationResponseDTO generateImageBatch(ImageGenerationRequestDTO requestDTO) {
        log.info("开始处理批量图像生成请求，模型: {}, 批量大小: {}", 
                requestDTO.getModel(), requestDTO.getBatchSize());

        // 验证批量参数
        if (requestDTO.getBatchSize() == null || requestDTO.getBatchSize() < 1) {
            requestDTO.setBatchSize(1);
        }
        
        if (requestDTO.getBatchSize() > 4) {
            throw BizException.of(ErrorCodeEnum.IMAGE_BATCH_SIZE_INVALID, "批量生成数量不能超过4张");
        }
        
        // 复用单个生成方法
        return generateImage(requestDTO);
    }

    /**
     * 检查图像生成服务状态
     * 
     * @return 服务是否可用
     */
    public boolean checkServiceHealth() {
        try {
            boolean available = imageGenerationRepository.isServiceAvailable();
            log.debug("图像生成服务健康检查结果: {}", available ? "可用" : "不可用");
            return available;
        } catch (Exception e) {
            log.warn("图像生成服务健康检查失败", e);
            return false;
        }
    }

    /**
     * 获取支持的模型列表
     * 
     * @return 支持的模型名称列表
     */
    public List<String> getSupportedModels() {
        try {
            List<String> models = imageGenerationRepository.getSupportedModels();
            log.debug("获取支持的图像生成模型: {}", models);
            return models;
        } catch (Exception e) {
            log.error("获取支持的模型列表失败", e);
            throw BizException.of(ErrorCodeEnum.IMAGE_SERVICE_ERROR, "获取支持的模型列表失败");
        }
    }

    /**
     * 验证API密钥
     * 
     * @param apiKey API密钥
     * @return 是否有效
     */
    public boolean validateApiKey(String apiKey) {
        try {
            boolean valid = imageGenerationRepository.validateApiKey(apiKey);
            log.debug("API密钥验证结果: {}", valid ? "有效" : "无效");
            return valid;
        } catch (Exception e) {
            log.error("API密钥验证失败", e);
            return false;
        }
    }

    /**
     * 获取模型推荐参数
     * 
     * @param modelName 模型名称
     * @return 推荐参数配置
     */
    public ImageGenerationRequestDTO getModelRecommendedParams(String modelName) {
        log.debug("获取模型推荐参数: {}", modelName);
        
        ImageGenerationRequestDTO recommendedParams = new ImageGenerationRequestDTO();
        recommendedParams.setModel(modelName);
        
        // 根据不同模型设置推荐参数
        if (modelName.startsWith("Qwen/")) {
            // Qwen模型推荐参数
            recommendedParams.setImageSize("1328x1328");
            recommendedParams.setNumInferenceSteps(20);
            recommendedParams.setCfg(4.0);
            recommendedParams.setBatchSize(1);
        } else if (modelName.startsWith("Kwai-Kolors/")) {
            // Kolors模型推荐参数
            recommendedParams.setImageSize("1024x1024");
            recommendedParams.setNumInferenceSteps(20);
            recommendedParams.setGuidanceScale(7.5);
            recommendedParams.setBatchSize(1);
        } else {
            // 默认参数
            recommendedParams.setImageSize("1024x1024");
            recommendedParams.setNumInferenceSteps(20);
            recommendedParams.setBatchSize(1);
        }
        
        return recommendedParams;
    }

    /**
     * 验证请求参数
     */
    private void validateRequest(ImageGenerationRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw BizException.of(ErrorCodeEnum.PARAM_ERROR, "图像生成请求不能为空");
        }

        if (requestDTO.getModel() == null || requestDTO.getModel().trim().isEmpty()) {
            throw BizException.of(ErrorCodeEnum.IMAGE_MODEL_NOT_SUPPORTED, "图像生成模型不能为空");
        }

        if (requestDTO.getPrompt() == null || requestDTO.getPrompt().trim().isEmpty()) {
            throw BizException.of(ErrorCodeEnum.IMAGE_PROMPT_INVALID, "图像描述提示词不能为空");
        }

        // 检查提示词长度
        if (requestDTO.getPrompt().length() > 2000) {
            throw BizException.of(ErrorCodeEnum.IMAGE_PROMPT_INVALID, "提示词长度不能超过2000字符");
        }

        // 检查负向提示词长度
        if (requestDTO.getNegativePrompt() != null && requestDTO.getNegativePrompt().length() > 1000) {
            throw BizException.of(ErrorCodeEnum.IMAGE_PROMPT_INVALID, "负向提示词长度不能超过1000字符");
        }

        // 检查批量大小
        if (requestDTO.getBatchSize() != null && (requestDTO.getBatchSize() < 1 || requestDTO.getBatchSize() > 4)) {
            throw BizException.of(ErrorCodeEnum.IMAGE_BATCH_SIZE_INVALID, "批量生成数量必须在1-4之间");
        }

        // 检查随机种子
        if (requestDTO.getSeed() != null && (requestDTO.getSeed() < 0 || requestDTO.getSeed() > 9999999999L)) {
            throw BizException.of(ErrorCodeEnum.IMAGE_SEED_INVALID, "随机种子必须在0-9999999999之间");
        }

        // 检查推理步数
        if (requestDTO.getNumInferenceSteps() != null && 
            (requestDTO.getNumInferenceSteps() < 1 || requestDTO.getNumInferenceSteps() > 100)) {
            throw BizException.of(ErrorCodeEnum.IMAGE_STEPS_INVALID, "推理步数必须在1-100之间");
        }

        // 检查引导比例
        if (requestDTO.getGuidanceScale() != null && 
            (requestDTO.getGuidanceScale() < 0 || requestDTO.getGuidanceScale() > 20)) {
            throw BizException.of(ErrorCodeEnum.IMAGE_GUIDANCE_SCALE_INVALID, "引导比例必须在0-20之间");
        }

        // 检查CFG参数
        if (requestDTO.getCfg() != null && 
            (requestDTO.getCfg() < 0.1 || requestDTO.getCfg() > 20)) {
            throw BizException.of(ErrorCodeEnum.IMAGE_CFG_INVALID, "CFG参数必须在0.1-20之间");
        }

        log.debug("图像生成请求参数验证通过");
    }

    /**
     * 验证生成结果
     */
    private void validateResult(ImageGenerationResult result) {
        if (result == null) {
            throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, "图像生成结果为空");
        }

        if (!result.isSuccess()) {
            throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, "图像生成失败，未返回有效图像");
        }

        if (result.getImageUrls() == null || result.getImageUrls().isEmpty()) {
            throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, "图像生成失败，图像URL列表为空");
        }

        // 验证图像URL的有效性
        for (String imageUrl : result.getImageUrls()) {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                throw BizException.of(ErrorCodeEnum.IMAGE_GENERATION_FAILED, "图像生成失败，存在无效的图像URL");
            }
        }

        log.debug("图像生成结果验证通过，生成了{}张图像", result.getImageCount());
    }
}

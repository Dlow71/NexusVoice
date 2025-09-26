package com.nexusvoice.interfaces.api.tts;

import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.application.tts.service.TTSService;
import com.nexusvoice.common.Result;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.TTSException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * TTS文本转语音控制器
 * 
 * @author NexusVoice Team
 * @since 2025-09-24
 */
@Slf4j
@RestController
@RequestMapping("/api/tts")
@RequiredArgsConstructor
@Tag(name = "TTS文本转语音", description = "提供文本转语音功能的API接口")
public class TTSController {

    private final TTSService ttsApplicationService;

    /**
     * 文本转语音
     * 
     * @param requestDTO TTS请求参数
     * @return 包含音频数据的响应结果
     */
    @PostMapping("/text-to-speech")
    @Operation(summary = "文本转语音", description = "将输入的文本转换为语音音频，返回Base64编码的音频数据")
    public Result<TTSResponseDTO> textToSpeech(@Valid @RequestBody TTSRequestDTO requestDTO) {
        try {
            log.info("收到TTS请求，文本长度: {}, 语音类型: {}, 编码格式: {}", 
                    requestDTO.getText().length(), 
                    requestDTO.getVoiceType(), 
                    requestDTO.getEncoding());

            TTSResponseDTO responseDTO = ttsApplicationService.textToSpeech(requestDTO);
            
            log.info("TTS处理成功，音频大小: {} 字节", responseDTO.getAudioSize());
            return Result.success("文本转语音成功", responseDTO);

        } catch (TTSException e) {
            log.error("TTS处理失败: {}", e.getMessage(), e);
            return Result.error(ErrorCodeEnum.TTS_SERVICE_ERROR, "TTS处理失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("TTS服务异常: {}", e.getMessage(), e);
            return Result.error(ErrorCodeEnum.INTERNAL_SERVER_ERROR, "TTS服务异常");
        }
    }

    /**
     * 获取支持的语音类型列表
     * 
     * @return 支持的语音类型
     */
    @GetMapping("/voice-types")
    @Operation(summary = "获取支持的语音类型", description = "获取系统支持的所有语音类型列表")
    public Result<String[]> getSupportedVoiceTypes() {
        String[] voiceTypes = {
            "qiniu_zh_female_wwxkjx",
            "qiniu_zh_male_wwxkjx",
            "qiniu_en_female_wwxkjx",
            "qiniu_en_male_wwxkjx"
        };
        return Result.success("获取语音类型成功", voiceTypes);
    }

}

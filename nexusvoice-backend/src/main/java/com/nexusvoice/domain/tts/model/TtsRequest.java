package com.nexusvoice.domain.tts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TTS合成请求
 * 纯粹的领域模型，零基础设施依赖
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TtsRequest {
    
    /**
     * 用户ID（用于日志和费用统计）
     */
    private Long userId;
    
    /**
     * 要合成的文本内容
     */
    private String text;
    
    /**
     * 语音类型（如：qiniu_zh_female_wwxkjx）
     * 从模型配置中读取，不同模型可能有不同的语音类型
     */
    private String voiceType;
    
    /**
     * 音频编码格式（mp3/wav/pcm）
     */
    private String encoding;
    
    /**
     * 语速比例（0.5-2.0）
     */
    private Double speedRatio;
    
    /**
     * 是否分段合成
     * 长文本时启用，提高并发效率
     */
    private Boolean chunked;
    
    /**
     * 分段最大字符数（仅chunked=true时有效）
     */
    private Integer maxChunkChars;
    
    /**
     * 分段并发数（仅chunked=true时有效）
     */
    private Integer maxConcurrency;
    
    /**
     * 会话ID（可选，用于关联对话）
     */
    private Long conversationId;
    
    /**
     * 临时文件过期时间（分钟）
     * 如果为null，则为永久文件
     */
    private Integer expireMinutes;
    
    /**
     * 验证请求参数
     */
    public void validate() {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("文本内容不能为空");
        }
        if (text.length() > 10000) {
            throw new IllegalArgumentException("文本长度不能超过10000字符");
        }
        if (speedRatio != null && (speedRatio < 0.5 || speedRatio > 2.0)) {
            throw new IllegalArgumentException("语速比例必须在0.5-2.0之间");
        }
        if (encoding != null && !isValidEncoding(encoding)) {
            throw new IllegalArgumentException("音频编码格式必须为mp3、wav或pcm");
        }
    }
    
    /**
     * 验证音频编码格式
     */
    private boolean isValidEncoding(String encoding) {
        String e = encoding.toLowerCase();
        return "mp3".equals(e) || "wav".equals(e) || "pcm".equals(e);
    }
    
    /**
     * 获取字符数（用于计费）
     */
    public int getCharCount() {
        return text != null ? text.length() : 0;
    }
}

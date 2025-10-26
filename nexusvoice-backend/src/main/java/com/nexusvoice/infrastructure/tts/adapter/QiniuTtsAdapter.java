package com.nexusvoice.infrastructure.tts.adapter;

import com.nexusvoice.application.file.service.UnifiedFileUploadService;
import com.nexusvoice.domain.ai.model.AiApiKey;
import com.nexusvoice.domain.ai.model.AiModel;
import com.nexusvoice.domain.storage.model.UploadResult;
import com.nexusvoice.domain.tts.model.TtsRequest;
import com.nexusvoice.domain.tts.model.TtsResult;
import com.nexusvoice.domain.tts.model.TtsSegment;
import com.nexusvoice.domain.tts.repository.TtsRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.exception.BizException;
import com.nexusvoice.exception.TTSException;
import com.nexusvoice.utils.TTSToolUtils;
import com.nexusvoice.utils.TextChunker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 七牛云TTS适配器
 * 实现TtsRepository接口，封装七牛云TTS技术细节
 * 
 * @author NexusVoice
 * @since 2025-10-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QiniuTtsAdapter implements TtsRepository {
    
    private final UnifiedFileUploadService fileUploadService;
    
    @Override
    public TtsResult synthesize(TtsRequest request, AiModel model, AiApiKey apiKey) {
        // 参数验证
        request.validate();
        
        try {
            // 从模型配置中读取语音参数
            String voiceType = getVoiceType(request, model);
            String encoding = request.getEncoding() != null ? request.getEncoding() : getDefaultEncoding(model);
            double speedRatio = request.getSpeedRatio() != null ? request.getSpeedRatio() : getDefaultSpeedRatio(model);
            
            // 创建TTS工具实例
            TTSToolUtils ttsToolUtils = TTSToolUtils.createWithDefaults(
                apiKey.getApiKey(),
                voiceType,
                encoding,
                speedRatio
            );
            
            String text = request.getText().trim();
            
            // 判断是否需要分段合成
            if (Boolean.TRUE.equals(request.getChunked()) && shouldChunk(text, request)) {
                return synthesizeInChunks(text, voiceType, encoding, speedRatio, ttsToolUtils, request);
            } else {
                return synthesizeSingle(text, voiceType, encoding, speedRatio, ttsToolUtils, request);
            }
            
        } catch (TTSException e) {
            log.error("七牛云TTS合成失败，模型：{}，错误：{}", model.getModelKey(), e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.TTS_SERVICE_ERROR, "TTS合成失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("七牛云TTS合成异常，模型：{}，错误：{}", model.getModelKey(), e.getMessage(), e);
            throw new BizException(ErrorCodeEnum.TTS_SERVICE_ERROR, "TTS合成异常：" + e.getMessage());
        }
    }
    
    @Override
    public boolean isAvailable(AiModel model, AiApiKey apiKey) {
        // 检查模型配置和API密钥是否有效
        return model != null && model.isEnabled() && 
               apiKey != null && apiKey.isAvailable();
    }
    
    /**
     * 单段合成
     */
    private TtsResult synthesizeSingle(String text, String voiceType, String encoding, 
                                       double speedRatio, TTSToolUtils ttsToolUtils, TtsRequest request) throws TTSException {
        try {
            // 调用TTS工具生成音频
            MultipartFile audioFile = ttsToolUtils.textToAudioFile(text, voiceType, encoding, speedRatio);
            if (audioFile == null || audioFile.isEmpty()) {
                throw new TTSException("音频生成失败，返回文件为空");
            }
            
            // 上传文件到CDN
            UploadResult uploadResult = uploadAudioFile(audioFile, request);
            
            // 构建结果
            return TtsResult.singleSegment(
                uploadResult.getFileUrl(),
                encoding,
                uploadResult.getFileSize().intValue(),
                text,
                voiceType,
                speedRatio
            );
            
        } catch (Exception e) {
            throw new TTSException("单段TTS合成失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 分段合成
     */
    private TtsResult synthesizeInChunks(String text, String voiceType, String encoding, 
                                         double speedRatio, TTSToolUtils ttsToolUtils, TtsRequest request) 
            throws TTSException, InterruptedException, ExecutionException {
        
        int maxChunkChars = request.getMaxChunkChars() != null ? request.getMaxChunkChars() : 300;
        int maxConcurrency = request.getMaxConcurrency() != null ? request.getMaxConcurrency() : 4;
        
        // 切分文本
        List<String> chunks = TextChunker.splitBySentence(text, maxChunkChars);
        if (chunks.isEmpty()) {
            throw new TTSException("文本切分失败");
        }
        
        log.info("开始分段TTS合成，总段数：{}，并发数：{}", chunks.size(), maxConcurrency);
        
        String groupId = UUID.randomUUID().toString();
        List<SegmentResult> results = new ArrayList<>(chunks.size());
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        
        try {
            Semaphore gate = new Semaphore(maxConcurrency);
            List<CompletableFuture<SegmentResult>> futures = new ArrayList<>();
            
            for (int i = 0; i < chunks.size(); i++) {
                final int index = i;
                final String segText = chunks.get(i);
                
                CompletableFuture<SegmentResult> cf = CompletableFuture.supplyAsync(() -> {
                    boolean acquired = false;
                    try {
                        try {
                            gate.acquire();
                            acquired = true;
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(ie);
                        }
                        
                        // 生成音频
                        MultipartFile audio = ttsToolUtils.textToAudioFile(segText, voiceType, encoding, speedRatio);
                        if (audio == null || audio.isEmpty()) {
                            throw new TTSException("分段音频生成失败，段号：" + index);
                        }
                        
                        // 上传文件
                        UploadResult uploadResult = uploadAudioFile(audio, request);
                        
                        return new SegmentResult(index, segText, uploadResult.getFileUrl(), 
                                uploadResult.getFileSize().intValue());
                        
                    } catch (Exception e) {
                        throw new RuntimeException("分段" + index + "处理失败：" + e.getMessage(), e);
                    } finally {
                        if (acquired) {
                            gate.release();
                        }
                    }
                }, executor);
                
                futures.add(cf);
            }
            
            // 等待全部完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // 收集结果
            for (CompletableFuture<SegmentResult> f : futures) {
                results.add(f.get());
            }
            
        } finally {
            executor.shutdown();
        }
        
        // 按index排序
        results.sort(Comparator.comparingInt(a -> a.index));
        
        // 转换为TtsSegment
        List<TtsSegment> segments = new ArrayList<>(results.size());
        for (SegmentResult r : results) {
            TtsSegment segment = TtsSegment.builder()
                    .index(r.index)
                    .text(r.text)
                    .url(r.url)
                    .size(r.size)
                    .charCount(r.text.length())
                    .build();
            segments.add(segment);
        }
        
        log.info("分段TTS合成完成，总段数：{}，总大小：{}字节", 
                segments.size(), segments.stream().mapToInt(TtsSegment::getSize).sum());
        
        // 构建多段结果
        return TtsResult.multiSegments(segments, encoding, text, voiceType, speedRatio, groupId);
    }
    
    /**
     * 上传音频文件到CDN
     */
    private UploadResult uploadAudioFile(MultipartFile audioFile, TtsRequest request) {
        try {
            // 注意：当前版本统一使用永久文件上传
            // 临时文件过期功能由文件存储服务统一管理
            // TODO: 如果需要临时文件功能，可以在上传后设置过期时间或使用定时清理任务
            return fileUploadService.uploadWithResult(audioFile, FileTypeEnum.AUDIO);
        } catch (Exception e) {
            throw new RuntimeException("音频文件上传失败：" + e.getMessage(), e);
        }
    }
    
    /**
     * 判断是否需要分段合成
     */
    private boolean shouldChunk(String text, TtsRequest request) {
        int maxChunkChars = request.getMaxChunkChars() != null ? request.getMaxChunkChars() : 300;
        return text.length() > maxChunkChars;
    }
    
    /**
     * 获取语音类型（从请求或模型配置）
     */
    private String getVoiceType(TtsRequest request, AiModel model) {
        if (request.getVoiceType() != null && !request.getVoiceType().trim().isEmpty()) {
            return request.getVoiceType();
        }
        return (String) model.getConfigMap().getOrDefault("voiceType", "qiniu_zh_female_wwxkjx");
    }
    
    /**
     * 获取默认编码格式
     */
    private String getDefaultEncoding(AiModel model) {
        return (String) model.getConfigMap().getOrDefault("encoding", "mp3");
    }
    
    /**
     * 获取默认语速
     */
    private double getDefaultSpeedRatio(AiModel model) {
        Object speed = model.getConfigMap().get("speedRatio");
        if (speed instanceof Number) {
            return ((Number) speed).doubleValue();
        }
        return 1.0;
    }
    
    /**
     * 分段结果内部类
     */
    private static class SegmentResult {
        final int index;
        final String text;
        final String url;
        final int size;
        
        SegmentResult(int index, String text, String url, int size) {
            this.index = index;
            this.text = text;
            this.url = url;
            this.size = size;
        }
    }
}

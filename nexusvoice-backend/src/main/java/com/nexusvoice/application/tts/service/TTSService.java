package com.nexusvoice.application.tts.service;

import com.nexusvoice.application.file.service.FileUploadService;
import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.domain.config.model.SystemConfig;
import com.nexusvoice.domain.config.repository.SystemConfigRepository;
import com.nexusvoice.enums.FileTypeEnum;
import com.nexusvoice.exception.TTSException;
import com.nexusvoice.infrastructure.config.QiniuConfig;
import com.nexusvoice.utils.TTSToolUtils;
import com.nexusvoice.utils.TextChunker;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
 * TTS应用服务
 * 负责TTS业务流程编排和处理
 * 
 * @author NexusVoice Team
 * @since 2025-09-24
 */
@Service
public class TTSService {

    @Value("${nexusvoice.tts.token}")
    private String qiniuToken;

    @Resource
    private FileUploadService fileUploadService;

    @Resource
    private QiniuConfig qiniuConfig;

    @Resource
    private SystemConfigRepository systemConfigRepository;

    /**
     * 文本转语音
     * 
     * @param requestDTO TTS请求DTO
     * @return TTS响应DTO
     * @throws TTSException TTS处理异常
     */
    public TTSResponseDTO textToSpeech(TTSRequestDTO requestDTO) throws TTSException {
        // 参数校验
        if (requestDTO == null || requestDTO.getText() == null || requestDTO.getText().trim().isEmpty()) {
            throw new TTSException("文本内容不能为空");
        }

        try {
            // 获取参数，设置默认值
            String voiceType = requestDTO.getVoiceType() != null ? requestDTO.getVoiceType() : "qiniu_zh_female_wwxkjx";
            String encoding = requestDTO.getEncoding() != null ? requestDTO.getEncoding() : "mp3";
            Double speedRatio = requestDTO.getSpeedRatio() != null ? requestDTO.getSpeedRatio() : 1.0;

            // 读取系统配置（数据库）
            boolean chunkEnabled = getBooleanConfig("tts.chunk.enabled", true);
            int maxChunkChars = getIntConfig("tts.chunk.max_chars", 300, 50, 2000);
            int maxConcurrency = getIntConfig("tts.chunk.max_concurrency", 4, 1, 16);

            // 创建TTS工具实例
            TTSToolUtils ttsToolUtils = TTSToolUtils.createWithDefaults(
                qiniuToken,
                voiceType,
                encoding,
                speedRatio
            );

            String text = requestDTO.getText().trim();

            // 是否走分段并发
            if (chunkEnabled && text.length() > maxChunkChars) {
                return processInChunks(text, voiceType, encoding, speedRatio, ttsToolUtils, maxChunkChars, maxConcurrency);
            } else {
                // 单段处理（保持原有逻辑）
                MultipartFile audioFile = ttsToolUtils.textToAudioFile(text, voiceType, encoding, speedRatio);
                if (audioFile == null || audioFile.isEmpty()) {
                    throw new TTSException("音频生成失败，返回文件为空");
                }
                String audioUrl = fileUploadService.upload(audioFile, FileTypeEnum.AUDIO);

                TTSResponseDTO responseDTO = new TTSResponseDTO();
                responseDTO.setAudioData(audioUrl);
                responseDTO.setAudioFormat(encoding);
                responseDTO.setAudioSize((int) audioFile.getSize());
                responseDTO.setText(text);
                responseDTO.setVoiceType(voiceType);
                responseDTO.setSpeedRatio(speedRatio);
                responseDTO.setChunked(false);
                // 为单段TTS补充统一的分段结构，便于前端一致处理
                List<TTSResponseDTO.Segment> segs = new ArrayList<>(1);
                TTSResponseDTO.Segment seg = new TTSResponseDTO.Segment();
                seg.setIndex(0);
                seg.setText(text);
                seg.setUrl(audioUrl);
                seg.setSize((int) audioFile.getSize());
                // 单段场景下时长未知，暂不填充
                segs.add(seg);
                responseDTO.setSegments(segs);
                return responseDTO;
            }

        } catch (TTSException e) {
            throw e;
        } catch (IOException e) {
            throw new TTSException("音频文件上传失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new TTSException("TTS服务处理失败: " + e.getMessage(), e);
        }
    }

    private TTSResponseDTO processInChunks(String text,
                                           String voiceType,
                                           String encoding,
                                           Double speedRatio,
                                           TTSToolUtils ttsToolUtils,
                                           int maxChunkChars,
                                           int maxConcurrency) throws IOException, TTSException, InterruptedException, ExecutionException {
        List<String> chunks = TextChunker.splitBySentence(text, maxChunkChars);
        if (chunks.isEmpty()) {
            throw new TTSException("文本切分失败");
        }

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
                        MultipartFile audio = ttsToolUtils.textToAudioFile(segText, voiceType, encoding, speedRatio);
                        if (audio == null || audio.isEmpty()) {
                            throw new TTSException("分段音频生成失败");
                        }
                        String url = fileUploadService.upload(audio, FileTypeEnum.AUDIO);
                        return new SegmentResult(index, segText, url, (int) audio.getSize());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
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
        results.sort(Comparator.comparingInt(a -> a.index));

        // 组装响应
        List<TTSResponseDTO.Segment> segDtos = new ArrayList<>(results.size());
        int totalSize = 0;
        for (SegmentResult r : results) {
            TTSResponseDTO.Segment seg = new TTSResponseDTO.Segment();
            seg.setIndex(r.index);
            seg.setText(r.text);
            seg.setUrl(r.url);
            seg.setSize(r.size);
            segDtos.add(seg);
            totalSize += r.size;
        }

        TTSResponseDTO dto = new TTSResponseDTO();
        dto.setChunked(true);
        dto.setGroupId(groupId);
        dto.setSegments(segDtos);
        dto.setAudioData(segDtos.get(0).getUrl()); // 兼容字段：首段URL
        dto.setAudioFormat(encoding);
        dto.setAudioSize(totalSize);
        dto.setText(text);
        dto.setVoiceType(voiceType);
        dto.setSpeedRatio(speedRatio);
        return dto;
    }

    private boolean getBooleanConfig(String key, boolean defaultVal) {
        return systemConfigRepository.findByKey(key)
                .filter(SystemConfig::isActive)
                .map(SystemConfig::getConfigValue)
                .map(v -> {
                    String s = v.trim().toLowerCase();
                    return s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("on");
                })
                .orElse(defaultVal);
    }

    private int getIntConfig(String key, int defaultVal, int min, int max) {
        return systemConfigRepository.findByKey(key)
                .filter(SystemConfig::isActive)
                .map(SystemConfig::getConfigValue)
                .map(v -> {
                    try {
                        int n = Integer.parseInt(v.trim());
                        if (n < min) return min;
                        if (n > max) return max;
                        return n;
                    } catch (Exception e) {
                        return defaultVal;
                    }
                })
                .orElse(defaultVal);
    }

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

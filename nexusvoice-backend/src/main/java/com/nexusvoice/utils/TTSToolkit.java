package com.nexusvoice.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * TTS工具类 - 用于接收文本并返回生成的音频数据
 * 专为模型回复转语音场景设计
 * 
 * @author NexusVoice Team
 */
public class TTSToolkit {
    
    private final String token;
    private final String host;
    private final String path;
    private final String defaultVoiceType;
    private final String defaultEncoding;
    private final double defaultSpeedRatio;
    
    // 默认配置
    private static final String DEFAULT_HOST = "openai.qiniu.com";
    private static final String DEFAULT_PATH = "/v1/voice/tts";
    private static final String DEFAULT_VOICE_TYPE = "qiniu_zh_female_xyqxxj";
    private static final String DEFAULT_ENCODING = "mp3";
    private static final double DEFAULT_SPEED_RATIO = 2.0;
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    
    /**
     * 构造函数 - 使用默认配置
     * 
     * @param token 七牛云API Token
     */
    public TTSToolkit(String token) {
        this(token, DEFAULT_HOST, DEFAULT_PATH, DEFAULT_VOICE_TYPE, DEFAULT_ENCODING, DEFAULT_SPEED_RATIO);
    }
    
    /**
     * 构造函数 - 自定义配置
     * 
     * @param token 七牛云API Token
     * @param voiceType 音色类型
     * @param encoding 编码格式
     * @param speedRatio 语速比例
     */
    public TTSToolkit(String token, String voiceType, String encoding, double speedRatio) {
        this(token, DEFAULT_HOST, DEFAULT_PATH, voiceType, encoding, speedRatio);
    }
    
    /**
     * 完整构造函数
     */
    private TTSToolkit(String token, String host, String path, String voiceType, String encoding, double speedRatio) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token不能为空");
        }
        
        this.token = token;
        this.host = host;
        this.path = path;
        this.defaultVoiceType = voiceType;
        this.defaultEncoding = encoding;
        this.defaultSpeedRatio = speedRatio;
    }
    
    /**
     * 将文本转换为音频字节数组
     * 
     * @param text 要转换的文本
     * @return 音频数据字节数组
     * @throws TTSException TTS转换异常
     */
    public byte[] textToAudioBytes(String text) throws TTSException {
        return textToAudioBytes(text, defaultVoiceType, defaultEncoding, defaultSpeedRatio);
    }
    
    /**
     * 将文本转换为音频字节数组（指定音色）
     * 
     * @param text 要转换的文本
     * @param voiceType 音色类型
     * @return 音频数据字节数组
     * @throws TTSException TTS转换异常
     */
    public byte[] textToAudioBytes(String text, String voiceType) throws TTSException {
        return textToAudioBytes(text, voiceType, defaultEncoding, defaultSpeedRatio);
    }
    
    /**
     * 将文本转换为音频字节数组（完整参数）
     * 
     * @param text 要转换的文本
     * @param voiceType 音色类型
     * @param encoding 编码格式
     * @param speedRatio 语速比例
     * @return 音频数据字节数组
     * @throws TTSException TTS转换异常
     */
    public byte[] textToAudioBytes(String text, String voiceType, String encoding, double speedRatio) throws TTSException {
        // 参数验证
        validateParameters(text, voiceType, encoding, speedRatio);
        
        // 清理文本
        String cleanedText = cleanText(text);
        
        // 生成临时文件名
        String tempFileName = "temp_tts_" + System.currentTimeMillis() + "." + encoding;
        
        try {
            // 执行TTS转换
            convertTextToFile(cleanedText, voiceType, encoding, speedRatio, tempFileName);
            
            // 读取文件为字节数组
            byte[] audioBytes = readFileToBytes(tempFileName);
            
            return audioBytes;
            
        } finally {
            // 清理临时文件
            cleanupTempFile(tempFileName);
        }
    }
    
    /**
     * 异步将文本转换为音频字节数组
     * 
     * @param text 要转换的文本
     * @return CompletableFuture包装的音频数据字节数组
     */
    public CompletableFuture<byte[]> textToAudioBytesAsync(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return textToAudioBytes(text);
            } catch (TTSException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 异步将文本转换为音频字节数组（指定音色）
     * 
     * @param text 要转换的文本
     * @param voiceType 音色类型
     * @return CompletableFuture包装的音频数据字节数组
     */
    public CompletableFuture<byte[]> textToAudioBytesAsync(String text, String voiceType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return textToAudioBytes(text, voiceType);
            } catch (TTSException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 参数验证
     */
    private void validateParameters(String text, String voiceType, String encoding, double speedRatio) throws TTSException {
        if (text == null || text.trim().isEmpty() || text.length() > 10000) {
            throw new TTSException("文本内容无效：文本不能为空且长度不能超过10000字符");
        }
        
        if (!isValidEncoding(encoding)) {
            throw new TTSException("编码格式无效：" + encoding);
        }
        
        if (speedRatio < 0.5 || speedRatio > 2.0) {
            throw new TTSException("语速比例无效：必须在0.5-2.0之间");
        }
    }
    
    /**
     * 验证编码格式
     */
    private boolean isValidEncoding(String encoding) {
        return encoding != null && ("mp3".equals(encoding) || "wav".equals(encoding) || "pcm".equals(encoding));
    }
    
    /**
     * 清理文本内容
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
    }
    
    /**
     * 执行文本转文件的转换
     */
    private void convertTextToFile(String text, String voiceType, String encoding, double speedRatio, String outputFile) throws TTSException {
        try {
            // 构建WebSocket URI
            URI uri = new URI("wss", host, path, null);
            
            // 设置请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);
            headers.put("VoiceType", voiceType);
            
            // 创建WebSocket客户端
            TTSWebSocketClient client = new TTSWebSocketClient(uri, headers, outputFile);
            
            try {
                // 连接WebSocket
                client.connect();
                
                // 等待连接建立
                Thread.sleep(2000);
                
                if (!client.isOpen()) {
                    throw new TTSException("WebSocket连接失败");
                }
                
                // 创建请求对象
                TTSRequest request = createTTSRequest(text, voiceType, encoding, speedRatio);
                
                // 发送请求
                client.sendTTSRequest(request);
                
                // 等待完成（带超时）
                boolean completed = client.waitForCompletion(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!completed) {
                    throw new TTSException("TTS转换超时");
                }
                
            } finally {
                if (client.isOpen()) {
                    client.close();
                }
            }
            
        } catch (Exception e) {
            throw new TTSException("TTS转换失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建TTS请求对象
     */
    private TTSRequest createTTSRequest(String text, String voiceType, String encoding, double speedRatio) {
        Audio audio = new Audio(voiceType, encoding, speedRatio);
        Request request = new Request(text);
        return new TTSRequest(audio, request);
    }
    
    /**
     * 读取文件为字节数组
     */
    private byte[] readFileToBytes(String fileName) throws TTSException {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new TTSException("音频文件生成失败");
        }
        
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            return baos.toByteArray();
            
        } catch (IOException e) {
            throw new TTSException("读取音频文件失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 清理临时文件
     */
    private void cleanupTempFile(String fileName) {
        try {
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            // 忽略清理错误
            System.err.println("清理临时文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取支持的音色列表
     */
    public static String[] getSupportedVoiceTypes() {
        return new String[]{
            "qiniu_zh_female_wwxkjx",
            "qiniu_zh_female_tmjxxy"
        };
    }
    
    /**
     * 获取支持的编码格式列表
     */
    public static String[] getSupportedEncodings() {
        return new String[]{"mp3", "wav", "pcm"};
    }
    
    /**
     * 创建默认配置的工具类实例
     */
    public static TTSToolkit createDefault(String token) {
        return new TTSToolkit(token);
    }
    
    /**
     * 创建快速配置的工具类实例（常用于中文女声）
     */
    public static TTSToolkit createChineseFemale(String token) {
        return new TTSToolkit(token, "qiniu_zh_female_xyqxxj", "mp3", DEFAULT_SPEED_RATIO);
    }
    
    /**
     * 创建快速配置的工具类实例（较快语速）
     */
    public static TTSToolkit createFastSpeech(String token) {
        return new TTSToolkit(token, "qiniu_zh_female_xyqxxj", "mp3", 2.0);
    }
    
    // 内部类定义
    
    /**
     * TTS异常类
     */
    public static class TTSException extends Exception {
        public TTSException(String message) {
            super(message);
        }
        
        public TTSException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * TTS WebSocket客户端
     */
    private static class TTSWebSocketClient extends WebSocketClient {
        
        private final ObjectMapper objectMapper;
        private final List<byte[]> audioDataList;
        private final String outputFile;
        private final CountDownLatch latch;
        private boolean isCompleted = false;
        
        public TTSWebSocketClient(URI serverUri, Map<String, String> headers, String outputFile) {
            super(serverUri, headers);
            this.objectMapper = new ObjectMapper();
            this.audioDataList = new ArrayList<>();
            this.outputFile = outputFile;
            this.latch = new CountDownLatch(1);
        }
        
        @Override
        public void onOpen(ServerHandshake handshake) {
            System.out.println("WebSocket连接已建立");
        }
        
        @Override
        public void onMessage(String message) {
            try {
                // 解析响应
                TTSResponse response = objectMapper.readValue(message, TTSResponse.class);
                
                // 处理音频数据
                if (response.getData() != null && !response.getData().isEmpty()) {
                    byte[] audioData = Base64.getDecoder().decode(response.getData());
                    audioDataList.add(audioData);
                }
                
                // 检查是否完成
                if (response.getSequence() < 0) {
                    saveAudioFile();
                    isCompleted = true;
                    latch.countDown();
                    close();
                }
                
            } catch (Exception e) {
                System.err.println("处理消息时出错: " + e.getMessage());
                latch.countDown();
            }
        }
        
        @Override
        public void onMessage(ByteBuffer bytes) {
            // 处理二进制消息
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (!isCompleted) {
                latch.countDown();
            }
        }
        
        @Override
        public void onError(Exception ex) {
            System.err.println("WebSocket连接出错: " + ex.getMessage());
            latch.countDown();
        }
        
        public void sendTTSRequest(TTSRequest request) {
            try {
                String jsonRequest = objectMapper.writeValueAsString(request);
                send(jsonRequest.getBytes());
            } catch (Exception e) {
                System.err.println("发送请求时出错: " + e.getMessage());
            }
        }
        
        public boolean waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
        
        private void saveAudioFile() {
            try {
                int totalSize = audioDataList.stream().mapToInt(data -> data.length).sum();
                byte[] completeAudio = new byte[totalSize];
                int offset = 0;
                for (byte[] audioData : audioDataList) {
                    System.arraycopy(audioData, 0, completeAudio, offset, audioData.length);
                    offset += audioData.length;
                }
                
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(completeAudio);
                    fos.flush();
                }
                
            } catch (IOException e) {
                System.err.println("保存音频文件时出错: " + e.getMessage());
            }
        }
    }
    
    // 数据模型类
    
    /**
     * TTS请求类
     */
    private static class TTSRequest {
        private Audio audio;
        private Request request;
        
        public TTSRequest(Audio audio, Request request) {
            this.audio = audio;
            this.request = request;
        }
        
        public Audio getAudio() { return audio; }
        public void setAudio(Audio audio) { this.audio = audio; }
        public Request getRequest() { return request; }
        public void setRequest(Request request) { this.request = request; }
    }
    
    /**
     * 音频配置类
     */
    private static class Audio {
        private String voiceType;
        private String encoding;
        private double speedRatio;
        
        public Audio(String voiceType, String encoding, double speedRatio) {
            this.voiceType = voiceType;
            this.encoding = encoding;
            this.speedRatio = speedRatio;
        }
        
        public String getVoiceType() { return voiceType; }
        public void setVoiceType(String voiceType) { this.voiceType = voiceType; }
        public String getEncoding() { return encoding; }
        public void setEncoding(String encoding) { this.encoding = encoding; }
        public double getSpeedRatio() { return speedRatio; }
        public void setSpeedRatio(double speedRatio) { this.speedRatio = speedRatio; }
    }
    
    /**
     * 请求内容类
     */
    private static class Request {
        private String text;
        
        public Request(String text) {
            this.text = text;
        }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
    
    /**
     * TTS响应类
     */
    private static class TTSResponse {
        private String reqid;
        private String operation;
        private int sequence;
        private String data;
        
        public String getReqid() { return reqid; }
        public void setReqid(String reqid) { this.reqid = reqid; }
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }
}

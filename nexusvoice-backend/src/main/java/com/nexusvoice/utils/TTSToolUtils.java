package com.nexusvoice.utils;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusvoice.exception.TTSException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @Author AJ
 * @Date 2025-09-24 0:45
 * @Description 面向业务的TTS工具类：接收文本，返回音频字节数组，便于直接返回给前端
 */
public class TTSToolUtils {

    // === Inlined defaults (remove dependency on TTSConfig) ===
    private static final String DEFAULT_HOST = "openai.qiniu.com";
    private static final String DEFAULT_PATH = "/v1/voice/tts";
    private static final String DEFAULT_VOICE = "qiniu_zh_female_wwxkjx";
    private static final String DEFAULT_ENCODING = "mp3";
    private static final double DEFAULT_SPEED = 1.0;
    private static final int READ_TIMEOUT_MS = 60_000; // 60 seconds

    private final String token;
    private final String host;
    private final String path;
    private final String defaultVoiceType;
    private final String defaultEncoding;
    private final double defaultSpeed;

    private TTSToolUtils(String token,
                       String host,
                       String path,
                       String defaultVoiceType,
                       String defaultEncoding,
                       double defaultSpeed) {
        this.token = token;
        this.host = host;
        this.path = path;
        this.defaultVoiceType = defaultVoiceType;
        this.defaultEncoding = defaultEncoding;
        this.defaultSpeed = defaultSpeed;
    }

    /**
     * 使用默认配置创建工具类
     */
    public static TTSToolUtils createDefault(String token) {
        return new TTSToolUtils(
                token,
                DEFAULT_HOST,
                DEFAULT_PATH,
                DEFAULT_VOICE,
                DEFAULT_ENCODING,
                DEFAULT_SPEED
        );
    }

    /**
     * 使用自定义的默认配置创建工具类
     */
    public static TTSToolUtils createWithDefaults(String token,
                                                String defaultVoiceType,
                                                String defaultEncoding,
                                                double defaultSpeed) {
        return new TTSToolUtils(
                token,
                DEFAULT_HOST,
                DEFAULT_PATH,
                defaultVoiceType,
                defaultEncoding,
                defaultSpeed
        );
    }

    /**
     * 文本转音频（使用默认配置）
     */
    public byte[] textToAudioBytes(String text) throws TTSException {
        return textToAudioBytes(text, defaultVoiceType, defaultEncoding, defaultSpeed);
    }

    /**
     * 文本转音频（自定义配置）
     */
    public byte[] textToAudioBytes(String text, String voiceType, String encoding, double speedRatio) throws TTSException {
        // 参数校验
        if (!isValidText(text)) {
            throw new TTSException("文本内容无效：文本不能为空且长度不能超过10000字符");
        }
        if (!isValidEncoding(encoding)) {
            throw new TTSException("编码格式无效：" + encoding);
        }
        if (!isValidSpeedRatio(speedRatio)) {
            throw new TTSException("语速比例无效：必须在0.5-2.0之间");
        }

        String cleanedText = cleanText(text);
        TTSRequestModel request = createTTSRequest(cleanedText, voiceType, encoding, speedRatio);

        try {
            // 构建 WebSocket 连接信息
            URI uri = new URI("wss", host, path, null);

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + token);
            headers.put("VoiceType", voiceType);

            // 创建收集音频字节的 WebSocket 客户端
            CollectingWebSocketClient client = new CollectingWebSocketClient(uri, headers);

            // 连接：阻塞等待握手完成，提升并发稳定性
            boolean connected = client.connectBlocking(5, TimeUnit.SECONDS);
            if (!connected || !client.isOpen()) {
                throw new TTSException("WebSocket连接失败");
            }

            // 发送请求
            client.sendTTSRequest(request);

            // 等待完成（读取超时）
            boolean finished = client.waitForCompletion(READ_TIMEOUT_MS);
            if (!finished) {
                throw new TTSException("TTS合成超时");
            }

            // 返回音频字节
            return client.getAudioBytes();
        } catch (TTSException e) {
            throw e;
        } catch (Exception e) {
            throw new TTSException("TTS处理失败：" + e.getMessage(), e);
        }
    }

    /**
     * 文本转音频文件（使用默认配置）
     * 直接返回MultipartFile，便于上传到云存储
     */
    public MultipartFile textToAudioFile(String text) throws TTSException {
        return textToAudioFile(text, defaultVoiceType, defaultEncoding, defaultSpeed);
    }

    /**
     * 文本转音频文件（自定义配置）
     * 直接返回MultipartFile，便于上传到云存储
     */
    public MultipartFile textToAudioFile(String text, String voiceType, String encoding, double speedRatio) throws TTSException {
        // 生成音频字节数组
        byte[] audioBytes = textToAudioBytes(text, voiceType, encoding, speedRatio);
        
        // 生成文件名
        String fileName = "tts_audio_" + System.currentTimeMillis() + "." + encoding;
        
        // 创建MultipartFile
        return new ByteArrayMultipartFile(
            "audio",
            fileName,
            "audio/" + encoding,
            audioBytes
        );
    }

    /**
     * 文本转音频（异步）
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
     * 文本转音频文件（异步）
     */
    public CompletableFuture<MultipartFile> textToAudioFileAsync(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return textToAudioFile(text);
            } catch (TTSException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 内部 WebSocket 客户端：收集服务端分片音频并合并为字节数组
     */
    static class CollectingWebSocketClient extends WebSocketClient {
        private final ObjectMapper mapper = new ObjectMapper();
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final CountDownLatch done = new CountDownLatch(1);
        private volatile boolean completed = false;

        CollectingWebSocketClient(URI serverUri, Map<String, String> headers) {
            super(serverUri, headers);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("[TTSToolkit] WebSocket已连接: " + handshakedata.getHttpStatus() + " " + handshakedata.getHttpStatusMessage());
        }

        @Override
        public void onMessage(String message) {
            try {
                TTSResponseModel resp = mapper.readValue(message, TTSResponseModel.class);
                if (resp.getData() != null && !resp.getData().isEmpty()) {
                    byte[] chunk = Base64.getDecoder().decode(resp.getData());
                    buffer.write(chunk);
                }
                if (resp.getSequence() < 0) {
                    completed = true;
                    done.countDown();
                    close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                done.countDown();
            }
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            // 若服务端返回二进制，可在此处理；当前协议主要通过JSON文本返回Base64数据
            System.out.println("[TTSToolkit] 收到二进制帧: " + bytes.remaining() + " bytes");
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("[TTSToolkit] WebSocket已关闭: code=" + code + ", reason=" + reason + ", remote=" + remote);
            if (!completed) {
                done.countDown();
            }
        }

        @Override
        public void onError(Exception ex) {
            System.err.println("[TTSToolkit] WebSocket错误: " + ex.getMessage());
            done.countDown();
        }

        void sendTTSRequest(TTSRequestModel req) throws Exception {
            String json = mapper.writeValueAsString(req);
            send(json.getBytes()); // 以二进制帧发送，与现有TTSService保持一致
        }

        boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
            return done.await(timeoutMillis, TimeUnit.MILLISECONDS);
        }

        byte[] getAudioBytes() {
            return buffer.toByteArray();
        }
    }

    // === Inlined utility methods (remove dependency on TTSUtil) ===
    private static boolean isValidText(String text) {
        return text != null && !text.trim().isEmpty() && text.length() <= 10000;
    }

    private static boolean isValidEncoding(String encoding) {
        if (encoding == null) return false;
        String e = encoding.toLowerCase();
        return e.equals("mp3") || e.equals("wav") || e.equals("pcm");
    }

    private static boolean isValidSpeedRatio(double speedRatio) {
        return speedRatio >= 0.5 && speedRatio <= 2.0;
    }

    private static String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
    }

    private static TTSRequestModel createTTSRequest(String text, String voiceType, String encoding, double speedRatio) {
        AudioModel audio = new AudioModel();
        audio.voiceType = voiceType;
        audio.encoding = encoding;
        audio.speedRatio = speedRatio;

        TextRequest req = new TextRequest();
        req.text = text;

        TTSRequestModel model = new TTSRequestModel();
        model.audio = audio;
        model.request = req;
        return model;
    }

    // === Inlined request/response models (remove dependency on com.aj.tts.model) ===
    static class TTSRequestModel {
        @JsonProperty("audio")
        public AudioModel audio;

        @JsonProperty("request")
        public TextRequest request;
    }

    static class AudioModel {
        @JsonProperty("voice_type")
        public String voiceType;

        @JsonProperty("encoding")
        public String encoding;

        @JsonProperty("speed_ratio")
        public double speedRatio;
    }

    static class TextRequest {
        @JsonProperty("text")
        public String text;
    }

    static class TTSResponseModel {
        @JsonProperty("reqid")
        private String reqid;

        @JsonProperty("operation")
        private String operation;

        @JsonProperty("sequence")
        private int sequence;

        @JsonProperty("data")
        private String data;

        @JsonProperty("addition")
        private AdditionModel addition;

        public String getReqid() { return reqid; }
        public String getOperation() { return operation; }
        public int getSequence() { return sequence; }
        public String getData() { return data; }
        public AdditionModel getAddition() { return addition; }
    }

    static class AdditionModel {
        @JsonProperty("duration")
        public String duration;
    }

    /**
     * 自定义的MultipartFile实现，用于包装字节数组
     */
    static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content.clone();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            throw new UnsupportedOperationException("transferTo not supported for ByteArrayMultipartFile");
        }
    }
}

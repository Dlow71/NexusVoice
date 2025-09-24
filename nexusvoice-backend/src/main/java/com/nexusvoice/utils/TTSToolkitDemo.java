package com.nexusvoice.utils;

import com.nexusvoice.exception.TTSException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * TTSToolkit使用示例
 * 演示如何在NexusVoice项目中将模型回复转换为语音返回给前端
 * 
 * @author NexusVoice Team
 */
public class TTSToolkitDemo {

    // 请替换为你的实际 Token
    private static final String TOKEN = "sk-bf66a8b078c5848e18e7a9c1970319253cd6def1bfe8a00b1b4e562f3f8843f9";

    public static void main(String[] args) {

        TTSToolUtils toolkit = TTSToolUtils.createWithDefaults(TOKEN,"qiniu_zh_female_wwxkjx", "mp3", 2.0);
        String text = "你好，这是一段用于演示 TTSToolkit 的文本。";

        try {
            System.out.println("开始将文本转换为音频：" + text);
            byte[] audioBytes = toolkit.textToAudioBytes(text);
            System.out.println("合成完成，音频字节数：" + (audioBytes == null ? 0 : audioBytes.length));

            // 将音频字节保存为本地文件，供本地试听验证
            String outFile = "toolkit_demo.mp3";
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(audioBytes);
            }
            System.out.println("已写入文件：" + outFile);
        } catch (TTSException e) {
            System.err.println("TTS 失败：" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("异常：" + e.getMessage());
            e.printStackTrace();
        }
    }
}

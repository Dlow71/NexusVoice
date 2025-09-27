package com.nexusvoice.exception;

/**
 * @Author AJ
 * @Date 2025-09-24 0:50
 * @Description 自定义TTS异常
 */
public class TTSException extends Exception{

    public TTSException(String message) {
        super(message);
    }

    public TTSException(String message, Throwable cause) {
        super(message, cause);
    }
}

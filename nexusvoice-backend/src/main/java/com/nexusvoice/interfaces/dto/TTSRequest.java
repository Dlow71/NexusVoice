package com.nexusvoice.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author AJ
 * @Date 2025-09-24 0:39
 * @Description TTS请求类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TTSRequest {

    @JsonProperty("audio")
    private Object audio;

    @JsonProperty("request")
    private Object request;
}

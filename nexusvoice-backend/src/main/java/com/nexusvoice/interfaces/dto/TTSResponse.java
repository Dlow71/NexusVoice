package com.nexusvoice.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author AJ
 * @Date 2025-09-24 0:27
 * @Description TTS响应类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TTSResponse {

    @JsonProperty("reqid")
    private String reqid;

    @JsonProperty("operation")
    private String operation;

    @JsonProperty("sequence")
    private int sequence;

    @JsonProperty("data")
    private String data;

    @JsonProperty("addition")
    private Object addition;
}

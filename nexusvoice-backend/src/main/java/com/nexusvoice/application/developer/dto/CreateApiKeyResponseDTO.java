package com.nexusvoice.application.developer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 创建开发者API密钥响应DTO
 * 包含完整的API密钥值（仅在创建时返回一次）
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "创建开发者API密钥响应（包含完整密钥值）")
public class CreateApiKeyResponseDTO extends ApiKeyResponseDTO {
    
    @Schema(description = "完整的API Key密钥值（仅在创建时返回，请妥善保存）", 
            example = "sk-nv-a1b2c3d4e5f6g7h8i9j0k1l2-abc123")
    private String apiKey;
    
    @Schema(description = "重要提示", 
            example = "请妥善保存此密钥，离开此页面后将无法再次查看完整密钥")
    private String warning;
    
    public CreateApiKeyResponseDTO() {
        this.warning = "请妥善保存此密钥，离开此页面后将无法再次查看完整密钥";
    }
}

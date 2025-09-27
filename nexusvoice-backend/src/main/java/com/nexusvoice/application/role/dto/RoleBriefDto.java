package com.nexusvoice.application.role.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色草稿（Brief）DTO
 * 用于对话总结阶段的结构化结果，避免直接存思维链，仅保存结论与参考来源。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色草稿")
public class RoleBriefDto {

    @Schema(description = "角色名称", example = "霍格沃茨年轻魔法研究员")
    private String name;

    @Schema(description = "角色描述", example = "聪明好奇，擅长将复杂知识讲得通俗易懂")
    private String description;

    @Schema(description = "人设提示词", example = "说话温和、条理清晰，喜欢用类比解释概念，避免剧透")
    private String personaPrompt;

    @Schema(description = "开场白文本", example = "你好，我是你的魔法学习伙伴，我们从基础开始")
    private String greetingMessage;

    @Schema(description = "头像URL")
    private String avatarUrl;

    @Schema(description = "TTS声音类型", example = "default")
    private String voiceType;

    @Schema(description = "参考来源列表")
    private List<SourceItem> sources;

    @Schema(description = "合规与边界声明")
    private List<String> disclaimers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "参考来源条目")
    public static class SourceItem {
        @Schema(description = "标题")
        private String title;
        @Schema(description = "URL")
        private String url;
        @Schema(description = "摘要")
        private String snippet;
    }
}


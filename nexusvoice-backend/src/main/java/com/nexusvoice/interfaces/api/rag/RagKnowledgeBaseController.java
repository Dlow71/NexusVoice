package com.nexusvoice.interfaces.api.rag;

import com.nexusvoice.annotation.RequireAuth;
import com.nexusvoice.application.rag.dto.KnowledgeBaseCreateRequest;
import com.nexusvoice.application.rag.dto.KnowledgeBaseFileResponse;
import com.nexusvoice.application.rag.dto.KnowledgeBaseResponse;
import com.nexusvoice.application.rag.dto.RagCitationContextDto;
import com.nexusvoice.application.rag.dto.RagSearchResultDto;
import com.nexusvoice.application.rag.service.RagApplicationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@Validated
@RequireAuth
@RequiredArgsConstructor
@RequestMapping("/api/v1/rag/knowledge-bases")
@Tag(name = "RAG知识库", description = "知识库管理与文档入库接口")
public class RagKnowledgeBaseController {

    private final RagApplicationService ragApplicationService;

    @GetMapping
    @Operation(summary = "获取当前用户知识库列表")
    public Result<List<KnowledgeBaseResponse>> listKnowledgeBases() {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        return Result.success(ragApplicationService.listKnowledgeBases(userId));
    }

    @PostMapping
    @Operation(summary = "创建知识库")
    public Result<KnowledgeBaseResponse> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        return Result.success(ragApplicationService.createKnowledgeBase(request, userId));
    }

    @GetMapping("/{knowledgeBaseId}")
    @Operation(summary = "获取知识库详情")
    public Result<KnowledgeBaseResponse> getKnowledgeBase(
            @Parameter(description = "知识库ID") @PathVariable Long knowledgeBaseId) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        return Result.success(ragApplicationService.getKnowledgeBase(knowledgeBaseId, userId));
    }

    @PostMapping("/{knowledgeBaseId}/files")
    @Operation(summary = "上传文档并同步完成RAG入库")
    public Result<KnowledgeBaseFileResponse> uploadFile(
            @Parameter(description = "知识库ID") @PathVariable Long knowledgeBaseId,
            @RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        log.info("上传RAG文档，用户ID：{}，知识库ID：{}，文件名：{}", userId, knowledgeBaseId, file.getOriginalFilename());
        return Result.success(ragApplicationService.uploadDocument(knowledgeBaseId, file, userId));
    }

    @GetMapping("/{knowledgeBaseId}/search")
    @Operation(summary = "测试知识库检索")
    public Result<List<RagSearchResultDto>> search(
            @PathVariable Long knowledgeBaseId,
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        return Result.success(ragApplicationService.search(knowledgeBaseId, query, limit, userId));
    }

    @GetMapping("/{knowledgeBaseId}/files/{fileId}/context")
    @Operation(summary = "获取引用文件的原文上下文")
    public Result<RagCitationContextDto> getCitationContext(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long fileId,
            @RequestParam("location") String location,
            @RequestParam(value = "window", defaultValue = "1") Integer window) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        return Result.success(ragApplicationService.getCitationContext(knowledgeBaseId, fileId, location, window, userId));
    }

    @DeleteMapping("/{knowledgeBaseId}/files/{fileId}")
    @Operation(summary = "删除知识库文件")
    public Result<Void> deleteFile(@PathVariable Long knowledgeBaseId, @PathVariable Long fileId) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        ragApplicationService.deleteFile(knowledgeBaseId, fileId, userId);
        return Result.success();
    }

    @DeleteMapping("/{knowledgeBaseId}")
    @Operation(summary = "删除知识库")
    public Result<Void> deleteKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        Long userId = SecurityUtils.getCurrentUserId().orElseThrow();
        ragApplicationService.deleteKnowledgeBase(knowledgeBaseId, userId);
        return Result.success();
    }
}

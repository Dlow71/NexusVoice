package com.nexusvoice.interfaces.api.admin;

import com.nexusvoice.annotation.RequireAdmin;
import com.nexusvoice.application.ai.dto.provider.AiProviderDTO;
import com.nexusvoice.application.ai.dto.provider.CreateAiProviderDTO;
import com.nexusvoice.application.ai.dto.provider.UpdateAiProviderDTO;
import com.nexusvoice.application.ai.service.AiProviderApplicationService;
import com.nexusvoice.common.Result;
import com.nexusvoice.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员-AI服务商管理控制器
 *
 * @author NexusVoice
 * @since 2026-03-19
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ai-providers")
@RequireAdmin
@Tag(name = "管理员-AI服务商管理", description = "AI服务商配置管理接口")
public class AdminAiProviderController {

    @Autowired
    private AiProviderApplicationService aiProviderApplicationService;

    @GetMapping
    @Operation(summary = "查询全部AI服务商", description = "查询全部未删除的AI服务商配置")
    public Result<List<AiProviderDTO>> listProviders() {
        List<AiProviderDTO> providers = aiProviderApplicationService.getAllProviders();
        log.info("管理员查询AI服务商列表，数量：{}", providers.size());
        return Result.success(providers);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询AI服务商详情", description = "根据ID查询AI服务商详情")
    public Result<AiProviderDTO> getProvider(
            @Parameter(description = "服务商ID", required = true) @PathVariable Long id) {
        return Result.success(aiProviderApplicationService.getProviderById(id));
    }

    @PostMapping
    @Operation(summary = "新增AI服务商", description = "管理员新增官方AI服务商")
    public Result<AiProviderDTO> createProvider(@Valid @RequestBody CreateAiProviderDTO request) {
        AiProviderDTO provider = aiProviderApplicationService.createOfficialProvider(request);
        log.info("管理员新增AI服务商成功，ID：{}，code：{}", provider.getId(), provider.getProviderCode());
        return Result.success("创建成功", provider);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新AI服务商", description = "管理员更新AI服务商配置")
    public Result<AiProviderDTO> updateProvider(
            @Parameter(description = "服务商ID", required = true) @PathVariable Long id,
            @Valid @RequestBody UpdateAiProviderDTO request) {
        Long currentUserId = getCurrentUserId();
        AiProviderDTO provider = aiProviderApplicationService.updateProvider(id, request, currentUserId, true);
        log.info("管理员更新AI服务商成功，ID：{}", id);
        return Result.success("更新成功", provider);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新AI服务商状态", description = "启用或禁用AI服务商")
    public Result<Void> updateProviderStatus(
            @Parameter(description = "服务商ID", required = true) @PathVariable Long id,
            @Parameter(description = "状态：1启用 0禁用", required = true) @RequestParam Integer status) {
        Long currentUserId = getCurrentUserId();
        if (status == null || (status != 0 && status != 1)) {
            return Result.error("状态值无效，只支持0或1");
        }

        if (status == 1) {
            aiProviderApplicationService.enableProvider(id, currentUserId, true);
        } else {
            aiProviderApplicationService.disableProvider(id, currentUserId, true);
        }

        log.info("管理员更新AI服务商状态成功，ID：{}，status：{}", id, status);
        return Result.success("状态更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除AI服务商", description = "删除指定AI服务商")
    public Result<Void> deleteProvider(
            @Parameter(description = "服务商ID", required = true) @PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        aiProviderApplicationService.deleteProvider(id, currentUserId, true);
        log.info("管理员删除AI服务商成功，ID：{}", id);
        return Result.success("删除成功");
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId().orElse(0L);
    }
}

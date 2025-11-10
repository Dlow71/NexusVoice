package com.nexusvoice.application.ai.service;

import com.nexusvoice.application.ai.assembler.AiProviderAssembler;
import com.nexusvoice.application.ai.dto.provider.AiProviderDTO;
import com.nexusvoice.application.ai.dto.provider.CreateAiProviderDTO;
import com.nexusvoice.application.ai.dto.provider.UpdateAiProviderDTO;
import com.nexusvoice.domain.ai.model.AiProvider;
import com.nexusvoice.domain.ai.repository.AiModelRepository;
import com.nexusvoice.domain.ai.repository.AiProviderRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI服务提供商应用服务
 * 处理服务商相关的业务逻辑
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
@Slf4j
@Service
public class AiProviderApplicationService {
    
    @Autowired
    private AiProviderRepository providerRepository;
    
    @Autowired
    private AiProviderAssembler assembler;

    @Autowired(required = false)
    private AiModelRepository modelRepository;
    
    /**
     * 查询所有启用的服务商
     *
     * @return 服务商DTO列表
     */
    public List<AiProviderDTO> getAllEnabledProviders() {
        List<AiProvider> providers = providerRepository.findAllEnabled();
        return assembler.toDTOList(providers);
    }
    
    /**
     * 查询所有官方服务商
     *
     * @return 官方服务商DTO列表
     */
    public List<AiProviderDTO> getAllOfficialProviders() {
        List<AiProvider> providers = providerRepository.findAllOfficial();
        return assembler.toDTOList(providers);
    }
    
    /**
     * 查询用户的自定义服务商
     *
     * @param userId 用户ID
     * @return 用户自定义服务商DTO列表
     */
    public List<AiProviderDTO> getUserCustomProviders(Long userId) {
        List<AiProvider> providers = providerRepository.findByUserId(userId);
        return assembler.toDTOList(providers);
    }
    
    /**
     * 根据ID查询服务商
     *
     * @param id 服务商ID
     * @return 服务商DTO
     */
    public AiProviderDTO getProviderById(Long id) {
        AiProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "服务商不存在"));
        return assembler.toDTO(provider);
    }
    
    /**
     * 根据代码查询服务商
     *
     * @param providerCode 服务商代码
     * @return 服务商DTO
     */
    public AiProviderDTO getProviderByCode(String providerCode) {
        AiProvider provider = providerRepository.findByCode(providerCode)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "服务商不存在"));
        return assembler.toDTO(provider);
    }
    
    /**
     * 用户创建自定义服务商
     *
     * @param dto 创建DTO
     * @param userId 用户ID
     * @return 创建的服务商DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public AiProviderDTO createCustomProvider(CreateAiProviderDTO dto, Long userId) {
        log.info("用户{}创建自定义服务商，代码：{}", userId, dto.getProviderCode());
        
        // 检查代码是否已存在
        if (providerRepository.existsByCode(dto.getProviderCode())) {
            throw new BizException(ErrorCodeEnum.DATA_ALREADY_EXISTS, 
                    "服务商代码已存在：" + dto.getProviderCode());
        }
        
        // 转换为领域实体
        AiProvider provider = assembler.toDomain(dto, userId);
        
        // 验证实体
        try {
            provider.validate();
        } catch (IllegalStateException e) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, e.getMessage());
        }
        
        // 保存
        AiProvider saved = providerRepository.save(provider);
        
        log.info("用户{}创建自定义服务商成功，ID：{}", userId, saved.getId());
        return assembler.toDTO(saved);
    }
    
    /**
     * 管理员创建官方服务商
     *
     * @param dto 创建DTO
     * @return 创建的服务商DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public AiProviderDTO createOfficialProvider(CreateAiProviderDTO dto) {
        log.info("管理员创建官方服务商，代码：{}", dto.getProviderCode());
        
        // 检查代码是否已存在
        if (providerRepository.existsByCode(dto.getProviderCode())) {
            throw new BizException(ErrorCodeEnum.DATA_ALREADY_EXISTS, 
                    "服务商代码已存在：" + dto.getProviderCode());
        }
        
        // 转换为官方服务商实体
        AiProvider provider = assembler.toOfficialDomain(dto);
        
        // 验证实体
        try {
            provider.validate();
        } catch (IllegalStateException e) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, e.getMessage());
        }
        
        // 保存
        AiProvider saved = providerRepository.save(provider);
        
        log.info("管理员创建官方服务商成功，ID：{}", saved.getId());
        return assembler.toDTO(saved);
    }
    
    /**
     * 更新服务商
     *
     * @param id 服务商ID
     * @param dto 更新DTO
     * @param userId 当前用户ID（用于权限检查）
     * @param isAdmin 是否管理员
     * @return 更新后的服务商DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public AiProviderDTO updateProvider(Long id, UpdateAiProviderDTO dto, Long userId, boolean isAdmin) {
        log.info("更新服务商，ID：{}，用户：{}", id, userId);
        
        // 查询服务商
        AiProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "服务商不存在"));
        
        // 权限检查
        if (!isAdmin && !provider.hasPermission(userId)) {
            throw new BizException(ErrorCodeEnum.PERMISSION_DENIED, "无权操作此服务商");
        }
        
        // 更新实体
        assembler.updateDomain(provider, dto);
        
        // 验证实体
        try {
            provider.validate();
        } catch (IllegalStateException e) {
            throw new BizException(ErrorCodeEnum.PARAM_ERROR, e.getMessage());
        }
        
        // 保存
        AiProvider updated = providerRepository.save(provider);
        
        log.info("更新服务商成功，ID：{}", id);
        return assembler.toDTO(updated);
    }
    
    /**
     * 删除服务商
     *
     * @param id 服务商ID
     * @param userId 当前用户ID（用于权限检查）
     * @param isAdmin 是否管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(Long id, Long userId, boolean isAdmin) {
        log.info("删除服务商，ID：{}，用户：{}", id, userId);
        
        // 查询服务商
        AiProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "服务商不存在"));
        
        // 权限检查
        if (!isAdmin && !provider.hasPermission(userId)) {
            throw new BizException(ErrorCodeEnum.PERMISSION_DENIED, "无权操作此服务商");
        }
        
        // 官方服务商只能由管理员删除
        if (provider.isOfficialProvider() && !isAdmin) {
            throw new BizException(ErrorCodeEnum.PERMISSION_DENIED, "官方服务商只能由管理员删除");
        }
        
        // 关联检查：若仍存在关联模型，则不允许删除
        if (modelRepository != null) {
            try {
                var relatedModels = modelRepository.findByProvider(provider.getProviderCode());
                if (relatedModels != null && !relatedModels.isEmpty()) {
                    throw new BizException(ErrorCodeEnum.OPERATION_FAILED, "该服务商下存在模型，不允许删除");
                }
            } catch (BizException be) {
                throw be;
            } catch (Exception e) {
                // 如果查询异常，出于安全起见阻止删除
                throw new BizException(ErrorCodeEnum.OPERATION_FAILED, "检查关联模型失败，暂不允许删除");
            }
        }
        
        // 删除
        boolean success = providerRepository.delete(id);
        if (!success) {
            throw new BizException(ErrorCodeEnum.OPERATION_FAILED, "删除服务商失败");
        }
        
        log.info("删除服务商成功，ID：{}", id);
    }
    
    /**
     * 启用服务商
     *
     * @param id 服务商ID
     * @param userId 当前用户ID
     * @param isAdmin 是否管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void enableProvider(Long id, Long userId, boolean isAdmin) {
        updateProviderStatus(id, userId, isAdmin, 1, "启用");
    }
    
    /**
     * 禁用服务商
     *
     * @param id 服务商ID
     * @param userId 当前用户ID
     * @param isAdmin 是否管理员
     */
    @Transactional(rollbackFor = Exception.class)
    public void disableProvider(Long id, Long userId, boolean isAdmin) {
        updateProviderStatus(id, userId, isAdmin, 0, "禁用");
    }
    
    /**
     * 更新服务商状态
     */
    private void updateProviderStatus(Long id, Long userId, boolean isAdmin, Integer status, String action) {
        log.info("{}服务商，ID：{}，用户：{}", action, id, userId);
        
        // 查询服务商
        AiProvider provider = providerRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCodeEnum.DATA_NOT_FOUND, "服务商不存在"));
        
        // 权限检查
        if (!isAdmin && !provider.hasPermission(userId)) {
            throw new BizException(ErrorCodeEnum.PERMISSION_DENIED, "无权操作此服务商");
        }
        
        // 更新状态
        boolean success = providerRepository.updateStatus(id, status);
        if (!success) {
            throw new BizException(ErrorCodeEnum.OPERATION_FAILED, action + "服务商失败");
        }
        
        log.info("{}服务商成功，ID：{}", action, id);
    }
    
    /**
     * 统计服务商数量
     *
     * @param isOfficial 是否官方（null表示不限）
     * @param status 状态（null表示不限）
     * @return 数量
     */
    public long countProviders(Boolean isOfficial, Integer status) {
        return providerRepository.count(isOfficial, status);
    }
}

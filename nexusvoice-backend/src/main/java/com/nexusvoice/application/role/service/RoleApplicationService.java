package com.nexusvoice.application.role.service;

import com.nexusvoice.application.role.assembler.RoleAssembler;
import com.nexusvoice.application.role.dto.RoleCreateRequest;
import com.nexusvoice.application.role.dto.RoleDTO;
import com.nexusvoice.application.role.dto.RoleUpdateRequest;
import com.nexusvoice.application.tts.dto.TTSRequestDTO;
import com.nexusvoice.application.tts.dto.TTSResponseDTO;
import com.nexusvoice.application.tts.service.TTSService;
import com.nexusvoice.application.user.dto.PageResult;
import com.nexusvoice.domain.role.model.Role;
import com.nexusvoice.domain.role.repository.RoleRepository;
import com.nexusvoice.enums.ErrorCodeEnum;
import com.nexusvoice.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 角色应用服务
 * 负责AI角色的业务编排与权限校验（资源归属）
 */
@Service
public class RoleApplicationService {

    private static final Logger log = LoggerFactory.getLogger(RoleApplicationService.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TTSService ttsService;

    // ======================= 公共方法（公共角色浏览） =======================

    /**
     * 分页查询公共角色（用户/管理员通用）
     */
    public PageResult<RoleDTO> pagePublicRoles(Integer page, Integer size, String keyword) {
        PageResult<Role> pageResult = roleRepository.pagePublicRoles(page, size, keyword);
        List<RoleDTO> list = RoleAssembler.toDTOList(pageResult.getRecords());
        return new PageResult<>(list, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    /**
     * 获取公共角色详情
     */
    public RoleDTO getPublicRoleDetail(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.ROLE_NOT_FOUND, "角色不存在"));
        if (!Boolean.TRUE.equals(role.getIsPublic())) {
            throw BizException.of(ErrorCodeEnum.PERMISSION_DENIED, "该角色不是公共角色");
        }
        return RoleAssembler.toDTO(role);
    }

    /**
     * 获取角色信息用于聊天（支持公共角色和用户自己的私人角色）
     */
    public Role getRoleForChat(Long roleId, Long currentUserId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.ROLE_NOT_FOUND, "角色不存在"));
        
        // 如果是公共角色，任何人都可以使用
        if (Boolean.TRUE.equals(role.getIsPublic())) {
            return role;
        }
        
        // 如果是私人角色，只有创建者可以使用
        if (role.ownedBy(currentUserId)) {
            return role;
        }
        
        throw BizException.of(ErrorCodeEnum.PERMISSION_DENIED, "无权使用该角色");
    }

    // ======================= 管理端 - 公共角色管理 =======================

    /**
     * 管理员创建公共角色
     */
    @Transactional
    public RoleDTO createPublicRole(RoleCreateRequest request) {
        validateCreateRequest(request);
        Role role = RoleAssembler.fromCreateRequest(request);
        role.makePublic();
        
        // 如果有开场白文本，生成对应的音频
        generateGreetingAudio(role);
        
        Role saved = roleRepository.save(role);
        log.info("创建公共角色成功: {} - {}", saved.getId(), saved.getName());
        return RoleAssembler.toDTO(saved);
    }

    /**
     * 管理员编辑公共角色
     */
    @Transactional
    public void updatePublicRole(Long roleId, RoleUpdateRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.ROLE_NOT_FOUND, "角色不存在"));
        if (!Boolean.TRUE.equals(role.getIsPublic())) {
            throw BizException.of(ErrorCodeEnum.PERMISSION_DENIED, "只能编辑公共角色");
        }
        RoleAssembler.copyToRole(request, role);
        roleRepository.update(role);
        log.info("更新公共角色成功: {}", roleId);
    }

    /**
     * 管理员删除公共角色（逻辑删除）
     */
    @Transactional
    public void deletePublicRole(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> BizException.of(ErrorCodeEnum.ROLE_NOT_FOUND, "角色不存在"));
        if (!Boolean.TRUE.equals(role.getIsPublic())) {
            throw BizException.of(ErrorCodeEnum.PERMISSION_DENIED, "只能删除公共角色");
        }
        roleRepository.deleteById(roleId);
        log.info("删除公共角色成功: {}", roleId);
    }

    /**
     * 管理员分页查看所有用户的私人角色，支持按用户过滤
     */
    public PageResult<RoleDTO> pageAllPrivateRoles(Integer page, Integer size, String keyword, Long userId) {
        PageResult<Role> pageResult = roleRepository.pageAllPrivateRoles(page, size, keyword, userId);
        List<RoleDTO> list = RoleAssembler.toDTOList(pageResult.getRecords());
        return new PageResult<>(list, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    // ======================= 用户端 - 私人角色管理 =======================

    /**
     * 用户创建私人角色
     */
    @Transactional
    public RoleDTO createPrivateRole(Long currentUserId, RoleCreateRequest request) {
        if (currentUserId == null) {
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录");
        }
        validateCreateRequest(request);
        Role role = RoleAssembler.fromCreateRequest(request);
        role.makePrivate(currentUserId);
        
        // 如果有开场白文本，生成对应的音频
        generateGreetingAudio(role);
        
        Role saved = roleRepository.save(role);
        log.info("用户 {} 创建私人角色成功: {} - {}", currentUserId, saved.getId(), saved.getName());
        return RoleAssembler.toDTO(saved);
    }

    /**
     * 用户编辑自己的私人角色
     */
    @Transactional
    public void updatePrivateRole(Long currentUserId, Long roleId, RoleUpdateRequest request) {
        Role role = ensureOwnedPrivateRole(currentUserId, roleId);
        RoleAssembler.copyToRole(request, role);
        roleRepository.update(role);
        log.info("用户 {} 更新私人角色成功: {}", currentUserId, roleId);
    }

    /**
     * 用户删除自己的私人角色（逻辑删除）
     */
    @Transactional
    public void deletePrivateRole(Long currentUserId, Long roleId) {
        ensureOwnedPrivateRole(currentUserId, roleId);
        roleRepository.deleteById(roleId);
        log.info("用户 {} 删除私人角色成功: {}", currentUserId, roleId);
    }

    /**
     * 用户分页查看自己的私人角色
     */
    public PageResult<RoleDTO> pageMyPrivateRoles(Long currentUserId, Integer page, Integer size, String keyword) {
        PageResult<Role> pageResult = roleRepository.pageUserPrivateRoles(page, size, keyword, currentUserId);
        List<RoleDTO> list = RoleAssembler.toDTOList(pageResult.getRecords());
        return new PageResult<>(list, pageResult.getTotal(), pageResult.getCurrent(), pageResult.getSize());
    }

    // ======================= 内部方法 =======================

    private void validateCreateRequest(RoleCreateRequest request) {
        // 目前依赖于注解校验，预留扩展点
    }

    /**
     * 生成开场白音频
     * 如果角色有开场白文本且没有音频URL，则调用TTS服务生成音频
     */
    private void generateGreetingAudio(Role role) {
        // 如果没有开场白文本，跳过
        if (role.getGreetingMessage() == null || role.getGreetingMessage().trim().isEmpty()) {
            log.debug("角色 {} 没有开场白文本，跳过TTS生成", role.getName());
            return;
        }
        
        // 如果已经有音频URL，跳过
        if (role.getGreetingAudioUrl() != null && !role.getGreetingAudioUrl().trim().isEmpty()) {
            log.debug("角色 {} 已有开场白音频URL，跳过TTS生成", role.getName());
            return;
        }
        
        try {
            // 构建TTS请求
            TTSRequestDTO ttsRequest = new TTSRequestDTO();
            ttsRequest.setText(role.getGreetingMessage());
            ttsRequest.setVoiceType(role.getVoiceType());
            ttsRequest.setEncoding("mp3");
            ttsRequest.setSpeedRatio(1.0);
            
            // 调用TTS服务
            TTSResponseDTO ttsResponse = ttsService.textToSpeech(ttsRequest);
            
            // 设置生成的音频URL
            if (ttsResponse != null && ttsResponse.getAudioData() != null) {
                role.setGreetingAudioUrl(ttsResponse.getAudioData());
                log.info("角色 {} 开场白TTS生成成功，音频URL: {}", role.getName(), ttsResponse.getAudioData());
            } else {
                log.warn("角色 {} 开场白TTS生成失败，返回结果为空", role.getName());
            }
            
        } catch (Exception e) {
            // TTS失败不影响角色创建，只记录日志
            log.warn("角色 {} 开场白TTS生成失败: {}", role.getName(), e.getMessage(), e);
        }
    }
    
    /**
     * 确保是当前用户拥有的私人角色
     */
    private Role ensureOwnedPrivateRole(Long currentUserId, Long roleId) {
        if (currentUserId == null) {
            throw BizException.of(ErrorCodeEnum.UNAUTHORIZED, "未登录");
        }
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        if (roleOpt.isEmpty()) {
            throw BizException.of(ErrorCodeEnum.ROLE_NOT_FOUND, "角色不存在");
        }
        Role role = roleOpt.get();
        if (Boolean.TRUE.equals(role.getIsPublic())) {
            throw BizException.of(ErrorCodeEnum.PERMISSION_DENIED, "不能操作公共角色");
        }
        if (!role.ownedBy(currentUserId)) {
            throw BizException.of(ErrorCodeEnum.PERMISSION_DENIED, "无权操作他人角色");
        }
        return role;
    }
}

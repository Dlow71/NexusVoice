package com.nexusvoice.application.role.assembler;

import com.nexusvoice.application.role.dto.RoleDTO;
import com.nexusvoice.application.role.dto.RoleCreateRequest;
import com.nexusvoice.application.role.dto.RoleUpdateRequest;
import com.nexusvoice.domain.role.model.Role;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色对象转换器
 */
public class RoleAssembler {

    public static RoleDTO toDTO(Role role) {
        if (role == null) {
            return null;
        }
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setPersonaPrompt(role.getPersonaPrompt());
        dto.setGreetingMessage(role.getGreetingMessage());
        dto.setGreetingAudioUrl(role.getGreetingAudioUrl());
        dto.setAvatarUrl(role.getAvatarUrl());
        dto.setVoiceType(role.getVoiceType());
        dto.setIsPublic(role.getIsPublic());
        dto.setUserId(role.getUserId());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());
        return dto;
    }

    public static List<RoleDTO> toDTOList(List<Role> roles) {
        return roles == null ? List.of() : roles.stream().map(RoleAssembler::toDTO).collect(Collectors.toList());
    }

    public static Role fromCreateRequest(RoleCreateRequest req) {
        Role role = new Role();
        role.setName(req.getName());
        role.setDescription(req.getDescription());
        role.setPersonaPrompt(req.getPersonaPrompt());
        role.setGreetingMessage(req.getGreetingMessage());
        role.setGreetingAudioUrl(req.getGreetingAudioUrl());
        role.setAvatarUrl(req.getAvatarUrl());
        role.setVoiceType(req.getVoiceType());
        return role;
    }

    public static void copyToRole(RoleUpdateRequest req, Role role) {
        if (req.getName() != null) {
            role.setName(req.getName());
        }
        if (req.getDescription() != null) {
            role.setDescription(req.getDescription());
        }
        if (req.getPersonaPrompt() != null) {
            role.setPersonaPrompt(req.getPersonaPrompt());
        }
        if (req.getGreetingMessage() != null) {
            role.setGreetingMessage(req.getGreetingMessage());
        }
        if (req.getGreetingAudioUrl() != null) {
            role.setGreetingAudioUrl(req.getGreetingAudioUrl());
        }
        if (req.getAvatarUrl() != null) {
            role.setAvatarUrl(req.getAvatarUrl());
        }
        if (req.getVoiceType() != null) {
            role.setVoiceType(req.getVoiceType());
        }
    }
}

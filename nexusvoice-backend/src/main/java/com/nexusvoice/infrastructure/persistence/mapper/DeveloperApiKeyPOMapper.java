package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.DeveloperApiKeyPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * DeveloperApiKey持久化Mapper
 * 
 * @author NexusVoice
 * @since 2025-10-29
 */
@Mapper
public interface DeveloperApiKeyPOMapper extends BaseMapper<DeveloperApiKeyPO> {
}

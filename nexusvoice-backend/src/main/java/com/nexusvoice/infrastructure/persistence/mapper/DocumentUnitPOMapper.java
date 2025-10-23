package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.DocumentUnitPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * DocumentUnit持久化Mapper
 *
 * @author NexusVoice
 * @since 2025-10-23
 */
@Mapper
public interface DocumentUnitPOMapper extends BaseMapper<DocumentUnitPO> {
}

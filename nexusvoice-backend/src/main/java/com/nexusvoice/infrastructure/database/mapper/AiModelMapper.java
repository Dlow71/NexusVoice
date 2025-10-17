package com.nexusvoice.infrastructure.database.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.domain.ai.model.AiModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI模型Mapper接口
 *
 * @author NexusVoice
 * @since 2025-10-16
 */
@Mapper
public interface AiModelMapper extends BaseMapper<AiModel> {
}

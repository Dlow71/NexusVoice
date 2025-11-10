package com.nexusvoice.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.infrastructure.persistence.po.AiProviderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI服务提供商Mapper接口
 * 使用MyBatis-Plus提供基础CRUD操作
 *
 * @author NexusVoice
 * @since 2025-01-11
 */
@Mapper
public interface AiProviderMapper extends BaseMapper<AiProviderPO> {
    
    // MyBatis-Plus的BaseMapper已提供基础CRUD方法
    // 如需自定义SQL，可在此添加方法并创建对应的XML文件
}

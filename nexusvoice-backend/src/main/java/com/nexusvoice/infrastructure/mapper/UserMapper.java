package com.nexusvoice.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nexusvoice.domain.user.model.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问接口
 * 
 * @author NexusVoice
 * @since 2025-09-23
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    
}

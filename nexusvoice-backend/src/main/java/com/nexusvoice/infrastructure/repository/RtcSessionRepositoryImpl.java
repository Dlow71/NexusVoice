package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.rtc.enums.RtcSessionState;
import com.nexusvoice.domain.rtc.model.RtcSession;
import com.nexusvoice.domain.rtc.repository.RtcSessionRepository;
import com.nexusvoice.infrastructure.persistence.converter.RtcSessionPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.RtcSessionPOMapper;
import com.nexusvoice.infrastructure.persistence.po.RtcSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RTC会话仓储实现
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Repository
@RequiredArgsConstructor
public class RtcSessionRepositoryImpl implements RtcSessionRepository {
    
    private final RtcSessionPOMapper mapper;
    private final RtcSessionPOConverter converter;
    
    @Override
    public RtcSession save(RtcSession session) {
        RtcSessionPO po = converter.toPO(session);
        
        if (session.getId() == null) {
            mapper.insert(po);
            session.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        
        return session;
    }
    
    @Override
    public Optional<RtcSession> findById(Long id) {
        RtcSessionPO po = mapper.selectById(id);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public Optional<RtcSession> findBySessionId(String sessionId) {
        LambdaQueryWrapper<RtcSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RtcSessionPO::getSessionId, sessionId)
               .eq(RtcSessionPO::getDeleted, 0);
        
        RtcSessionPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(converter.toDomain(po));
    }
    
    @Override
    public List<RtcSession> findActiveSessionsByUserId(Long userId) {
        LambdaQueryWrapper<RtcSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RtcSessionPO::getUserId, userId)
               .notIn(RtcSessionPO::getState, RtcSessionState.TERMINATED.name(), RtcSessionState.ERROR.name())
               .eq(RtcSessionPO::getDeleted, 0)
               .orderByDesc(RtcSessionPO::getCreatedAt);
        
        List<RtcSessionPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public int countActiveSessionsByUserId(Long userId) {
        LambdaQueryWrapper<RtcSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RtcSessionPO::getUserId, userId)
               .notIn(RtcSessionPO::getState, RtcSessionState.TERMINATED.name(), RtcSessionState.ERROR.name())
               .eq(RtcSessionPO::getDeleted, 0);
        
        return mapper.selectCount(wrapper).intValue();
    }
    
    @Override
    public List<RtcSession> findByState(RtcSessionState state) {
        LambdaQueryWrapper<RtcSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RtcSessionPO::getState, state.name())
               .eq(RtcSessionPO::getDeleted, 0)
               .orderByDesc(RtcSessionPO::getCreatedAt);
        
        List<RtcSessionPO> poList = mapper.selectList(wrapper);
        return poList.stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(Long id) {
        RtcSessionPO po = new RtcSessionPO();
        po.setId(id);
        po.setDeleted(1);
        mapper.updateById(po);
    }
}


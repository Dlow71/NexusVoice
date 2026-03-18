package com.nexusvoice.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusvoice.domain.voice.model.VoiceSession;
import com.nexusvoice.domain.voice.repository.VoiceSessionRepository;
import com.nexusvoice.infrastructure.persistence.converter.VoiceSessionPOConverter;
import com.nexusvoice.infrastructure.persistence.mapper.VoiceSessionPOMapper;
import com.nexusvoice.infrastructure.persistence.po.VoiceSessionPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 语音会话仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class VoiceSessionRepositoryImpl implements VoiceSessionRepository {

    private final VoiceSessionPOMapper mapper;
    private final VoiceSessionPOConverter converter;

    @Override
    public VoiceSession save(VoiceSession voiceSession) {
        VoiceSessionPO po = converter.toPO(voiceSession);
        if (voiceSession.getId() == null) {
            mapper.insert(po);
            voiceSession.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return voiceSession;
    }

    @Override
    public Optional<VoiceSession> findByVoiceSessionId(String voiceSessionId) {
        LambdaQueryWrapper<VoiceSessionPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoiceSessionPO::getVoiceSessionId, voiceSessionId)
                .eq(VoiceSessionPO::getDeleted, 0);
        return Optional.ofNullable(converter.toDomain(mapper.selectOne(wrapper)));
    }
}

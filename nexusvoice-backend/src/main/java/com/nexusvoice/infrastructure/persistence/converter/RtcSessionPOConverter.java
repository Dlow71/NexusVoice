package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.rtc.enums.RtcSessionState;
import com.nexusvoice.domain.rtc.model.RtcSession;
import com.nexusvoice.infrastructure.persistence.po.RtcSessionPO;
import org.springframework.stereotype.Component;

/**
 * RTC会话转换器
 * 
 * @author NexusVoice Team
 * @since 2025-11-01
 */
@Component
public class RtcSessionPOConverter {
    
    /**
     * Domain → PO
     */
    public RtcSessionPO toPO(RtcSession domain) {
        if (domain == null) {
            return null;
        }
        
        RtcSessionPO po = new RtcSessionPO();
        po.setId(domain.getId());
        po.setSessionId(domain.getSessionId());
        po.setUserId(domain.getUserId());
        po.setRoleId(domain.getRoleId());
        po.setConversationId(domain.getConversationId());
        po.setState(domain.getState() != null ? domain.getState().name() : null);
        po.setLocalSdp(domain.getLocalSdp());
        po.setRemoteSdp(domain.getRemoteSdp());
        po.setKmsPipelineId(domain.getKmsPipelineId());
        po.setKmsWebrtcEndpointId(domain.getKmsWebrtcEndpointId());
        po.setGrpcAsrSessionId(domain.getGrpcAsrSessionId());
        po.setGrpcTtsSessionId(domain.getGrpcTtsSessionId());
        po.setCurrentAsrText(domain.getCurrentAsrText());
        po.setCurrentTtsSegmentId(domain.getCurrentTtsSegmentId());
        po.setInterruptCount(domain.getInterruptCount());
        po.setLastErrorCode(domain.getLastErrorCode());
        po.setLastErrorMessage(domain.getLastErrorMessage());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setStartedAt(domain.getStartedAt());
        po.setEndedAt(domain.getEndedAt());
        po.setDeleted(domain.getDeleted() != null && domain.getDeleted() ? 1 : 0);
        
        return po;
    }
    
    /**
     * PO → Domain
     */
    public RtcSession toDomain(RtcSessionPO po) {
        if (po == null) {
            return null;
        }
        
        RtcSession domain = new RtcSession();
        domain.setId(po.getId());
        domain.setSessionId(po.getSessionId());
        domain.setUserId(po.getUserId());
        domain.setRoleId(po.getRoleId());
        domain.setConversationId(po.getConversationId());
        domain.setState(RtcSessionState.fromCode(po.getState()));
        domain.setLocalSdp(po.getLocalSdp());
        domain.setRemoteSdp(po.getRemoteSdp());
        domain.setKmsPipelineId(po.getKmsPipelineId());
        domain.setKmsWebrtcEndpointId(po.getKmsWebrtcEndpointId());
        domain.setGrpcAsrSessionId(po.getGrpcAsrSessionId());
        domain.setGrpcTtsSessionId(po.getGrpcTtsSessionId());
        domain.setCurrentAsrText(po.getCurrentAsrText());
        domain.setCurrentTtsSegmentId(po.getCurrentTtsSegmentId());
        domain.setInterruptCount(po.getInterruptCount());
        domain.setLastErrorCode(po.getLastErrorCode());
        domain.setLastErrorMessage(po.getLastErrorMessage());
        domain.setCreatedAt(po.getCreatedAt());
        domain.setUpdatedAt(po.getUpdatedAt());
        domain.setStartedAt(po.getStartedAt());
        domain.setEndedAt(po.getEndedAt());
        domain.setDeleted(po.getDeleted() != null && po.getDeleted() == 1);
        
        return domain;
    }
}


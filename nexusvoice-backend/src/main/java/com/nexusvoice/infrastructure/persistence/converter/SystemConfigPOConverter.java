package com.nexusvoice.infrastructure.persistence.converter;

import com.nexusvoice.domain.config.model.SystemConfig;
import com.nexusvoice.infrastructure.persistence.po.SystemConfigPO;
import org.springframework.stereotype.Component;

/**
 * SystemConfig领域对象与PO转换器
 *
 * @author NexusVoice
 * @since 2025-01-21
 */
@Component
public class SystemConfigPOConverter {

    /**
     * Domain转PO
     */
    public SystemConfigPO toPO(SystemConfig domain) {
        if (domain == null) {
            return null;
        }

        SystemConfigPO po = new SystemConfigPO();
        po.setId(domain.getId());
        po.setConfigKey(domain.getConfigKey());
        po.setConfigValue(domain.getConfigValue());
        po.setDescription(domain.getDescription());
        po.setConfigGroup(domain.getConfigGroup());
        po.setEnabled(convertBooleanToInt(domain.getEnabled()));
        po.setReadonly(convertBooleanToInt(domain.getReadonly()));
        po.setSortOrder(domain.getSortOrder());
        po.setRemark(domain.getRemark());
        po.setVersion(domain.getVersion());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedAt(domain.getUpdatedAt());
        po.setDeleted(domain.getDeleted());
        return po;
    }

    /**
     * PO转Domain
     */
    public SystemConfig toDomain(SystemConfigPO po) {
        if (po == null) {
            return null;
        }

        SystemConfig domain = new SystemConfig();
        domain.setId(po.getId());
        domain.setConfigKey(po.getConfigKey());
        domain.setConfigValue(po.getConfigValue());
        domain.setDescription(po.getDescription());
        domain.setConfigGroup(po.getConfigGroup());
        domain.setEnabled(convertIntToBoolean(po.getEnabled()));
        domain.setReadonly(convertIntToBoolean(po.getReadonly()));
        domain.setSortOrder(po.getSortOrder());
        domain.setRemark(po.getRemark());
        domain.setVersion(po.getVersion());
        domain.setCreatedAt(po.getCreatedAt());
        domain.setUpdatedAt(po.getUpdatedAt());
        domain.setDeleted(po.getDeleted());
        return domain;
    }

    /**
     * Boolean转Integer（0/1）
     */
    private Integer convertBooleanToInt(Boolean value) {
        if (value == null) {
            return 0;
        }
        return value ? 1 : 0;
    }

    /**
     * Integer转Boolean
     */
    private Boolean convertIntToBoolean(Integer value) {
        if (value == null) {
            return false;
        }
        return value == 1;
    }
}

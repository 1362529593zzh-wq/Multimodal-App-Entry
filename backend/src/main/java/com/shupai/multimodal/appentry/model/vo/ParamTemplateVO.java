package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record ParamTemplateVO(
        Long id,
        String templateCode,
        String templateName,
        String functionCode,
        String serviceCode,
        String templateType,
        String templatePayload,
        String presetParams,
        String description,
        Integer sortOrder,
        Boolean enabled,
        Boolean isDefault,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

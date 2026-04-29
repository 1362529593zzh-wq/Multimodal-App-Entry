package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record ModelServiceVO(
        Long id,
        String serviceCode,
        String serviceName,
        String modelCode,
        String modelName,
        String modelType,
        String functionCode,
        String endpoint,
        String authType,
        Integer timeoutMs,
        Boolean enabled,
        String publishStatus,
        Boolean isDefault,
        Boolean allowFrontSelect,
        String supportedOptions,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

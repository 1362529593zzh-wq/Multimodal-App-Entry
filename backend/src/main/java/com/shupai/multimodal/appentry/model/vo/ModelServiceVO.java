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
        String vendorCode,
        String vendorName,
        String vendorType,
        String providerType,
        String endpoint,
        String authType,
        String secretRef,
        String apiKeyMasked,
        String secretKeyMasked,
        String requestMethod,
        String headerTemplate,
        String payloadTemplate,
        String extraConfig,
        Integer timeoutMs,
        Boolean enabled,
        String publishStatus,
        Boolean isDefault,
        Boolean allowFrontSelect,
        String supportedOptions,
        Integer sortOrder,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

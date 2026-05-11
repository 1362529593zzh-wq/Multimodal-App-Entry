package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModelServiceCreateRequest(
        @NotBlank @Size(max = 64) String serviceCode,
        @NotBlank @Size(max = 128) String serviceName,
        @NotBlank @Size(max = 128) String modelCode,
        @NotBlank @Size(max = 128) String modelName,
        @NotBlank @Size(max = 64) String modelType,
        @NotBlank @Size(max = 64) String functionCode,
        @Size(max = 64) String vendorCode,
        @Size(max = 128) String vendorName,
        @Size(max = 32) String vendorType,
        @Size(max = 64) String providerType,
        @Size(max = 512) String endpoint,
        @Size(max = 32) String authType,
        @Size(max = 128) String secretRef,
        @Size(max = 256) String apiKey,
        @Size(max = 256) String secretKey,
        @Size(max = 16) String requestMethod,
        String headerTemplate,
        String payloadTemplate,
        String extraConfig,
        Integer timeoutMs,
        Boolean enabled,
        @Size(max = 32) String publishStatus,
        Boolean isDefault,
        Boolean allowFrontSelect,
        String supportedOptions,
        Integer sortOrder,
        @Size(max = 255) String remark
) {
}

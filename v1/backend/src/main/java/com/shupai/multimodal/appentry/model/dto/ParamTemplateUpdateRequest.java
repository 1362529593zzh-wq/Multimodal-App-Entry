package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParamTemplateUpdateRequest(
        @NotBlank @Size(max = 128) String templateName,
        @NotBlank @Size(max = 64) String functionCode,
        @Size(max = 64) String serviceCode,
        @Size(max = 32) String templateType,
        @NotBlank String templatePayload,
        @NotBlank String presetParams,
        @Size(max = 255) String description,
        Integer sortOrder,
        Boolean enabled,
        Boolean isDefault
) {
}

package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FunctionModelBindingCreateRequest(
        @NotBlank @Size(max = 64) String functionCode,
        @NotBlank @Size(max = 64) String serviceCode,
        Boolean isDefault,
        Integer sortOrder,
        Boolean enabled,
        @Size(max = 255) String remark
) {
}

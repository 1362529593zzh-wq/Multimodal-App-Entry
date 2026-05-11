package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FunctionConfigCreateRequest(
        @NotBlank @Size(max = 64) String functionCode,
        @NotBlank @Size(max = 64) String functionName,
        @Size(max = 128) String icon,
        Integer sortOrder,
        Boolean enabled,
        Boolean isDefault,
        @Size(max = 64) String defaultServiceCode,
        Boolean allowManualModelSelect,
        Boolean showInMainBar,
        Boolean showInMoreMenu,
        @Size(max = 255) String description
) {
}

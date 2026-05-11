package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.Size;

public record FunctionModelBindingUpdateRequest(
        Boolean isDefault,
        Integer sortOrder,
        Boolean enabled,
        @Size(max = 255) String remark
) {
}

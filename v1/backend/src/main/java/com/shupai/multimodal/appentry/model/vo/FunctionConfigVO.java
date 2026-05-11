package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record FunctionConfigVO(
        Long id,
        String functionCode,
        String functionName,
        String icon,
        Integer sortOrder,
        Boolean enabled,
        Boolean isDefault,
        String defaultServiceCode,
        Boolean allowManualModelSelect,
        Boolean showInMainBar,
        Boolean showInMoreMenu,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

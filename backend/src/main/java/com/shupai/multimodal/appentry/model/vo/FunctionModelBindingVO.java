package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record FunctionModelBindingVO(
        Long id,
        String functionCode,
        String serviceCode,
        Boolean isDefault,
        Integer sortOrder,
        Boolean enabled,
        String remark,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

package com.shupai.multimodal.appentry.model.dto;

import java.time.OffsetDateTime;

public record CallRecordQuery(
        Long pageNum,
        Long pageSize,
        String keyword,
        String functionCode,
        String serviceCode,
        String status,
        String resultType,
        OffsetDateTime startedFrom,
        OffsetDateTime startedTo
) {
}

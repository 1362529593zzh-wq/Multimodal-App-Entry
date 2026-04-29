package com.shupai.multimodal.appentry.model.dto;

public record ModelServiceQuery(
        Long pageNum,
        Long pageSize,
        String keyword,
        String functionCode,
        Boolean enabled
) {
}

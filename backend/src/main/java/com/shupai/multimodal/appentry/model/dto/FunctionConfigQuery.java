package com.shupai.multimodal.appentry.model.dto;

public record FunctionConfigQuery(
        Long pageNum,
        Long pageSize,
        String keyword,
        Boolean enabled
) {
}

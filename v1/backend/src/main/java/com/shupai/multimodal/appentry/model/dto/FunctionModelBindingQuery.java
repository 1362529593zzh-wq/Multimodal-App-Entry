package com.shupai.multimodal.appentry.model.dto;

public record FunctionModelBindingQuery(
        Long pageNum,
        Long pageSize,
        String functionCode,
        String serviceCode,
        Boolean enabled
) {
}

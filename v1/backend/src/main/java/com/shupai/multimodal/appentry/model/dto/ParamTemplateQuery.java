package com.shupai.multimodal.appentry.model.dto;

public record ParamTemplateQuery(
        Long pageNum,
        Long pageSize,
        String functionCode,
        String serviceCode,
        String templateType,
        Boolean enabled
) {
}

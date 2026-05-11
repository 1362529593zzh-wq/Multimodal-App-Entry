package com.shupai.multimodal.appentry.model.dto;

public record ModelServiceQuery(
        Long pageNum,
        Long pageSize,
        String keyword,
        String functionCode,
        String vendorCode,
        String providerType,
        String modelType,
        Boolean enabled
) {
}

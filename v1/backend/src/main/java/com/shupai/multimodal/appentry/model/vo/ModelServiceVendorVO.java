package com.shupai.multimodal.appentry.model.vo;

import lombok.Builder;

@Builder
public record ModelServiceVendorVO(
        String vendorCode,
        String vendorName,
        String vendorType,
        Long total,
        Long enabledCount,
        Integer sortOrder
) {
}

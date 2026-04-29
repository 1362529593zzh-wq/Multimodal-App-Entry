package com.shupai.multimodal.appentry.model.vo;

import lombok.Builder;

@Builder
public record WorkbenchFunctionVO(
        Long id,
        String functionCode,
        String functionName,
        String icon,
        Integer sortOrder,
        Boolean allowManualModelSelect,
        Boolean showInMainBar,
        Boolean showInMoreMenu,
        String defaultServiceCode,
        String defaultServiceName,
        String description
) {
}

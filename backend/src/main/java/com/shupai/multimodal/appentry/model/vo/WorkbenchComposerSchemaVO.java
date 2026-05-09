package com.shupai.multimodal.appentry.model.vo;

import java.util.List;
import lombok.Builder;

@Builder
public record WorkbenchComposerSchemaVO(
        String capabilityCode,
        String placeholder,
        String helper,
        Boolean supportsUpload,
        List<WorkbenchComposerFieldVO> fields
) {
}

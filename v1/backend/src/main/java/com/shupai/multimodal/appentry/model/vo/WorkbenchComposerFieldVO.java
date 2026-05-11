package com.shupai.multimodal.appentry.model.vo;

import java.util.List;
import lombok.Builder;

@Builder
public record WorkbenchComposerFieldVO(
        String key,
        String label,
        String placeholder,
        List<WorkbenchComposerOptionVO> options
) {
}

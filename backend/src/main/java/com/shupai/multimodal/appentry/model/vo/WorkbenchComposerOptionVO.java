package com.shupai.multimodal.appentry.model.vo;

import lombok.Builder;

@Builder
public record WorkbenchComposerOptionVO(
        String label,
        String value
) {
}

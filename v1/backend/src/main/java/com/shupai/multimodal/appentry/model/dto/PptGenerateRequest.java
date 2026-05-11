package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record PptGenerateRequest(
        @NotEmpty
        Map<String, Object> pptDraft
) {
}

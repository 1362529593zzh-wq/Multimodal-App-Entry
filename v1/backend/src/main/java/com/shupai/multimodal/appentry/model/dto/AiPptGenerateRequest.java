package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record AiPptGenerateRequest(
        @NotBlank String prompt,
        Map<String, Object> options
) {
}

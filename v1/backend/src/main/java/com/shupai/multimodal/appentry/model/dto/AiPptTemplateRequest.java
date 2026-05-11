package com.shupai.multimodal.appentry.model.dto;

import com.shupai.multimodal.appentry.model.ppt.AiPptDeckPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiPptTemplateRequest(
        @NotNull AiPptDeckPlan deckPlan,
        @NotBlank String templateCode
) {
}

package com.shupai.multimodal.appentry.model.dto;

import com.shupai.multimodal.appentry.model.ppt.AiPptDeckPlan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiPptRewriteRequest(
        @NotNull AiPptDeckPlan deckPlan,
        @NotBlank String instruction
) {
}

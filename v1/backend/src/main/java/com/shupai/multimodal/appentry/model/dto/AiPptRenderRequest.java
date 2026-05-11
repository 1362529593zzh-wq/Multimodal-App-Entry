package com.shupai.multimodal.appentry.model.dto;

import com.shupai.multimodal.appentry.model.ppt.AiPptDeckPlan;
import jakarta.validation.constraints.NotNull;

public record AiPptRenderRequest(
        @NotNull AiPptDeckPlan deckPlan
) {
}

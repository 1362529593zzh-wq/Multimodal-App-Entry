package com.shupai.multimodal.appentry.model.ppt;

public record AiPptGenerationResult(
        AiPptDeckPlan deckPlan,
        String rawResponse
) {
}

package com.shupai.multimodal.appentry.model.ppt;

import java.util.List;

public record AiPptDeckPlan(
        DeckMeta deck,
        AiPptTheme theme,
        List<String> storyline,
        List<AiPptSlidePlan> slides
) {
}

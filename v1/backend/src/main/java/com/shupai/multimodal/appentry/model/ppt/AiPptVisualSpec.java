package com.shupai.multimodal.appentry.model.ppt;

import java.util.List;

public record AiPptVisualSpec(
        List<String> icons,
        String imagePrompt,
        String imageFileId,
        AiPptChartSpec chartSpec,
        AiPptBackground background
) {
}

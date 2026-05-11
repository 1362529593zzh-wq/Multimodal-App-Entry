package com.shupai.multimodal.appentry.model.ppt;

import java.util.List;

public record AiPptChartSpec(
        String type,
        String title,
        List<String> categories,
        List<AiPptChartSeries> series,
        String unit
) {
}

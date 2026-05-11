package com.shupai.multimodal.appentry.model.ppt;

import java.util.List;

public record AiPptChartSeries(
        String name,
        List<Double> values
) {
}

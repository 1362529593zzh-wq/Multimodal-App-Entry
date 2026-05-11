package com.shupai.multimodal.appentry.model.ppt;

import java.util.List;

public record AiPptContentBlock(
        String type,
        String title,
        String text,
        String value,
        String unit,
        List<String> items
) {
}

package com.shupai.multimodal.appentry.model.ppt;

import java.util.List;

public record AiPptSlidePlan(
        String id,
        Integer page,
        String role,
        String type,
        String title,
        String subtitle,
        String takeaway,
        List<AiPptContentBlock> content,
        AiPptLayoutSpec layout,
        AiPptVisualSpec visual,
        String speakerNotes
) {
}

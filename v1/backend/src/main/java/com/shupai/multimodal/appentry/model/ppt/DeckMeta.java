package com.shupai.multimodal.appentry.model.ppt;

public record DeckMeta(
        String title,
        String audience,
        String scenario,
        String goal,
        String language,
        String tone,
        Integer pageCount,
        String designDirection,
        String serviceCode,
        String modelName
) {
}

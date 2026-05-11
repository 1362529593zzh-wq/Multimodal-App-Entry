package com.shupai.multimodal.appentry.service.ppt;

import com.shupai.multimodal.appentry.model.ppt.AiPptDeckPlan;

public interface PptRenderService {
    GeneratedPptx render(AiPptDeckPlan deckPlan);
}

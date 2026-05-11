package com.shupai.multimodal.appentry.service.ppt;

import com.shupai.multimodal.appentry.model.ppt.AiPptDeckPlan;
import com.shupai.multimodal.appentry.model.ppt.AiPptTheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PptTemplateService {

    public AiPptDeckPlan applyTemplate(AiPptDeckPlan deckPlan, String templateCode) {
        AiPptTheme theme = switch (templateCode) {
            case "tech-dark" -> new AiPptTheme("#081020", "#1C385E", "#40DEFF", "#EDF4FC", "#121C2C", "Microsoft YaHei", "tech-dark");
            case "finance-green" -> new AiPptTheme("#0E4334", "#226E54", "#CDA649", "#F6FAF7", "#1C302A", "Microsoft YaHei", "finance-green");
            case "government-red" -> new AiPptTheme("#751C24", "#96373F", "#DBB25C", "#FCF8F2", "#412D2D", "Microsoft YaHei", "government-red");
            case "startup-gradient" -> new AiPptTheme("#1E293B", "#F472B6", "#38BDF8", "#F8FAFC", "#1E293B", "Microsoft YaHei", "startup-gradient");
            case "business-blue", "business" -> new AiPptTheme("#12355B", "#5D7491", "#E07A5F", "#F8FAFC", "#1F2937", "Microsoft YaHei", "business-blue");
            default -> new AiPptTheme("#122237", "#5D7491", "#DB7640", "#FAFBFC", "#1F2937", "Microsoft YaHei", "consulting-clean");
        };
        return new AiPptDeckPlan(deckPlan.deck(), theme, deckPlan.storyline(), deckPlan.slides());
    }
}

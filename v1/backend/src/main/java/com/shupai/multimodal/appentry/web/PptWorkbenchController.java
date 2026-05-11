package com.shupai.multimodal.appentry.web;

import com.shupai.multimodal.appentry.model.dto.AiPptRenderRequest;
import com.shupai.multimodal.appentry.model.dto.AiPptRewriteRequest;
import com.shupai.multimodal.appentry.model.dto.AiPptTemplateRequest;
import com.shupai.multimodal.appentry.service.ppt.AiPptWorkspaceService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workbench/ppt")
public class PptWorkbenchController {

    private final AiPptWorkspaceService aiPptWorkspaceService;

    @PostMapping("/render")
    public Map<String, Object> render(@Valid @RequestBody AiPptRenderRequest request) {
        return aiPptWorkspaceService.render(request.deckPlan());
    }

    @PostMapping("/slides/{slideId}/rewrite")
    public Map<String, Object> rewrite(
            @PathVariable String slideId,
            @Valid @RequestBody AiPptRewriteRequest request
    ) {
        return aiPptWorkspaceService.rewrite(request.deckPlan(), slideId, request.instruction());
    }

    @PostMapping("/optimize")
    public Map<String, Object> optimize(@Valid @RequestBody AiPptRewriteRequest request) {
        return aiPptWorkspaceService.optimize(request.deckPlan(), request.instruction());
    }

    @PostMapping("/change-template")
    public Map<String, Object> changeTemplate(@Valid @RequestBody AiPptTemplateRequest request) {
        return aiPptWorkspaceService.changeTemplate(request.deckPlan(), request.templateCode());
    }
}

package com.shupai.multimodal.appentry.web;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateQuery;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateUpdateRequest;
import com.shupai.multimodal.appentry.model.vo.ParamTemplateVO;
import com.shupai.multimodal.appentry.service.ParamTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/config/param-templates")
public class ParamTemplateController {

    private final ParamTemplateService paramTemplateService;

    @GetMapping
    public PageResponse<ParamTemplateVO> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String functionCode,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Boolean enabled
    ) {
        return paramTemplateService.page(
                new ParamTemplateQuery(pageNum, pageSize, functionCode, serviceCode, templateType, enabled)
        );
    }

    @GetMapping("/{templateCode}")
    public ParamTemplateVO detail(@PathVariable String templateCode) {
        return paramTemplateService.getByCode(templateCode);
    }

    @PostMapping
    public ParamTemplateVO create(@Valid @RequestBody ParamTemplateCreateRequest request) {
        return paramTemplateService.create(request);
    }

    @PutMapping("/{templateCode}")
    public ParamTemplateVO update(
            @PathVariable String templateCode,
            @Valid @RequestBody ParamTemplateUpdateRequest request
    ) {
        return paramTemplateService.update(templateCode, request);
    }

    @PatchMapping("/{templateCode}/enabled")
    public void updateEnabled(
            @PathVariable String templateCode,
            @RequestParam boolean enabled
    ) {
        paramTemplateService.updateEnabled(templateCode, enabled);
    }
}

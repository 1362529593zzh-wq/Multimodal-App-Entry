package com.shupai.multimodal.appentry.web;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.ModelServiceCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ModelServiceQuery;
import com.shupai.multimodal.appentry.model.dto.ModelServiceUpdateRequest;
import com.shupai.multimodal.appentry.model.vo.ModelServiceVO;
import com.shupai.multimodal.appentry.service.ModelServiceService;
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
@RequestMapping("/api/config/model-services")
public class ModelServiceController {

    private final ModelServiceService modelServiceService;

    @GetMapping
    public PageResponse<ModelServiceVO> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String functionCode,
            @RequestParam(required = false) Boolean enabled
    ) {
        return modelServiceService.page(new ModelServiceQuery(pageNum, pageSize, keyword, functionCode, enabled));
    }

    @GetMapping("/{serviceCode}")
    public ModelServiceVO detail(@PathVariable String serviceCode) {
        return modelServiceService.getByCode(serviceCode);
    }

    @PostMapping
    public ModelServiceVO create(@Valid @RequestBody ModelServiceCreateRequest request) {
        return modelServiceService.create(request);
    }

    @PutMapping("/{serviceCode}")
    public ModelServiceVO update(
            @PathVariable String serviceCode,
            @Valid @RequestBody ModelServiceUpdateRequest request
    ) {
        return modelServiceService.update(serviceCode, request);
    }

    @PatchMapping("/{serviceCode}/enabled")
    public void updateEnabled(
            @PathVariable String serviceCode,
            @RequestParam boolean enabled
    ) {
        modelServiceService.updateEnabled(serviceCode, enabled);
    }
}

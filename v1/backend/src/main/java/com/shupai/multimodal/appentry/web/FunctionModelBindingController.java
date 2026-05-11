package com.shupai.multimodal.appentry.web;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingCreateRequest;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingQuery;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingUpdateRequest;
import com.shupai.multimodal.appentry.model.vo.FunctionModelBindingVO;
import com.shupai.multimodal.appentry.service.FunctionModelBindingService;
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
@RequestMapping("/api/config/function-model-bindings")
public class FunctionModelBindingController {

    private final FunctionModelBindingService functionModelBindingService;

    @GetMapping
    public PageResponse<FunctionModelBindingVO> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String functionCode,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) Boolean enabled
    ) {
        return functionModelBindingService.page(
                new FunctionModelBindingQuery(pageNum, pageSize, functionCode, serviceCode, enabled)
        );
    }

    @GetMapping("/{functionCode}/{serviceCode}")
    public FunctionModelBindingVO detail(@PathVariable String functionCode, @PathVariable String serviceCode) {
        return functionModelBindingService.getDetail(functionCode, serviceCode);
    }

    @PostMapping
    public FunctionModelBindingVO create(@Valid @RequestBody FunctionModelBindingCreateRequest request) {
        return functionModelBindingService.create(request);
    }

    @PutMapping("/{functionCode}/{serviceCode}")
    public FunctionModelBindingVO update(
            @PathVariable String functionCode,
            @PathVariable String serviceCode,
            @Valid @RequestBody FunctionModelBindingUpdateRequest request
    ) {
        return functionModelBindingService.update(functionCode, serviceCode, request);
    }

    @PatchMapping("/{functionCode}/{serviceCode}/enabled")
    public void updateEnabled(
            @PathVariable String functionCode,
            @PathVariable String serviceCode,
            @RequestParam boolean enabled
    ) {
        functionModelBindingService.updateEnabled(functionCode, serviceCode, enabled);
    }
}

package com.shupai.multimodal.appentry.web;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigCreateRequest;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigQuery;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigUpdateRequest;
import com.shupai.multimodal.appentry.model.vo.FunctionConfigVO;
import com.shupai.multimodal.appentry.service.FunctionConfigService;
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
@RequestMapping("/api/config/functions")
public class FunctionConfigController {

    private final FunctionConfigService functionConfigService;

    @GetMapping
    public PageResponse<FunctionConfigVO> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled
    ) {
        return functionConfigService.page(new FunctionConfigQuery(pageNum, pageSize, keyword, enabled));
    }

    @GetMapping("/{functionCode}")
    public FunctionConfigVO detail(@PathVariable String functionCode) {
        return functionConfigService.getByCode(functionCode);
    }

    @PostMapping
    public FunctionConfigVO create(@Valid @RequestBody FunctionConfigCreateRequest request) {
        return functionConfigService.create(request);
    }

    @PutMapping("/{functionCode}")
    public FunctionConfigVO update(
            @PathVariable String functionCode,
            @Valid @RequestBody FunctionConfigUpdateRequest request
    ) {
        return functionConfigService.update(functionCode, request);
    }

    @PatchMapping("/{functionCode}/enabled")
    public void updateEnabled(
            @PathVariable String functionCode,
            @RequestParam boolean enabled
    ) {
        functionConfigService.updateEnabled(functionCode, enabled);
    }
}

package com.shupai.multimodal.appentry.service;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigCreateRequest;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigQuery;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigUpdateRequest;
import com.shupai.multimodal.appentry.model.vo.FunctionConfigVO;

public interface FunctionConfigService {

    PageResponse<FunctionConfigVO> page(FunctionConfigQuery query);

    FunctionConfigVO getByCode(String functionCode);

    FunctionConfigVO create(FunctionConfigCreateRequest request);

    FunctionConfigVO update(String functionCode, FunctionConfigUpdateRequest request);

    void updateEnabled(String functionCode, boolean enabled);
}

package com.shupai.multimodal.appentry.service;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingCreateRequest;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingQuery;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingUpdateRequest;
import com.shupai.multimodal.appentry.model.vo.FunctionModelBindingVO;

public interface FunctionModelBindingService {

    PageResponse<FunctionModelBindingVO> page(FunctionModelBindingQuery query);

    FunctionModelBindingVO getDetail(String functionCode, String serviceCode);

    FunctionModelBindingVO create(FunctionModelBindingCreateRequest request);

    FunctionModelBindingVO update(String functionCode, String serviceCode, FunctionModelBindingUpdateRequest request);

    void updateEnabled(String functionCode, String serviceCode, boolean enabled);
}

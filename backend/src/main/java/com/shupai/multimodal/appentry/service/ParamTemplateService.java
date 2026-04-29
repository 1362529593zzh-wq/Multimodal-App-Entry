package com.shupai.multimodal.appentry.service;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateQuery;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateUpdateRequest;
import com.shupai.multimodal.appentry.model.vo.ParamTemplateVO;

public interface ParamTemplateService {

    PageResponse<ParamTemplateVO> page(ParamTemplateQuery query);

    ParamTemplateVO getByCode(String templateCode);

    ParamTemplateVO create(ParamTemplateCreateRequest request);

    ParamTemplateVO update(String templateCode, ParamTemplateUpdateRequest request);

    void updateEnabled(String templateCode, boolean enabled);
}

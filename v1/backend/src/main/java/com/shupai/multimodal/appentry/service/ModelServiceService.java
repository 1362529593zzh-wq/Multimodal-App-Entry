package com.shupai.multimodal.appentry.service;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.ModelServiceCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ModelServiceQuery;
import com.shupai.multimodal.appentry.model.dto.ModelServiceUpdateRequest;
import com.shupai.multimodal.appentry.model.vo.ModelServiceVO;
import com.shupai.multimodal.appentry.model.vo.ModelServiceVendorVO;
import java.util.List;
import java.util.Map;

public interface ModelServiceService {

    PageResponse<ModelServiceVO> page(ModelServiceQuery query);

    ModelServiceVO getByCode(String serviceCode);

    ModelServiceVO create(ModelServiceCreateRequest request);

    ModelServiceVO update(String serviceCode, ModelServiceUpdateRequest request);

    void updateEnabled(String serviceCode, boolean enabled);

    void delete(String serviceCode);

    List<ModelServiceVendorVO> listVendors();

    Map<String, Object> testConnection(String serviceCode);
}

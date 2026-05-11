package com.shupai.multimodal.appentry.service;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.CallRecordQuery;
import com.shupai.multimodal.appentry.model.vo.CallRecordVO;

public interface CallRecordService {

    PageResponse<CallRecordVO> page(CallRecordQuery query);

    CallRecordVO getByRecordId(String recordId);

    byte[] exportCsv(CallRecordQuery query);
}

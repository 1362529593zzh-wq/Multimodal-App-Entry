package com.shupai.multimodal.appentry.web;

import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.model.dto.CallRecordQuery;
import com.shupai.multimodal.appentry.model.vo.CallRecordVO;
import com.shupai.multimodal.appentry.service.CallRecordService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-records")
public class CallRecordController {

    private final CallRecordService callRecordService;

    @GetMapping
    public PageResponse<CallRecordVO> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String functionCode,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String resultType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startedTo
    ) {
        return callRecordService.page(new CallRecordQuery(pageNum, pageSize, keyword, functionCode, serviceCode, status, resultType, startedFrom, startedTo));
    }

    @GetMapping("/{recordId}")
    public CallRecordVO detail(@PathVariable String recordId) {
        return callRecordService.getByRecordId(recordId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String functionCode,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String resultType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startedTo
    ) {
        byte[] payload = callRecordService.exportCsv(new CallRecordQuery(1L, 5000L, keyword, functionCode, serviceCode, status, resultType, startedFrom, startedTo));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("call-records.csv").build().toString())
                .body(payload);
    }
}

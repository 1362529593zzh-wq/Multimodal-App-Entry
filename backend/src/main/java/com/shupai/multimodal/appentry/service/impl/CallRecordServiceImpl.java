package com.shupai.multimodal.appentry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.mapper.CallRecordMapper;
import com.shupai.multimodal.appentry.mapper.FunctionConfigMapper;
import com.shupai.multimodal.appentry.mapper.ModelServiceMapper;
import com.shupai.multimodal.appentry.model.dto.CallRecordQuery;
import com.shupai.multimodal.appentry.model.entity.CallRecordEntity;
import com.shupai.multimodal.appentry.model.entity.FunctionConfigEntity;
import com.shupai.multimodal.appentry.model.entity.ModelServiceEntity;
import com.shupai.multimodal.appentry.model.vo.CallRecordVO;
import com.shupai.multimodal.appentry.service.CallRecordService;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CallRecordServiceImpl implements CallRecordService {

    private static final DateTimeFormatter CSV_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int EXPORT_LIMIT = 5000;

    private final CallRecordMapper callRecordMapper;
    private final FunctionConfigMapper functionConfigMapper;
    private final ModelServiceMapper modelServiceMapper;

    @Override
    public PageResponse<CallRecordVO> page(CallRecordQuery query) {
        long pageNum = query.pageNum() == null || query.pageNum() < 1 ? 1L : query.pageNum();
        long pageSize = query.pageSize() == null || query.pageSize() < 1 ? 10L : Math.min(query.pageSize(), 200L);

        IPage<CallRecordEntity> page = callRecordMapper.selectPage(new Page<>(pageNum, pageSize), buildWrapper(query));
        List<CallRecordVO> records = enrich(page.getRecords());
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public CallRecordVO getByRecordId(String recordId) {
        CallRecordEntity entity = callRecordMapper.selectOne(new LambdaQueryWrapper<CallRecordEntity>()
                .eq(CallRecordEntity::getRecordId, recordId)
                .eq(CallRecordEntity::getIsDeleted, false)
                .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "call record not found");
        }
        return enrich(List.of(entity)).get(0);
    }

    @Override
    public byte[] exportCsv(CallRecordQuery query) {
        LambdaQueryWrapper<CallRecordEntity> wrapper = buildWrapper(query);
        wrapper.last("limit " + EXPORT_LIMIT);
        List<CallRecordVO> records = enrich(callRecordMapper.selectList(wrapper));

        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff');
        csv.append("调用记录ID,任务ID,功能,模型服务,模型名称,状态,结果类型,开始时间,结束时间,调用时长(ms),错误信息,下载次数\n");
        for (CallRecordVO record : records) {
            appendCsvRow(csv, List.of(
                    safe(record.recordId()),
                    safe(record.taskId()),
                    safe(StringUtils.hasText(record.functionName()) ? record.functionName() : record.functionCode()),
                    safe(StringUtils.hasText(record.serviceName()) ? record.serviceName() : record.serviceCode()),
                    safe(record.modelName()),
                    safe(record.status()),
                    safe(record.resultType()),
                    formatTime(record.startedAt()),
                    formatTime(record.finishedAt()),
                    record.durationMs() == null ? "" : String.valueOf(record.durationMs()),
                    safe(record.errorMessage()),
                    record.downloadCount() == null ? "0" : String.valueOf(record.downloadCount())
            ));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private LambdaQueryWrapper<CallRecordEntity> buildWrapper(CallRecordQuery query) {
        LambdaQueryWrapper<CallRecordEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CallRecordEntity::getIsDeleted, false);
        wrapper.eq(StringUtils.hasText(query.functionCode()), CallRecordEntity::getFunctionCode, query.functionCode());
        wrapper.eq(StringUtils.hasText(query.serviceCode()), CallRecordEntity::getServiceCode, query.serviceCode());
        wrapper.eq(StringUtils.hasText(query.status()), CallRecordEntity::getStatus, query.status());
        wrapper.eq(StringUtils.hasText(query.resultType()), CallRecordEntity::getResultType, query.resultType());
        wrapper.ge(query.startedFrom() != null, CallRecordEntity::getStartedAt, query.startedFrom());
        wrapper.le(query.startedTo() != null, CallRecordEntity::getStartedAt, query.startedTo());
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();
            wrapper.and(w -> w.like(CallRecordEntity::getRecordId, keyword)
                    .or()
                    .like(CallRecordEntity::getTaskId, keyword)
                    .or()
                    .like(CallRecordEntity::getInputText, keyword)
                    .or()
                    .like(CallRecordEntity::getRequestSummary, keyword)
                    .or()
                    .like(CallRecordEntity::getRemark, keyword));
        }
        wrapper.orderByDesc(CallRecordEntity::getCreatedAt)
                .orderByDesc(CallRecordEntity::getId);
        return wrapper;
    }

    private List<CallRecordVO> enrich(List<CallRecordEntity> entities) {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> functionCodes = entities.stream().map(CallRecordEntity::getFunctionCode).filter(StringUtils::hasText).distinct().toList();
        List<String> serviceCodes = entities.stream().map(CallRecordEntity::getServiceCode).filter(StringUtils::hasText).distinct().toList();
        Map<String, FunctionConfigEntity> functionMap = functionCodes.isEmpty() ? Collections.emptyMap()
                : functionConfigMapper.selectList(new LambdaQueryWrapper<FunctionConfigEntity>().in(FunctionConfigEntity::getFunctionCode, functionCodes))
                .stream().collect(Collectors.toMap(FunctionConfigEntity::getFunctionCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<String, ModelServiceEntity> serviceMap = serviceCodes.isEmpty() ? Collections.emptyMap()
                : modelServiceMapper.selectList(new LambdaQueryWrapper<ModelServiceEntity>().in(ModelServiceEntity::getServiceCode, serviceCodes))
                .stream().collect(Collectors.toMap(ModelServiceEntity::getServiceCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        return entities.stream().map(entity -> toVO(entity, functionMap.get(entity.getFunctionCode()), serviceMap.get(entity.getServiceCode()))).toList();
    }

    private CallRecordVO toVO(CallRecordEntity entity, FunctionConfigEntity function, ModelServiceEntity service) {
        return CallRecordVO.builder()
                .id(entity.getId())
                .recordId(entity.getRecordId())
                .taskId(entity.getTaskId())
                .conversationId(entity.getSessionId())
                .messageId(entity.getMessageId())
                .parentTaskId(entity.getParentTaskId())
                .sourceAssetIds(entity.getSourceAssetIds())
                .functionCode(entity.getFunctionCode())
                .functionName(function == null ? null : function.getFunctionName())
                .selectionMode(entity.getSelectionMode())
                .resolvedIntent(entity.getResolvedIntent())
                .serviceCode(entity.getServiceCode())
                .serviceName(service == null ? null : service.getServiceName())
                .modelName(entity.getModelName())
                .requestSummary(entity.getRequestSummary())
                .inputText(entity.getInputText())
                .requestParams(entity.getRequestParams())
                .inputAssets(entity.getInputAssets())
                .status(entity.getStatus())
                .resultType(entity.getResultType())
                .resultSummary(entity.getResultSummary())
                .errorMessage(entity.getErrorMessage())
                .downloadCount(entity.getDownloadCount())
                .recordStatus(entity.getRecordStatus())
                .remark(entity.getRemark())
                .tags(entity.getTags())
                .createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt())
                .finishedAt(entity.getFinishedAt())
                .durationMs(entity.getDurationMs())
                .build();
    }

    private void appendCsvRow(StringBuilder csv, List<String> values) {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escapeCsv(values.get(index)));
        }
        csv.append('\n');
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value.replace("\r", " ").replace("\n", " ");
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatTime(java.time.OffsetDateTime value) {
        return value == null ? "" : value.format(CSV_TIME_FORMATTER);
    }
}

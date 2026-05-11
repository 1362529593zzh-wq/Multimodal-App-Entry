package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record CallRecordVO(
        Long id,
        String recordId,
        String taskId,
        String conversationId,
        String messageId,
        String parentTaskId,
        String sourceAssetIds,
        String functionCode,
        String functionName,
        String selectionMode,
        String resolvedIntent,
        String serviceCode,
        String serviceName,
        String modelName,
        String requestSummary,
        String inputText,
        String requestParams,
        String inputAssets,
        String status,
        String resultType,
        String resultSummary,
        String errorMessage,
        Integer downloadCount,
        String recordStatus,
        String remark,
        String tags,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long durationMs
) {
}

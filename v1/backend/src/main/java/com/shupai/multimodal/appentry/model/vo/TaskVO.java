package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record TaskVO(
        Long id,
        String taskId,
        String conversationId,
        String messageId,
        String parentTaskId,
        String capabilityCode,
        String selectionMode,
        String resolvedIntent,
        String serviceCode,
        String modelName,
        String inputText,
        String requestParams,
        String inputAssetIds,
        String inheritanceMode,
        String sourceAssetIds,
        String status,
        String resultType,
        String errorMessage,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Long durationMs
) {
}

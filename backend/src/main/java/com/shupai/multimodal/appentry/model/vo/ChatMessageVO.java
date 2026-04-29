package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record ChatMessageVO(
        Long id,
        String messageId,
        String conversationId,
        String messageType,
        String contentType,
        String contentText,
        String relatedTaskId,
        String relatedRecordId,
        Integer sequenceNo,
        OffsetDateTime createdAt
) {
}

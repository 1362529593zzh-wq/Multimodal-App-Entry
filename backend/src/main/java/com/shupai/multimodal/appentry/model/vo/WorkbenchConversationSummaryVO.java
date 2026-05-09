package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record WorkbenchConversationSummaryVO(
        Long id,
        String conversationId,
        String title,
        String lastCapabilityCode,
        String lastServiceCode,
        String status,
        String latestMessagePreview,
        String latestMessageType,
        OffsetDateTime latestMessageAt,
        Integer messageCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

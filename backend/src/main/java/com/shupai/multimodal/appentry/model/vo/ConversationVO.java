package com.shupai.multimodal.appentry.model.vo;

import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record ConversationVO(
        Long id,
        String conversationId,
        String title,
        String lastCapabilityCode,
        String lastServiceCode,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

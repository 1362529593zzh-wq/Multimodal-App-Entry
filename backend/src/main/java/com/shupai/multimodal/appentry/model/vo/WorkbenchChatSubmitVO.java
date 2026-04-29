package com.shupai.multimodal.appentry.model.vo;

import lombok.Builder;

@Builder
public record WorkbenchChatSubmitVO(
        String conversationId,
        String userMessageId,
        String taskMessageId,
        String taskId,
        String resolvedCapability,
        String resolvedServiceCode,
        String resolvedModel,
        String status
) {
}

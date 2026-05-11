package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkbenchChatRequest(
        @Size(max = 64) String conversationId,
        @Size(max = 16) String selectionMode,
        @Size(max = 64) String capability,
        @NotBlank @Size(max = 4000) String inputText,
        @Size(max = 64) String serviceCode,
        @Size(max = 64) String model,
        String options,
        String composerOptions,
        @Size(max = 32) String schemaVersion,
        @Size(max = 64) String templateCode,
        String attachments,
        @Size(max = 32) String inheritanceMode,
        @Size(max = 64) String parentTaskId,
        String sourceAssetIds
) {
}

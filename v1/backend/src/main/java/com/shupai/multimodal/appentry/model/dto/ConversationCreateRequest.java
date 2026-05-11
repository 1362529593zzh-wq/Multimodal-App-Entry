package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.Size;

public record ConversationCreateRequest(
        @Size(max = 255) String title
) {
}

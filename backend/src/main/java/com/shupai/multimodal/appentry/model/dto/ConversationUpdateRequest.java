package com.shupai.multimodal.appentry.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConversationUpdateRequest(
        @NotBlank @Size(max = 255) String title
) {
}

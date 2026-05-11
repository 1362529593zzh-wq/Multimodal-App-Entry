package com.shupai.multimodal.appentry.model.vo;

import org.springframework.core.io.Resource;

public record WorkbenchFileResource(
        String fileName,
        String mimeType,
        Resource resource
) {
}

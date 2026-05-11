package com.shupai.multimodal.appentry.model.vo;

import lombok.Builder;

@Builder
public record WorkbenchUploadedFileVO(
        String fileId,
        String fileName,
        String fileType,
        String mimeType,
        Long fileSize,
        String previewUrl,
        String downloadUrl
) {
}

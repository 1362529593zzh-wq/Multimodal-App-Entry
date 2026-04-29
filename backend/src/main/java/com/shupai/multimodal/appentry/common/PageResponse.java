package com.shupai.multimodal.appentry.common;

import java.util.List;

public record PageResponse<T>(
        long pageNum,
        long pageSize,
        long total,
        List<T> records
) {
}

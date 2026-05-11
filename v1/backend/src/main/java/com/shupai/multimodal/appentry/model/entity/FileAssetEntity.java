package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shupai.multimodal.appentry.mybatis.JsonbStringTypeHandler;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName(value = "app.mm_file_asset", autoResultMap = true)
public class FileAssetEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileId;
    private String relatedType;
    private String relatedId;
    private String fileRole;
    private String fileType;
    private String fileName;
    private String storageKey;
    private Long fileSize;
    private String mimeType;
    private String previewUrl;
    private String downloadUrl;
    private String sourceTaskId;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String extraMeta;
    private OffsetDateTime createdAt;
}

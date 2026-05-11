package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shupai.multimodal.appentry.mybatis.JsonbStringTypeHandler;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName(value = "app.mm_call_record", autoResultMap = true)
public class CallRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordId;
    private String taskId;
    private String sessionId;
    private String messageId;
    private String parentTaskId;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String sourceAssetIds;
    private String functionCode;
    private String selectionMode;
    private String resolvedIntent;
    private String serviceCode;
    private String modelName;
    private String requestSummary;
    private String inputText;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String requestParams;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String inputAssets;
    private String status;
    private String resultType;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String resultSummary;
    private String errorMessage;
    private Integer downloadCount;
    private String recordStatus;
    private String remark;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String tags;
    private OffsetDateTime createdAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private Long durationMs;
    private Boolean isDeleted;
    private OffsetDateTime deletedAt;
}

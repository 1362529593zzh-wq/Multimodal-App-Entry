package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shupai.multimodal.appentry.mybatis.JsonbStringTypeHandler;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName(value = "app.mm_task", autoResultMap = true)
public class TaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String sessionId;
    private String messageId;
    private String parentTaskId;
    private String functionCode;
    private String selectionMode;
    private String resolvedIntent;
    private String serviceCode;
    private String modelName;
    private String inputText;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String requestParams;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String inputAssetIds;
    private String inheritanceMode;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String sourceAssetIds;
    private String status;
    private String resultType;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private Long durationMs;
}

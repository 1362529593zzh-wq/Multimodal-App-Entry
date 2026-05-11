package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shupai.multimodal.appentry.mybatis.JsonbStringTypeHandler;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName(value = "app.mm_task_result", autoResultMap = true)
public class TaskResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String resultId;
    private String taskId;
    private String recordId;
    private String resultType;
    private String status;
    private String textResult;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String fileIds;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String structuredResult;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String rawResponse;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String errorDetail;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

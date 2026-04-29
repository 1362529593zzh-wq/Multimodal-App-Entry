package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shupai.multimodal.appentry.mybatis.JsonbStringTypeHandler;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName(value = "app.mm_model_service", autoResultMap = true)
public class ModelServiceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String serviceCode;
    private String serviceName;
    private String modelCode;
    private String modelName;
    private String modelType;
    private String functionCode;
    private String endpoint;
    private String authType;
    private Integer timeoutMs;
    private Boolean enabled;
    private String publishStatus;
    private Boolean isDefault;
    private Boolean allowFrontSelect;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String supportedOptions;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

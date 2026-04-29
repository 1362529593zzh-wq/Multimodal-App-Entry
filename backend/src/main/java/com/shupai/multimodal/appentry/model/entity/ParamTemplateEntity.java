package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shupai.multimodal.appentry.mybatis.JsonbStringTypeHandler;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName(value = "app.mm_param_template", autoResultMap = true)
public class ParamTemplateEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateCode;
    private String templateName;
    private String functionCode;
    private String serviceCode;
    private String templateType;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String templatePayload;
    @TableField(typeHandler = JsonbStringTypeHandler.class)
    private String presetParams;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean isDefault;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

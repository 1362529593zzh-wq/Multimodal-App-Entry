package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("app.mm_function_model_binding")
public class FunctionModelBindingEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String functionCode;
    private String serviceCode;
    private Boolean isDefault;
    private Integer sortOrder;
    private Boolean enabled;
    private String remark;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

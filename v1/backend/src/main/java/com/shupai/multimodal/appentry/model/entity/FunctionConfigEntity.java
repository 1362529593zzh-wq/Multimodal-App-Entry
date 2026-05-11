package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("app.mm_function_config")
public class FunctionConfigEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String functionCode;
    private String functionName;
    private String icon;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean isDefault;
    private String defaultServiceCode;
    private Boolean allowManualModelSelect;
    private Boolean showInMainBar;
    private Boolean showInMoreMenu;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

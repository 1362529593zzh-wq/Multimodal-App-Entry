package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("app.mm_chat_session")
public class ChatSessionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String title;
    private String lastFunctionCode;
    private String lastServiceCode;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

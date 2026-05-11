package com.shupai.multimodal.appentry.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@TableName("app.mm_chat_message")
public class ChatMessageEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String messageId;
    private String sessionId;
    private String messageType;
    private String contentType;
    private String contentText;
    private String relatedTaskId;
    private String relatedRecordId;
    private Integer sequenceNo;
    private OffsetDateTime createdAt;
}

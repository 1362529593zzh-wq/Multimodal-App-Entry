package com.shupai.multimodal.appentry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shupai.multimodal.appentry.model.entity.ChatMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessageEntity> {
}

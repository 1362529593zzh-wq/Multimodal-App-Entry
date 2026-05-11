package com.shupai.multimodal.appentry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shupai.multimodal.appentry.model.entity.TaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<TaskEntity> {
}

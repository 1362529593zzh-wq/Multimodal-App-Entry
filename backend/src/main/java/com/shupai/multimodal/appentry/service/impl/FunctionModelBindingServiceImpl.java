package com.shupai.multimodal.appentry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.mapper.FunctionModelBindingMapper;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingCreateRequest;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingQuery;
import com.shupai.multimodal.appentry.model.dto.FunctionModelBindingUpdateRequest;
import com.shupai.multimodal.appentry.model.entity.FunctionModelBindingEntity;
import com.shupai.multimodal.appentry.model.vo.FunctionModelBindingVO;
import com.shupai.multimodal.appentry.service.FunctionModelBindingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FunctionModelBindingServiceImpl implements FunctionModelBindingService {

    private final FunctionModelBindingMapper functionModelBindingMapper;

    @Override
    public PageResponse<FunctionModelBindingVO> page(FunctionModelBindingQuery query) {
        long pageNum = query.pageNum() == null || query.pageNum() < 1 ? 1L : query.pageNum();
        long pageSize = query.pageSize() == null || query.pageSize() < 1 ? 10L : query.pageSize();

        LambdaQueryWrapper<FunctionModelBindingEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.enabled() != null, FunctionModelBindingEntity::getEnabled, query.enabled());
        wrapper.eq(StringUtils.hasText(query.functionCode()), FunctionModelBindingEntity::getFunctionCode, query.functionCode());
        wrapper.eq(StringUtils.hasText(query.serviceCode()), FunctionModelBindingEntity::getServiceCode, query.serviceCode());
        wrapper.orderByAsc(FunctionModelBindingEntity::getSortOrder)
                .orderByDesc(FunctionModelBindingEntity::getCreatedAt);

        IPage<FunctionModelBindingEntity> page = functionModelBindingMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<FunctionModelBindingVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public FunctionModelBindingVO getDetail(String functionCode, String serviceCode) {
        return toVO(findByPair(functionCode, serviceCode));
    }

    @Override
    @Transactional
    public FunctionModelBindingVO create(FunctionModelBindingCreateRequest request) {
        if (exists(request.functionCode(), request.serviceCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "binding already exists");
        }

        FunctionModelBindingEntity entity = new FunctionModelBindingEntity();
        entity.setFunctionCode(request.functionCode());
        entity.setServiceCode(request.serviceCode());
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        entity.setSortOrder(defaultInteger(request.sortOrder(), 0));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setRemark(request.remark());
        functionModelBindingMapper.insert(entity);
        return toVO(findByPair(request.functionCode(), request.serviceCode()));
    }

    @Override
    @Transactional
    public FunctionModelBindingVO update(String functionCode, String serviceCode, FunctionModelBindingUpdateRequest request) {
        FunctionModelBindingEntity entity = findByPair(functionCode, serviceCode);
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        entity.setSortOrder(defaultInteger(request.sortOrder(), 0));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setRemark(request.remark());
        functionModelBindingMapper.updateById(entity);
        return toVO(findByPair(functionCode, serviceCode));
    }

    @Override
    @Transactional
    public void updateEnabled(String functionCode, String serviceCode, boolean enabled) {
        FunctionModelBindingEntity entity = findByPair(functionCode, serviceCode);
        entity.setEnabled(enabled);
        functionModelBindingMapper.updateById(entity);
    }

    private boolean exists(String functionCode, String serviceCode) {
        return functionModelBindingMapper.selectCount(new LambdaQueryWrapper<FunctionModelBindingEntity>()
                .eq(FunctionModelBindingEntity::getFunctionCode, functionCode)
                .eq(FunctionModelBindingEntity::getServiceCode, serviceCode)) > 0;
    }

    private FunctionModelBindingEntity findByPair(String functionCode, String serviceCode) {
        FunctionModelBindingEntity entity = functionModelBindingMapper.selectOne(new LambdaQueryWrapper<FunctionModelBindingEntity>()
                .eq(FunctionModelBindingEntity::getFunctionCode, functionCode)
                .eq(FunctionModelBindingEntity::getServiceCode, serviceCode)
                .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "binding not found");
        }
        return entity;
    }

    private FunctionModelBindingVO toVO(FunctionModelBindingEntity entity) {
        return FunctionModelBindingVO.builder()
                .id(entity.getId())
                .functionCode(entity.getFunctionCode())
                .serviceCode(entity.getServiceCode())
                .isDefault(entity.getIsDefault())
                .sortOrder(entity.getSortOrder())
                .enabled(entity.getEnabled())
                .remark(entity.getRemark())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Integer defaultInteger(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private Boolean defaultBoolean(Boolean value, boolean defaultValue) {
        return value == null ? defaultValue : value;
    }
}

package com.shupai.multimodal.appentry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.mapper.FunctionConfigMapper;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigCreateRequest;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigQuery;
import com.shupai.multimodal.appentry.model.dto.FunctionConfigUpdateRequest;
import com.shupai.multimodal.appentry.model.entity.FunctionConfigEntity;
import com.shupai.multimodal.appentry.model.vo.FunctionConfigVO;
import com.shupai.multimodal.appentry.service.FunctionConfigService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FunctionConfigServiceImpl implements FunctionConfigService {

    private final FunctionConfigMapper functionConfigMapper;

    @Override
    public PageResponse<FunctionConfigVO> page(FunctionConfigQuery query) {
        long pageNum = query.pageNum() == null || query.pageNum() < 1 ? 1L : query.pageNum();
        long pageSize = query.pageSize() == null || query.pageSize() < 1 ? 10L : query.pageSize();

        LambdaQueryWrapper<FunctionConfigEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.enabled() != null, FunctionConfigEntity::getEnabled, query.enabled());
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(FunctionConfigEntity::getFunctionCode, query.keyword())
                    .or()
                    .like(FunctionConfigEntity::getFunctionName, query.keyword()));
        }
        wrapper.orderByAsc(FunctionConfigEntity::getSortOrder)
                .orderByDesc(FunctionConfigEntity::getCreatedAt);

        IPage<FunctionConfigEntity> page = functionConfigMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<FunctionConfigVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public FunctionConfigVO getByCode(String functionCode) {
        return toVO(findByCode(functionCode));
    }

    @Override
    @Transactional
    public FunctionConfigVO create(FunctionConfigCreateRequest request) {
        if (existsByCode(request.functionCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "functionCode already exists");
        }

        FunctionConfigEntity entity = new FunctionConfigEntity();
        entity.setFunctionCode(request.functionCode());
        entity.setFunctionName(request.functionName());
        entity.setIcon(request.icon());
        entity.setSortOrder(defaultInteger(request.sortOrder(), 0));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        entity.setDefaultServiceCode(request.defaultServiceCode());
        entity.setAllowManualModelSelect(defaultBoolean(request.allowManualModelSelect(), true));
        entity.setShowInMainBar(defaultBoolean(request.showInMainBar(), true));
        entity.setShowInMoreMenu(defaultBoolean(request.showInMoreMenu(), false));
        entity.setDescription(request.description());
        functionConfigMapper.insert(entity);
        return toVO(findByCode(request.functionCode()));
    }

    @Override
    @Transactional
    public FunctionConfigVO update(String functionCode, FunctionConfigUpdateRequest request) {
        FunctionConfigEntity entity = findByCode(functionCode);
        entity.setFunctionName(request.functionName());
        entity.setIcon(request.icon());
        entity.setSortOrder(defaultInteger(request.sortOrder(), 0));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        entity.setDefaultServiceCode(request.defaultServiceCode());
        entity.setAllowManualModelSelect(defaultBoolean(request.allowManualModelSelect(), true));
        entity.setShowInMainBar(defaultBoolean(request.showInMainBar(), true));
        entity.setShowInMoreMenu(defaultBoolean(request.showInMoreMenu(), false));
        entity.setDescription(request.description());
        functionConfigMapper.updateById(entity);
        return toVO(findByCode(functionCode));
    }

    @Override
    @Transactional
    public void updateEnabled(String functionCode, boolean enabled) {
        FunctionConfigEntity entity = findByCode(functionCode);
        entity.setEnabled(enabled);
        functionConfigMapper.updateById(entity);
    }

    private boolean existsByCode(String functionCode) {
        return functionConfigMapper.selectCount(new LambdaQueryWrapper<FunctionConfigEntity>()
                .eq(FunctionConfigEntity::getFunctionCode, functionCode)) > 0;
    }

    private FunctionConfigEntity findByCode(String functionCode) {
        FunctionConfigEntity entity = functionConfigMapper.selectOne(new LambdaQueryWrapper<FunctionConfigEntity>()
                .eq(FunctionConfigEntity::getFunctionCode, functionCode)
                .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "function config not found");
        }
        return entity;
    }

    private FunctionConfigVO toVO(FunctionConfigEntity entity) {
        return FunctionConfigVO.builder()
                .id(entity.getId())
                .functionCode(entity.getFunctionCode())
                .functionName(entity.getFunctionName())
                .icon(entity.getIcon())
                .sortOrder(entity.getSortOrder())
                .enabled(entity.getEnabled())
                .isDefault(entity.getIsDefault())
                .defaultServiceCode(entity.getDefaultServiceCode())
                .allowManualModelSelect(entity.getAllowManualModelSelect())
                .showInMainBar(entity.getShowInMainBar())
                .showInMoreMenu(entity.getShowInMoreMenu())
                .description(entity.getDescription())
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

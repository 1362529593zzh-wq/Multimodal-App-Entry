package com.shupai.multimodal.appentry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.mapper.ModelServiceMapper;
import com.shupai.multimodal.appentry.model.dto.ModelServiceCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ModelServiceQuery;
import com.shupai.multimodal.appentry.model.dto.ModelServiceUpdateRequest;
import com.shupai.multimodal.appentry.model.entity.ModelServiceEntity;
import com.shupai.multimodal.appentry.model.vo.ModelServiceVO;
import com.shupai.multimodal.appentry.service.ModelServiceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ModelServiceServiceImpl implements ModelServiceService {

    private final ModelServiceMapper modelServiceMapper;

    @Override
    public PageResponse<ModelServiceVO> page(ModelServiceQuery query) {
        long pageNum = query.pageNum() == null || query.pageNum() < 1 ? 1L : query.pageNum();
        long pageSize = query.pageSize() == null || query.pageSize() < 1 ? 10L : query.pageSize();

        LambdaQueryWrapper<ModelServiceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.enabled() != null, ModelServiceEntity::getEnabled, query.enabled());
        wrapper.eq(StringUtils.hasText(query.functionCode()), ModelServiceEntity::getFunctionCode, query.functionCode());
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(ModelServiceEntity::getServiceCode, query.keyword())
                    .or()
                    .like(ModelServiceEntity::getServiceName, query.keyword())
                    .or()
                    .like(ModelServiceEntity::getModelName, query.keyword()));
        }
        wrapper.orderByDesc(ModelServiceEntity::getCreatedAt);

        IPage<ModelServiceEntity> page = modelServiceMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ModelServiceVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public ModelServiceVO getByCode(String serviceCode) {
        return toVO(findByCode(serviceCode));
    }

    @Override
    @Transactional
    public ModelServiceVO create(ModelServiceCreateRequest request) {
        if (existsByCode(request.serviceCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "serviceCode already exists");
        }

        ModelServiceEntity entity = new ModelServiceEntity();
        entity.setServiceCode(request.serviceCode());
        entity.setServiceName(request.serviceName());
        entity.setModelCode(request.modelCode());
        entity.setModelName(request.modelName());
        entity.setModelType(request.modelType());
        entity.setFunctionCode(request.functionCode());
        entity.setEndpoint(request.endpoint());
        entity.setAuthType(request.authType());
        entity.setTimeoutMs(defaultInteger(request.timeoutMs(), 30000));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setPublishStatus(defaultString(request.publishStatus(), "PUBLISHED"));
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        entity.setAllowFrontSelect(defaultBoolean(request.allowFrontSelect(), true));
        entity.setSupportedOptions(defaultString(request.supportedOptions(), "{}"));
        entity.setRemark(request.remark());
        modelServiceMapper.insert(entity);
        return toVO(findByCode(request.serviceCode()));
    }

    @Override
    @Transactional
    public ModelServiceVO update(String serviceCode, ModelServiceUpdateRequest request) {
        ModelServiceEntity entity = findByCode(serviceCode);
        entity.setServiceName(request.serviceName());
        entity.setModelCode(request.modelCode());
        entity.setModelName(request.modelName());
        entity.setModelType(request.modelType());
        entity.setFunctionCode(request.functionCode());
        entity.setEndpoint(request.endpoint());
        entity.setAuthType(request.authType());
        entity.setTimeoutMs(defaultInteger(request.timeoutMs(), 30000));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setPublishStatus(defaultString(request.publishStatus(), "PUBLISHED"));
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        entity.setAllowFrontSelect(defaultBoolean(request.allowFrontSelect(), true));
        entity.setSupportedOptions(defaultString(request.supportedOptions(), "{}"));
        entity.setRemark(request.remark());
        modelServiceMapper.updateById(entity);
        return toVO(findByCode(serviceCode));
    }

    @Override
    @Transactional
    public void updateEnabled(String serviceCode, boolean enabled) {
        ModelServiceEntity entity = findByCode(serviceCode);
        entity.setEnabled(enabled);
        modelServiceMapper.updateById(entity);
    }

    private boolean existsByCode(String serviceCode) {
        return modelServiceMapper.selectCount(new LambdaQueryWrapper<ModelServiceEntity>()
                .eq(ModelServiceEntity::getServiceCode, serviceCode)) > 0;
    }

    private ModelServiceEntity findByCode(String serviceCode) {
        ModelServiceEntity entity = modelServiceMapper.selectOne(new LambdaQueryWrapper<ModelServiceEntity>()
                .eq(ModelServiceEntity::getServiceCode, serviceCode)
                .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "model service not found");
        }
        return entity;
    }

    private ModelServiceVO toVO(ModelServiceEntity entity) {
        return ModelServiceVO.builder()
                .id(entity.getId())
                .serviceCode(entity.getServiceCode())
                .serviceName(entity.getServiceName())
                .modelCode(entity.getModelCode())
                .modelName(entity.getModelName())
                .modelType(entity.getModelType())
                .functionCode(entity.getFunctionCode())
                .endpoint(entity.getEndpoint())
                .authType(entity.getAuthType())
                .timeoutMs(entity.getTimeoutMs())
                .enabled(entity.getEnabled())
                .publishStatus(entity.getPublishStatus())
                .isDefault(entity.getIsDefault())
                .allowFrontSelect(entity.getAllowFrontSelect())
                .supportedOptions(entity.getSupportedOptions())
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

    private String defaultString(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}

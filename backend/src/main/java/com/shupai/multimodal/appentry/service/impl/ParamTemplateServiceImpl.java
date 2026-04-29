package com.shupai.multimodal.appentry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.mapper.ParamTemplateMapper;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateQuery;
import com.shupai.multimodal.appentry.model.dto.ParamTemplateUpdateRequest;
import com.shupai.multimodal.appentry.model.entity.ParamTemplateEntity;
import com.shupai.multimodal.appentry.model.vo.ParamTemplateVO;
import com.shupai.multimodal.appentry.service.ParamTemplateService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ParamTemplateServiceImpl implements ParamTemplateService {

    private final ParamTemplateMapper paramTemplateMapper;

    @Override
    public PageResponse<ParamTemplateVO> page(ParamTemplateQuery query) {
        long pageNum = query.pageNum() == null || query.pageNum() < 1 ? 1L : query.pageNum();
        long pageSize = query.pageSize() == null || query.pageSize() < 1 ? 10L : query.pageSize();

        LambdaQueryWrapper<ParamTemplateEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.enabled() != null, ParamTemplateEntity::getEnabled, query.enabled());
        wrapper.eq(StringUtils.hasText(query.functionCode()), ParamTemplateEntity::getFunctionCode, query.functionCode());
        wrapper.eq(StringUtils.hasText(query.serviceCode()), ParamTemplateEntity::getServiceCode, query.serviceCode());
        wrapper.eq(StringUtils.hasText(query.templateType()), ParamTemplateEntity::getTemplateType, query.templateType());
        wrapper.orderByAsc(ParamTemplateEntity::getSortOrder)
                .orderByDesc(ParamTemplateEntity::getCreatedAt);

        IPage<ParamTemplateEntity> page = paramTemplateMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ParamTemplateVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResponse<>(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public ParamTemplateVO getByCode(String templateCode) {
        return toVO(findByCode(templateCode));
    }

    @Override
    @Transactional
    public ParamTemplateVO create(ParamTemplateCreateRequest request) {
        if (existsByCode(request.templateCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "templateCode already exists");
        }

        ParamTemplateEntity entity = new ParamTemplateEntity();
        entity.setTemplateCode(request.templateCode());
        entity.setTemplateName(request.templateName());
        entity.setFunctionCode(request.functionCode());
        entity.setServiceCode(request.serviceCode());
        entity.setTemplateType(defaultString(request.templateType(), "default"));
        entity.setTemplatePayload(request.templatePayload());
        entity.setPresetParams(request.presetParams());
        entity.setDescription(request.description());
        entity.setSortOrder(defaultInteger(request.sortOrder(), 0));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        paramTemplateMapper.insert(entity);
        return toVO(findByCode(request.templateCode()));
    }

    @Override
    @Transactional
    public ParamTemplateVO update(String templateCode, ParamTemplateUpdateRequest request) {
        ParamTemplateEntity entity = findByCode(templateCode);
        entity.setTemplateName(request.templateName());
        entity.setFunctionCode(request.functionCode());
        entity.setServiceCode(request.serviceCode());
        entity.setTemplateType(defaultString(request.templateType(), "default"));
        entity.setTemplatePayload(request.templatePayload());
        entity.setPresetParams(request.presetParams());
        entity.setDescription(request.description());
        entity.setSortOrder(defaultInteger(request.sortOrder(), 0));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        paramTemplateMapper.updateById(entity);
        return toVO(findByCode(templateCode));
    }

    @Override
    @Transactional
    public void updateEnabled(String templateCode, boolean enabled) {
        ParamTemplateEntity entity = findByCode(templateCode);
        entity.setEnabled(enabled);
        paramTemplateMapper.updateById(entity);
    }

    private boolean existsByCode(String templateCode) {
        return paramTemplateMapper.selectCount(new LambdaQueryWrapper<ParamTemplateEntity>()
                .eq(ParamTemplateEntity::getTemplateCode, templateCode)) > 0;
    }

    private ParamTemplateEntity findByCode(String templateCode) {
        ParamTemplateEntity entity = paramTemplateMapper.selectOne(new LambdaQueryWrapper<ParamTemplateEntity>()
                .eq(ParamTemplateEntity::getTemplateCode, templateCode)
                .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "param template not found");
        }
        return entity;
    }

    private ParamTemplateVO toVO(ParamTemplateEntity entity) {
        return ParamTemplateVO.builder()
                .id(entity.getId())
                .templateCode(entity.getTemplateCode())
                .templateName(entity.getTemplateName())
                .functionCode(entity.getFunctionCode())
                .serviceCode(entity.getServiceCode())
                .templateType(entity.getTemplateType())
                .templatePayload(entity.getTemplatePayload())
                .presetParams(entity.getPresetParams())
                .description(entity.getDescription())
                .sortOrder(entity.getSortOrder())
                .enabled(entity.getEnabled())
                .isDefault(entity.getIsDefault())
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

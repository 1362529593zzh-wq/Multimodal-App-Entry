package com.shupai.multimodal.appentry.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shupai.multimodal.appentry.common.PageResponse;
import com.shupai.multimodal.appentry.mapper.FunctionModelBindingMapper;
import com.shupai.multimodal.appentry.mapper.ModelServiceMapper;
import com.shupai.multimodal.appentry.model.dto.ModelServiceCreateRequest;
import com.shupai.multimodal.appentry.model.dto.ModelServiceQuery;
import com.shupai.multimodal.appentry.model.dto.ModelServiceUpdateRequest;
import com.shupai.multimodal.appentry.model.entity.FunctionModelBindingEntity;
import com.shupai.multimodal.appentry.model.entity.ModelServiceEntity;
import com.shupai.multimodal.appentry.model.vo.ModelServiceVO;
import com.shupai.multimodal.appentry.model.vo.ModelServiceVendorVO;
import com.shupai.multimodal.appentry.service.ModelServiceService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final FunctionModelBindingMapper functionModelBindingMapper;

    @Override
    public PageResponse<ModelServiceVO> page(ModelServiceQuery query) {
        long pageNum = query.pageNum() == null || query.pageNum() < 1 ? 1L : query.pageNum();
        long pageSize = query.pageSize() == null || query.pageSize() < 1 ? 10L : query.pageSize();

        LambdaQueryWrapper<ModelServiceEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.enabled() != null, ModelServiceEntity::getEnabled, query.enabled());
        wrapper.eq(StringUtils.hasText(query.functionCode()), ModelServiceEntity::getFunctionCode, query.functionCode());
        wrapper.eq(StringUtils.hasText(query.vendorCode()), ModelServiceEntity::getVendorCode, query.vendorCode());
        wrapper.eq(StringUtils.hasText(query.providerType()), ModelServiceEntity::getProviderType, query.providerType());
        wrapper.eq(StringUtils.hasText(query.modelType()), ModelServiceEntity::getModelType, query.modelType());
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(ModelServiceEntity::getServiceCode, query.keyword())
                    .or()
                    .like(ModelServiceEntity::getServiceName, query.keyword())
                    .or()
                    .like(ModelServiceEntity::getVendorName, query.keyword())
                    .or()
                    .like(ModelServiceEntity::getModelName, query.keyword()));
        }
        wrapper.orderByAsc(ModelServiceEntity::getSortOrder)
                .orderByDesc(ModelServiceEntity::getIsDefault)
                .orderByDesc(ModelServiceEntity::getCreatedAt);

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
        entity.setVendorCode(defaultString(request.vendorCode(), "custom"));
        entity.setVendorName(defaultString(request.vendorName(), "自定义"));
        entity.setVendorType(defaultString(request.vendorType(), "custom"));
        entity.setProviderType(defaultString(request.providerType(), "openai_compatible"));
        entity.setEndpoint(request.endpoint());
        entity.setAuthType(request.authType());
        entity.setSecretRef(request.secretRef());
        entity.setApiKeyMasked(maskSecret(request.apiKey()));
        entity.setSecretKeyMasked(maskSecret(request.secretKey()));
        entity.setApiKeySecret(trimToNull(request.apiKey()));
        entity.setSecretKeySecret(trimToNull(request.secretKey()));
        entity.setRequestMethod(defaultString(request.requestMethod(), "POST"));
        entity.setHeaderTemplate(defaultJson(request.headerTemplate()));
        entity.setPayloadTemplate(defaultJson(request.payloadTemplate()));
        entity.setExtraConfig(defaultJson(request.extraConfig()));
        entity.setTimeoutMs(defaultInteger(request.timeoutMs(), 30000));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setPublishStatus(defaultString(request.publishStatus(), "PUBLISHED"));
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        entity.setAllowFrontSelect(defaultBoolean(request.allowFrontSelect(), true));
        entity.setSupportedOptions(defaultString(request.supportedOptions(), "{}"));
        entity.setSortOrder(defaultInteger(request.sortOrder(), 0));
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
        entity.setVendorCode(defaultString(request.vendorCode(), "custom"));
        entity.setVendorName(defaultString(request.vendorName(), "自定义"));
        entity.setVendorType(defaultString(request.vendorType(), "custom"));
        entity.setProviderType(defaultString(request.providerType(), "openai_compatible"));
        entity.setEndpoint(request.endpoint());
        entity.setAuthType(request.authType());
        entity.setSecretRef(request.secretRef());
        if (StringUtils.hasText(request.apiKey())) {
            entity.setApiKeyMasked(maskSecret(request.apiKey()));
            entity.setApiKeySecret(request.apiKey().trim());
        }
        if (StringUtils.hasText(request.secretKey())) {
            entity.setSecretKeyMasked(maskSecret(request.secretKey()));
            entity.setSecretKeySecret(request.secretKey().trim());
        }
        entity.setRequestMethod(defaultString(request.requestMethod(), "POST"));
        entity.setHeaderTemplate(defaultJson(request.headerTemplate()));
        entity.setPayloadTemplate(defaultJson(request.payloadTemplate()));
        entity.setExtraConfig(defaultJson(request.extraConfig()));
        entity.setTimeoutMs(defaultInteger(request.timeoutMs(), 30000));
        entity.setEnabled(defaultBoolean(request.enabled(), true));
        entity.setPublishStatus(defaultString(request.publishStatus(), "PUBLISHED"));
        entity.setIsDefault(defaultBoolean(request.isDefault(), false));
        entity.setAllowFrontSelect(defaultBoolean(request.allowFrontSelect(), true));
        entity.setSupportedOptions(defaultString(request.supportedOptions(), "{}"));
        entity.setSortOrder(defaultInteger(request.sortOrder(), 0));
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

    @Override
    @Transactional
    public void delete(String serviceCode) {
        ModelServiceEntity entity = findByCode(serviceCode);
        Long bindingCount = functionModelBindingMapper.selectCount(new LambdaQueryWrapper<FunctionModelBindingEntity>()
                .eq(FunctionModelBindingEntity::getServiceCode, serviceCode)
                .eq(FunctionModelBindingEntity::getEnabled, true));
        if (bindingCount != null && bindingCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "model service is still bound to enabled functions");
        }
        modelServiceMapper.deleteById(entity.getId());
    }

    @Override
    public List<ModelServiceVendorVO> listVendors() {
        List<ModelServiceEntity> services = modelServiceMapper.selectList(new LambdaQueryWrapper<ModelServiceEntity>()
                .orderByAsc(ModelServiceEntity::getSortOrder)
                .orderByAsc(ModelServiceEntity::getVendorName));
        Map<String, List<ModelServiceEntity>> grouped = services.stream()
                .collect(Collectors.groupingBy(
                        service -> defaultString(service.getVendorCode(), "default_vendor"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        return grouped.entrySet().stream()
                .map(entry -> {
                    List<ModelServiceEntity> vendorServices = entry.getValue();
                    ModelServiceEntity first = vendorServices.get(0);
                    long enabledCount = vendorServices.stream()
                            .filter(service -> Boolean.TRUE.equals(service.getEnabled()))
                            .count();
                    int sortOrder = vendorServices.stream()
                            .map(ModelServiceEntity::getSortOrder)
                            .filter(value -> value != null)
                            .min(Integer::compareTo)
                            .orElse(0);
                    return ModelServiceVendorVO.builder()
                            .vendorCode(entry.getKey())
                            .vendorName(defaultString(first.getVendorName(), entry.getKey()))
                            .vendorType(defaultString(first.getVendorType(), "official"))
                            .total((long) vendorServices.size())
                            .enabledCount(enabledCount)
                            .sortOrder(sortOrder)
                            .build();
                })
                .toList();
    }

    @Override
    public Map<String, Object> testConnection(String serviceCode) {
        ModelServiceEntity entity = findByCode(serviceCode);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serviceCode", entity.getServiceCode());
        result.put("providerType", defaultString(entity.getProviderType(), "openai_compatible"));
        result.put("endpoint", entity.getEndpoint());
        result.put("enabled", entity.getEnabled());
        result.put("success", StringUtils.hasText(entity.getEndpoint()) && !entity.getEndpoint().contains("mock-api"));
        result.put("message", StringUtils.hasText(entity.getEndpoint())
                ? "配置已具备测试所需 endpoint；真实连通性将在 provider 适配器接入后执行。"
                : "当前服务未配置 endpoint。");
        return result;
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
                .vendorCode(entity.getVendorCode())
                .vendorName(entity.getVendorName())
                .vendorType(entity.getVendorType())
                .providerType(entity.getProviderType())
                .endpoint(entity.getEndpoint())
                .authType(entity.getAuthType())
                .secretRef(entity.getSecretRef())
                .apiKeyMasked(entity.getApiKeyMasked())
                .secretKeyMasked(entity.getSecretKeyMasked())
                .requestMethod(entity.getRequestMethod())
                .headerTemplate(entity.getHeaderTemplate())
                .payloadTemplate(entity.getPayloadTemplate())
                .extraConfig(entity.getExtraConfig())
                .timeoutMs(entity.getTimeoutMs())
                .enabled(entity.getEnabled())
                .publishStatus(entity.getPublishStatus())
                .isDefault(entity.getIsDefault())
                .allowFrontSelect(entity.getAllowFrontSelect())
                .supportedOptions(entity.getSupportedOptions())
                .sortOrder(entity.getSortOrder())
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

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultJson(String value) {
        return StringUtils.hasText(value) ? value : "{}";
    }

    private String maskSecret(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "********";
        }
        return trimmed.substring(0, 3) + "********" + trimmed.substring(trimmed.length() - 4);
    }
}

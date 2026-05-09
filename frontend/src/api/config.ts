import { requestData, requestEnvelope } from './http';
import type {
  FunctionConfig,
  FunctionConfigPayload,
  FunctionConfigQuery,
  FunctionModelBinding,
  FunctionModelBindingPayload,
  FunctionModelBindingQuery,
  ModelService,
  ModelServicePayload,
  ModelServiceQuery,
  ModelServiceVendor,
  PageResponse,
  ParamTemplate,
  ParamTemplatePayload,
  ParamTemplateQuery,
} from '../types/api';

export const fetchFunctionConfigs = (params: FunctionConfigQuery) =>
  requestData<PageResponse<FunctionConfig>>({
    url: '/api/config/functions',
    method: 'GET',
    params,
  });

export const createFunctionConfig = (payload: FunctionConfigPayload) =>
  requestEnvelope<FunctionConfig>({
    url: '/api/config/functions',
    method: 'POST',
    data: payload,
  });

export const updateFunctionConfig = (functionCode: string, payload: FunctionConfigPayload) =>
  requestEnvelope<FunctionConfig>({
    url: `/api/config/functions/${encodeURIComponent(functionCode)}`,
    method: 'PUT',
    data: payload,
  });

export const toggleFunctionConfig = (functionCode: string, enabled: boolean) =>
  requestEnvelope<null>({
    url: `/api/config/functions/${encodeURIComponent(functionCode)}/enabled`,
    method: 'PATCH',
    params: { enabled },
  });

export const fetchModelServices = (params: ModelServiceQuery) =>
  requestData<PageResponse<ModelService>>({
    url: '/api/config/model-services',
    method: 'GET',
    params,
  });

export const createModelService = (payload: ModelServicePayload) =>
  requestEnvelope<ModelService>({
    url: '/api/config/model-services',
    method: 'POST',
    data: payload,
  });

export const updateModelService = (serviceCode: string, payload: ModelServicePayload) =>
  requestEnvelope<ModelService>({
    url: `/api/config/model-services/${encodeURIComponent(serviceCode)}`,
    method: 'PUT',
    data: payload,
  });

export const toggleModelService = (serviceCode: string, enabled: boolean) =>
  requestEnvelope<null>({
    url: `/api/config/model-services/${encodeURIComponent(serviceCode)}/enabled`,
    method: 'PATCH',
    params: { enabled },
  });

export const deleteModelService = (serviceCode: string) =>
  requestEnvelope<null>({
    url: `/api/config/model-services/${encodeURIComponent(serviceCode)}`,
    method: 'DELETE',
  });

export const fetchModelServiceVendors = () =>
  requestData<ModelServiceVendor[]>({
    url: '/api/config/model-services/vendors',
    method: 'GET',
  });

export const testModelService = (serviceCode: string) =>
  requestEnvelope<Record<string, unknown>>({
    url: `/api/config/model-services/${encodeURIComponent(serviceCode)}/test`,
    method: 'POST',
  });

export const fetchFunctionModelBindings = (params: FunctionModelBindingQuery) =>
  requestData<PageResponse<FunctionModelBinding>>({
    url: '/api/config/function-model-bindings',
    method: 'GET',
    params,
  });

export const createFunctionModelBinding = (payload: FunctionModelBindingPayload) =>
  requestEnvelope<FunctionModelBinding>({
    url: '/api/config/function-model-bindings',
    method: 'POST',
    data: payload,
  });

export const updateFunctionModelBinding = (
  functionCode: string,
  serviceCode: string,
  payload: FunctionModelBindingPayload,
) =>
  requestEnvelope<FunctionModelBinding>({
    url: `/api/config/function-model-bindings/${encodeURIComponent(functionCode)}/${encodeURIComponent(serviceCode)}`,
    method: 'PUT',
    data: payload,
  });

export const toggleFunctionModelBinding = (functionCode: string, serviceCode: string, enabled: boolean) =>
  requestEnvelope<null>({
    url: `/api/config/function-model-bindings/${encodeURIComponent(functionCode)}/${encodeURIComponent(serviceCode)}/enabled`,
    method: 'PATCH',
    params: { enabled },
  });

export const fetchParamTemplates = (params: ParamTemplateQuery) =>
  requestData<PageResponse<ParamTemplate>>({
    url: '/api/config/param-templates',
    method: 'GET',
    params,
  });

export const createParamTemplate = (payload: ParamTemplatePayload) =>
  requestEnvelope<ParamTemplate>({
    url: '/api/config/param-templates',
    method: 'POST',
    data: payload,
  });

export const updateParamTemplate = (templateCode: string, payload: ParamTemplatePayload) =>
  requestEnvelope<ParamTemplate>({
    url: `/api/config/param-templates/${encodeURIComponent(templateCode)}`,
    method: 'PUT',
    data: payload,
  });

export const toggleParamTemplate = (templateCode: string, enabled: boolean) =>
  requestEnvelope<null>({
    url: `/api/config/param-templates/${encodeURIComponent(templateCode)}/enabled`,
    method: 'PATCH',
    params: { enabled },
  });

export const fetchFunctionDictionary = async () => {
  const page = await fetchFunctionConfigs({ pageNum: 1, pageSize: 200 });
  return page.records;
};

export const fetchServiceDictionary = async () => {
  const page = await fetchModelServices({ pageNum: 1, pageSize: 400 });
  return page.records;
};

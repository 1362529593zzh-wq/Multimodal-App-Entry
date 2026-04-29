import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios';
import type { ApiEnvelope } from '../types/api';

export class ApiError extends Error {
  code?: number;
  status?: number;
  fieldErrors?: Record<string, string>;

  constructor(
    message: string,
    options?: {
      code?: number;
      status?: number;
      fieldErrors?: Record<string, string>;
    },
  ) {
    super(message);
    this.name = 'ApiError';
    this.code = options?.code;
    this.status = options?.status;
    this.fieldErrors = options?.fieldErrors;
  }
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
});

http.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => Promise.reject(toApiError(error)),
);

const isFieldErrorMap = (value: unknown): value is Record<string, string> => {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    return false;
  }
  return Object.values(value).every((item) => typeof item === 'string');
};

const unwrapEnvelope = <T>(response: AxiosResponse<ApiEnvelope<T>>): ApiEnvelope<T> => {
  const body = response.data;
  if (!body || typeof body.code !== 'number') {
    throw new ApiError('响应格式异常', { status: response.status });
  }
  if (body.code !== 0) {
    throw new ApiError(body.message || '请求失败', {
      code: body.code,
      status: response.status,
      fieldErrors: isFieldErrorMap(body.data) ? body.data : undefined,
    });
  }
  return body;
};

const toApiError = (error: AxiosError<ApiEnvelope<unknown>>): ApiError => {
  const responseBody = error.response?.data;
  if (responseBody && typeof responseBody.code === 'number') {
    return new ApiError(responseBody.message || '请求失败', {
      code: responseBody.code,
      status: error.response?.status,
      fieldErrors: isFieldErrorMap(responseBody.data) ? responseBody.data : undefined,
    });
  }
  if (error.code === 'ECONNABORTED') {
    return new ApiError('请求超时，请检查后端服务是否可用');
  }
  if (error.message === 'Network Error') {
    return new ApiError('网络异常，请确认前端代理与后端服务已启动');
  }
  return new ApiError(error.message || '未知错误');
};

export const requestEnvelope = async <T>(config: AxiosRequestConfig): Promise<ApiEnvelope<T>> => {
  const response = await http.request<ApiEnvelope<T>>(config);
  return unwrapEnvelope(response);
};

export const requestData = async <T>(config: AxiosRequestConfig): Promise<T> => {
  const envelope = await requestEnvelope<T>(config);
  return envelope.data;
};

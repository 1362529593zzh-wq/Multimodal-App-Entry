import { message } from 'antd';
import type { FormInstance, Rule } from 'antd/es/form';
import { ApiError } from '../api/http';

export type EnabledFilterValue = 'all' | 'enabled' | 'disabled';

export const enabledFilterOptions = [
  { label: '全部状态', value: 'all' },
  { label: '仅启用', value: 'enabled' },
  { label: '仅停用', value: 'disabled' },
];

export const parseEnabledFilter = (value?: EnabledFilterValue) => {
  if (value === 'enabled') {
    return true;
  }
  if (value === 'disabled') {
    return false;
  }
  return undefined;
};

export const sanitizeOptionalString = (value?: string | null) => {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
};

export const jsonStringRule = (label: string): Rule => ({
  validator: async (_, value?: string) => {
    if (!value || !value.trim()) {
      throw new Error(`请输入${label}`);
    }
    try {
      JSON.parse(value);
    } catch {
      throw new Error(`${label}必须是合法 JSON`);
    }
  },
});

export const applyServerFieldErrors = (form: FormInstance, error: unknown) => {
  if (error instanceof ApiError && error.fieldErrors) {
    form.setFields(
      Object.entries(error.fieldErrors).map(([name, errorMessage]) => ({
        name,
        errors: [errorMessage],
      })),
    );
  }
  const errorMessage = error instanceof Error ? error.message : '操作失败';
  message.error(errorMessage);
};

export const announceError = (error: unknown) => {
  message.error(error instanceof Error ? error.message : '操作失败');
};

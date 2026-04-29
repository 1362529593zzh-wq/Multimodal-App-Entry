import dayjs from 'dayjs';

export const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '--';
  }
  return dayjs(value).format('YYYY-MM-DD HH:mm');
};

export const formatJsonForEditor = (value?: string | null, fallback = '{}') => {
  if (!value) {
    return fallback;
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

export const normalizeJsonString = (value: string) => JSON.stringify(JSON.parse(value));

export const formatJsonPreview = (value?: string | null) => {
  if (!value) {
    return '--';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

export const compactText = (value?: string | null, fallback = '--') => {
  if (!value || !value.trim()) {
    return fallback;
  }
  return value;
};

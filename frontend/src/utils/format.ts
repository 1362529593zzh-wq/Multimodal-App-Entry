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

export const formatDuration = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '--';
  }
  if (value < 1000) {
    return `${value} ms`;
  }
  const seconds = value / 1000;
  if (seconds < 60) {
    return `${seconds.toFixed(2)} s`;
  }
  const minutes = Math.floor(seconds / 60);
  const restSeconds = Math.round(seconds % 60);
  return `${minutes} min ${restSeconds} s`;
};

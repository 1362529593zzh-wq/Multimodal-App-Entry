import { requestData } from './http';
import type { CallRecord, CallRecordQuery, PageResponse } from '../types/api';

export const fetchCallRecords = (params: CallRecordQuery) =>
  requestData<PageResponse<CallRecord>>({
    url: '/api/call-records',
    method: 'GET',
    params,
  });

export const fetchCallRecordDetail = (recordId: string) =>
  requestData<CallRecord>({
    url: `/api/call-records/${encodeURIComponent(recordId)}`,
    method: 'GET',
  });

export const exportCallRecordsUrl = (params: Partial<CallRecordQuery>) => {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
  const query = search.toString();
  return `${baseUrl}/api/call-records/export${query ? `?${query}` : ''}`;
};

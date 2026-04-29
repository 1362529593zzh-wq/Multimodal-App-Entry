import { useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';
import { fetchFunctionDictionary, fetchServiceDictionary } from '../api/config';
import type { SelectOptionItem } from '../types/api';

export const useDictionaries = () => {
  const functionQuery = useQuery({
    queryKey: ['dictionary', 'functions'],
    queryFn: fetchFunctionDictionary,
    staleTime: 60_000,
  });

  const serviceQuery = useQuery({
    queryKey: ['dictionary', 'services'],
    queryFn: fetchServiceDictionary,
    staleTime: 60_000,
  });

  const functionOptions = useMemo<SelectOptionItem[]>(
    () =>
      (functionQuery.data ?? []).map((item) => ({
        label: `${item.functionName} · ${item.functionCode}`,
        value: item.functionCode,
        meta: item.functionName,
      })),
    [functionQuery.data],
  );

  const serviceOptions = useMemo<SelectOptionItem[]>(
    () =>
      (serviceQuery.data ?? []).map((item) => ({
        label: `${item.serviceName} · ${item.serviceCode}`,
        value: item.serviceCode,
        meta: item.functionCode,
      })),
    [serviceQuery.data],
  );

  const functionNameMap = useMemo(
    () => Object.fromEntries((functionQuery.data ?? []).map((item) => [item.functionCode, item.functionName])),
    [functionQuery.data],
  );

  const serviceNameMap = useMemo(
    () => Object.fromEntries((serviceQuery.data ?? []).map((item) => [item.serviceCode, item.serviceName])),
    [serviceQuery.data],
  );

  return {
    functionOptions,
    serviceOptions,
    functionNameMap,
    serviceNameMap,
    isLoading: functionQuery.isLoading || serviceQuery.isLoading,
  };
};

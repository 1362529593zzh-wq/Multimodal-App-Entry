import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Descriptions, Form, Input, Modal, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import {
  createFunctionModelBinding,
  fetchFunctionModelBindings,
  toggleFunctionModelBinding,
  updateFunctionModelBinding,
} from '../../api/config';
import { PageIntro } from '../../components/page-intro';
import { StatusBadge } from '../../components/status-badge';
import { useDictionaries } from '../../hooks/use-dictionaries';
import type { FunctionModelBinding, FunctionModelBindingPayload } from '../../types/api';
import { compactText, formatDateTime } from '../../utils/format';
import {
  announceError,
  applyServerFieldErrors,
  enabledFilterOptions,
  parseEnabledFilter,
  sanitizeOptionalString,
} from '../../utils/forms';

interface FilterValues {
  functionCode?: string;
  serviceCode?: string;
  enabled?: 'all' | 'enabled' | 'disabled';
}

interface FormValues {
  functionCode?: string;
  serviceCode?: string;
  isDefault: boolean;
  sortOrder: number;
  enabled: boolean;
  remark?: string;
}

export const FunctionModelBindingPage = () => {
  const queryClient = useQueryClient();
  const { functionOptions, serviceOptions, functionNameMap, serviceNameMap } = useDictionaries();
  const [filterForm] = Form.useForm<FilterValues>();
  const [editForm] = Form.useForm<FormValues>();
  const [filters, setFilters] = useState<FilterValues>({ enabled: 'all' });
  const [pagination, setPagination] = useState({ pageNum: 1, pageSize: 10 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<FunctionModelBinding | null>(null);
  const [togglingKey, setTogglingKey] = useState<string>();

  const query = useQuery({
    queryKey: ['function-model-bindings', filters, pagination],
    queryFn: () =>
      fetchFunctionModelBindings({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        functionCode: sanitizeOptionalString(filters.functionCode),
        serviceCode: sanitizeOptionalString(filters.serviceCode),
        enabled: parseEnabledFilter(filters.enabled),
      }),
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['function-model-bindings'] });
  };

  const saveMutation = useMutation({
    mutationFn: (payload: FunctionModelBindingPayload) =>
      editingRecord
        ? updateFunctionModelBinding(editingRecord.functionCode, editingRecord.serviceCode, payload)
        : createFunctionModelBinding(payload),
    onSuccess: async (result) => {
      message.success(result.message);
      setModalOpen(false);
      setEditingRecord(null);
      editForm.resetFields();
      await invalidate();
    },
    onError: (error) => applyServerFieldErrors(editForm, error),
  });

  const watchedFunctionCode = Form.useWatch('functionCode', editForm);

  const filteredServiceOptions = useMemo(() => {
    if (!watchedFunctionCode || editingRecord) {
      return serviceOptions;
    }
    return serviceOptions.filter((item) => item.meta === watchedFunctionCode);
  }, [editingRecord, serviceOptions, watchedFunctionCode]);

  function openModal(record?: FunctionModelBinding) {
    setEditingRecord(record ?? null);
    setModalOpen(true);
    editForm.setFieldsValue(
      record
        ? {
            ...record,
            remark: record.remark ?? undefined,
          }
        : {
            sortOrder: 0,
            enabled: true,
            isDefault: false,
          },
    );
  }

  function closeModal() {
    setModalOpen(false);
    setEditingRecord(null);
    editForm.resetFields();
  }

  async function handleToggle(record: FunctionModelBinding, enabled: boolean) {
    const key = `${record.functionCode}:${record.serviceCode}`;
    try {
      setTogglingKey(key);
      const result = await toggleFunctionModelBinding(record.functionCode, record.serviceCode, enabled);
      message.success(result.message);
      await invalidate();
    } catch (error) {
      announceError(error);
    } finally {
      setTogglingKey(undefined);
    }
  }

  const columns: ColumnsType<FunctionModelBinding> = [
    {
      title: '功能能力',
      dataIndex: 'functionCode',
      width: 220,
      render: (value: string) => (
        <div>
          <span className="table-code">{value}</span>
          <div className="table-subtext">{functionNameMap[value] ?? '未命名功能'}</div>
        </div>
      ),
    },
    {
      title: '绑定服务',
      dataIndex: 'serviceCode',
      width: 220,
      render: (value: string) => (
        <div>
          <span className="table-code">{value}</span>
          <div className="table-subtext">{serviceNameMap[value] ?? '未命名服务'}</div>
        </div>
      ),
    },
    {
      title: '策略',
      key: 'strategy',
      width: 200,
      render: (_, record) => (
        <Space wrap>
          {record.isDefault ? <Tag color="gold">默认绑定</Tag> : <Tag>普通绑定</Tag>}
          <Tag color="processing">排序 {record.sortOrder}</Tag>
        </Space>
      ),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      width: 240,
      render: (value: string | null) => compactText(value),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 120,
      render: (value: boolean) => <StatusBadge active={value} />,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 144,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'action',
      width: 170,
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => openModal(record)}>
            编辑
          </Button>
          <Switch
            checked={record.enabled}
            loading={togglingKey === `${record.functionCode}:${record.serviceCode}`}
            onChange={(checked) => handleToggle(record, checked)}
          />
        </Space>
      ),
    },
  ];

  const submitFilters = () => {
    setPagination((prev) => ({ ...prev, pageNum: 1 }));
    setFilters(filterForm.getFieldsValue());
  };

  const resetFilters = () => {
    filterForm.setFieldsValue({ functionCode: undefined, serviceCode: undefined, enabled: 'all' });
    setPagination((prev) => ({ ...prev, pageNum: 1 }));
    setFilters({ enabled: 'all' });
  };

  const handleSubmit = async () => {
    const values = await editForm.validateFields();
    const payload: FunctionModelBindingPayload = editingRecord
      ? {
          isDefault: values.isDefault,
          sortOrder: values.sortOrder,
          enabled: values.enabled,
          remark: sanitizeOptionalString(values.remark),
        }
      : {
          functionCode: values.functionCode?.trim(),
          serviceCode: values.serviceCode?.trim(),
          isDefault: values.isDefault,
          sortOrder: values.sortOrder,
          enabled: values.enabled,
          remark: sanitizeOptionalString(values.remark),
        };
    saveMutation.mutate(payload);
  };

  return (
    <div className="page-stack">
      <PageIntro
        eyebrow="Configuration / Function Bindings"
        title="功能模型绑定页"
        description="把功能与模型服务的落地关系展示出来，支持默认项、排序和启停控制，是前端完成配置闭环的第三步。"
        stats={[
          { label: '当前记录', value: query.data?.total ?? '--', hint: '双主键 functionCode + serviceCode' },
          { label: '依赖来源', value: '功能 + 服务字典', hint: '表单选项直接复用前两页数据' },
        ]}
        actions={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新增绑定
          </Button>
        }
      />

      <Card className="surface-card">
        <Form form={filterForm} layout="inline" initialValues={filters} className="filter-toolbar">
          <Form.Item name="functionCode">
            <Select
              allowClear
              showSearch
              placeholder="按功能筛选"
              optionFilterProp="label"
              style={{ width: 220 }}
              options={functionOptions}
            />
          </Form.Item>
          <Form.Item name="serviceCode">
            <Select
              allowClear
              showSearch
              placeholder="按服务筛选"
              optionFilterProp="label"
              style={{ width: 240 }}
              options={serviceOptions}
            />
          </Form.Item>
          <Form.Item name="enabled">
            <Select options={enabledFilterOptions} style={{ width: 144 }} />
          </Form.Item>
          <Button type="primary" onClick={submitFilters}>
            查询
          </Button>
          <Button icon={<ReloadOutlined />} onClick={resetFilters}>
            重置
          </Button>
        </Form>

        <Table
          rowKey={(record) => `${record.functionCode}:${record.serviceCode}`}
          loading={query.isLoading}
          columns={columns}
          dataSource={query.data?.records ?? []}
          className="data-table"
          scroll={{ x: 1340 }}
          pagination={{
            current: pagination.pageNum,
            pageSize: pagination.pageSize,
            total: query.data?.total ?? 0,
            showSizeChanger: true,
            onChange: (pageNum, pageSize) => setPagination({ pageNum, pageSize }),
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </Card>

      <Modal
        title={editingRecord ? '编辑功能绑定' : '新增功能绑定'}
        open={modalOpen}
        width={760}
        destroyOnClose
        onCancel={closeModal}
        onOk={handleSubmit}
        confirmLoading={saveMutation.isPending}
      >
        <Form form={editForm} layout="vertical">
          {editingRecord ? (
            <Descriptions column={2} size="small" className="record-meta">
              <Descriptions.Item label="功能编码">{editingRecord.functionCode}</Descriptions.Item>
              <Descriptions.Item label="服务编码">{editingRecord.serviceCode}</Descriptions.Item>
            </Descriptions>
          ) : (
            <div className="form-grid form-grid--two">
              <Form.Item label="功能编码" name="functionCode" rules={[{ required: true, message: '请选择功能编码' }]}>
                <Select showSearch optionFilterProp="label" options={functionOptions} />
              </Form.Item>
              <Form.Item label="服务编码" name="serviceCode" rules={[{ required: true, message: '请选择服务编码' }]}>
                <Select showSearch optionFilterProp="label" options={filteredServiceOptions} />
              </Form.Item>
            </div>
          )}

          <div className="form-grid form-grid--two">
            <Form.Item label="排序" name="sortOrder">
              <Input type="number" min={0} />
            </Form.Item>
            <Form.Item label="备注" name="remark">
              <Input placeholder="例如：图片生成默认绑定" />
            </Form.Item>
          </div>

          <div className="toggle-grid">
            <Form.Item label="启用状态" name="enabled" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="默认绑定" name="isDefault" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
};

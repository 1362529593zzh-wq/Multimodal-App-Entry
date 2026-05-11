import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, Modal, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { createFunctionConfig, fetchFunctionConfigs, toggleFunctionConfig, updateFunctionConfig } from '../../api/config';
import { PageIntro } from '../../components/page-intro';
import { StatusBadge } from '../../components/status-badge';
import { useDictionaries } from '../../hooks/use-dictionaries';
import type { FunctionConfig, FunctionConfigPayload } from '../../types/api';
import { compactText, formatDateTime } from '../../utils/format';
import {
  announceError,
  applyServerFieldErrors,
  enabledFilterOptions,
  parseEnabledFilter,
  sanitizeOptionalString,
} from '../../utils/forms';

interface FilterValues {
  keyword?: string;
  enabled?: 'all' | 'enabled' | 'disabled';
}

interface FormValues {
  functionCode?: string;
  functionName: string;
  icon?: string;
  sortOrder: number;
  enabled: boolean;
  isDefault: boolean;
  defaultServiceCode?: string;
  allowManualModelSelect: boolean;
  showInMainBar: boolean;
  showInMoreMenu: boolean;
  description?: string;
}

const pageSizeOptions = ['10', '20', '50'];

export const FunctionConfigPage = () => {
  const queryClient = useQueryClient();
  const { serviceOptions, serviceNameMap } = useDictionaries();
  const [filterForm] = Form.useForm<FilterValues>();
  const [editForm] = Form.useForm<FormValues>();
  const [filters, setFilters] = useState<FilterValues>({ enabled: 'all' });
  const [pagination, setPagination] = useState({ pageNum: 1, pageSize: 10 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<FunctionConfig | null>(null);
  const [togglingCode, setTogglingCode] = useState<string>();

  const query = useQuery({
    queryKey: ['function-configs', filters, pagination],
    queryFn: () =>
      fetchFunctionConfigs({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        keyword: sanitizeOptionalString(filters.keyword),
        enabled: parseEnabledFilter(filters.enabled),
      }),
  });

  const invalidate = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['function-configs'] }),
      queryClient.invalidateQueries({ queryKey: ['dictionary', 'functions'] }),
      queryClient.invalidateQueries({ queryKey: ['dictionary', 'services'] }),
    ]);
  };

  const saveMutation = useMutation({
    mutationFn: (payload: FunctionConfigPayload) =>
      editingRecord ? updateFunctionConfig(editingRecord.functionCode, payload) : createFunctionConfig(payload),
    onSuccess: async (result) => {
      message.success(result.message);
      setModalOpen(false);
      setEditingRecord(null);
      editForm.resetFields();
      await invalidate();
    },
    onError: (error) => applyServerFieldErrors(editForm, error),
  });

  function openModal(record?: FunctionConfig) {
    setEditingRecord(record ?? null);
    setModalOpen(true);
    editForm.setFieldsValue(
      record
        ? {
            ...record,
            defaultServiceCode: record.defaultServiceCode ?? undefined,
            description: record.description ?? undefined,
            icon: record.icon ?? undefined,
          }
        : {
            sortOrder: 0,
            enabled: true,
            isDefault: false,
            allowManualModelSelect: true,
            showInMainBar: true,
            showInMoreMenu: false,
          },
    );
  }

  function closeModal() {
    setModalOpen(false);
    setEditingRecord(null);
    editForm.resetFields();
  }

  async function handleToggle(functionCode: string, enabled: boolean) {
    try {
      setTogglingCode(functionCode);
      const result = await toggleFunctionConfig(functionCode, enabled);
      message.success(result.message);
      await invalidate();
    } catch (error) {
      announceError(error);
    } finally {
      setTogglingCode(undefined);
    }
  }

  const columns: ColumnsType<FunctionConfig> = [
    {
      title: '功能标识',
      dataIndex: 'functionCode',
      width: 180,
      render: (value: string) => <span className="table-code">{value}</span>,
    },
    {
      title: '功能名称',
      dataIndex: 'functionName',
      width: 180,
      render: (value: string, record) => (
        <div>
          <strong>{value}</strong>
          <div className="table-subtext">{compactText(record.icon, '未设置图标')}</div>
        </div>
      ),
    },
    {
      title: '展示策略',
      key: 'display',
      width: 220,
      render: (_, record) => (
        <Space wrap>
          {record.showInMainBar ? <Tag color="processing">主入口</Tag> : null}
          {record.showInMoreMenu ? <Tag>更多菜单</Tag> : null}
          {record.allowManualModelSelect ? <Tag color="purple">前台可切模型</Tag> : null}
          {record.isDefault ? <Tag color="gold">默认能力</Tag> : null}
        </Space>
      ),
    },
    {
      title: '默认服务',
      dataIndex: 'defaultServiceCode',
      width: 200,
      render: (value: string | null) =>
        value ? (
          <div>
            <span className="table-code">{value}</span>
            <div className="table-subtext">{serviceNameMap[value] ?? '未命名服务'}</div>
          </div>
        ) : (
          '--'
        ),
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      width: 88,
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      width: 128,
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
      fixed: 'right',
      width: 170,
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => openModal(record)}>
            编辑
          </Button>
          <Switch
            checked={record.enabled}
            loading={togglingCode === record.functionCode}
            onChange={(checked) => handleToggle(record.functionCode, checked)}
          />
        </Space>
      ),
    },
  ];

  const submitFilters = () => {
    const values = filterForm.getFieldsValue();
    setPagination((prev) => ({ ...prev, pageNum: 1 }));
    setFilters(values);
  };

  const resetFilters = () => {
    filterForm.setFieldsValue({ keyword: undefined, enabled: 'all' });
    setPagination((prev) => ({ ...prev, pageNum: 1 }));
    setFilters({ enabled: 'all' });
  };

  const handleSubmit = async () => {
    const values = await editForm.validateFields();
    const payload: FunctionConfigPayload = {
      ...values,
      functionCode: editingRecord ? undefined : values.functionCode?.trim(),
      functionName: values.functionName.trim(),
      icon: sanitizeOptionalString(values.icon),
      defaultServiceCode: sanitizeOptionalString(values.defaultServiceCode),
      description: sanitizeOptionalString(values.description),
    };
    saveMutation.mutate(payload);
  };

  return (
    <div className="page-stack">
      <PageIntro
        eyebrow="Configuration / Functions"
        title="功能配置页"
        description="用最小依赖先打样第一张页面：支持关键字查询、启停切换、新增与编辑，承担后续模型/模板页面的字段范式。"
        stats={[
          { label: '当前记录', value: query.data?.total ?? '--', hint: '按 sortOrder 升序展示' },
          { label: '联调重点', value: 'keyword + enabled', hint: '统一对齐后端分页结构' },
        ]}
        actions={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新增功能
          </Button>
        }
      />

      <Card className="surface-card">
        <Form form={filterForm} layout="inline" initialValues={filters} className="filter-toolbar">
          <Form.Item name="keyword" className="filter-toolbar__grow">
            <Input allowClear placeholder="搜索 functionCode / functionName" onPressEnter={submitFilters} />
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
          rowKey="functionCode"
          loading={query.isLoading}
          columns={columns}
          dataSource={query.data?.records ?? []}
          className="data-table"
          scroll={{ x: 1260 }}
          pagination={{
            current: pagination.pageNum,
            pageSize: pagination.pageSize,
            total: query.data?.total ?? 0,
            showSizeChanger: true,
            pageSizeOptions,
            onChange: (pageNum, pageSize) => setPagination({ pageNum, pageSize }),
            showTotal: (total) => `共 ${total} 条`,
          }}
        />
      </Card>

      <Modal
        title={editingRecord ? '编辑功能配置' : '新增功能配置'}
        open={modalOpen}
        width={760}
        destroyOnClose
        onCancel={closeModal}
        onOk={handleSubmit}
        confirmLoading={saveMutation.isPending}
      >
        <Form form={editForm} layout="vertical">
          <div className="form-grid form-grid--two">
            <Form.Item
              label="功能编码"
              name="functionCode"
              rules={[{ required: !editingRecord, message: '请输入功能编码' }]}
            >
              <Input disabled={Boolean(editingRecord)} placeholder="image_generation" />
            </Form.Item>
            <Form.Item label="功能名称" name="functionName" rules={[{ required: true, message: '请输入功能名称' }]}>
              <Input placeholder="Image Generation" />
            </Form.Item>
            <Form.Item label="图标标识" name="icon">
              <Input placeholder="image-generation" />
            </Form.Item>
            <Form.Item label="排序" name="sortOrder">
              <Input type="number" />
            </Form.Item>
            <Form.Item label="默认服务编码" name="defaultServiceCode">
              <Select allowClear showSearch placeholder="选择默认服务" optionFilterProp="label" options={serviceOptions} />
            </Form.Item>
            <Form.Item label="描述" name="description">
              <Input placeholder="说明该功能的场景与入口位置" />
            </Form.Item>
          </div>

          <div className="toggle-grid">
            <Form.Item label="启用状态" name="enabled" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="默认功能" name="isDefault" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="前台允许手选模型" name="allowManualModelSelect" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="显示在主入口" name="showInMainBar" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="显示在更多菜单" name="showInMoreMenu" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
};

import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, Modal, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { createParamTemplate, fetchParamTemplates, toggleParamTemplate, updateParamTemplate } from '../../api/config';
import { JsonPreview } from '../../components/json-preview';
import { PageIntro } from '../../components/page-intro';
import { StatusBadge } from '../../components/status-badge';
import { useDictionaries } from '../../hooks/use-dictionaries';
import type { ParamTemplate, ParamTemplatePayload } from '../../types/api';
import { compactText, formatDateTime, formatJsonForEditor, normalizeJsonString } from '../../utils/format';
import {
  announceError,
  applyServerFieldErrors,
  enabledFilterOptions,
  jsonStringRule,
  parseEnabledFilter,
  sanitizeOptionalString,
} from '../../utils/forms';

interface FilterValues {
  functionCode?: string;
  serviceCode?: string;
  templateType?: string;
  enabled?: 'all' | 'enabled' | 'disabled';
}

interface FormValues {
  templateCode?: string;
  templateName: string;
  functionCode: string;
  serviceCode?: string;
  templateType: string;
  templatePayload: string;
  presetParams: string;
  description?: string;
  sortOrder: number;
  enabled: boolean;
  isDefault: boolean;
}

const templateTypeOptions = ['default', 'advanced', 'mobile', 'studio'];

export const ParamTemplatePage = () => {
  const queryClient = useQueryClient();
  const { functionOptions, serviceOptions, functionNameMap, serviceNameMap } = useDictionaries();
  const [filterForm] = Form.useForm<FilterValues>();
  const [editForm] = Form.useForm<FormValues>();
  const [filters, setFilters] = useState<FilterValues>({ enabled: 'all' });
  const [pagination, setPagination] = useState({ pageNum: 1, pageSize: 10 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ParamTemplate | null>(null);
  const [togglingCode, setTogglingCode] = useState<string>();

  const query = useQuery({
    queryKey: ['param-templates', filters, pagination],
    queryFn: () =>
      fetchParamTemplates({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        functionCode: sanitizeOptionalString(filters.functionCode),
        serviceCode: sanitizeOptionalString(filters.serviceCode),
        templateType: sanitizeOptionalString(filters.templateType),
        enabled: parseEnabledFilter(filters.enabled),
      }),
  });

  const invalidate = async () => {
    await queryClient.invalidateQueries({ queryKey: ['param-templates'] });
  };

  const saveMutation = useMutation({
    mutationFn: (payload: ParamTemplatePayload) =>
      editingRecord ? updateParamTemplate(editingRecord.templateCode, payload) : createParamTemplate(payload),
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
    if (!watchedFunctionCode) {
      return serviceOptions;
    }
    return serviceOptions.filter((item) => item.meta === watchedFunctionCode);
  }, [serviceOptions, watchedFunctionCode]);

  function openModal(record?: ParamTemplate) {
    setEditingRecord(record ?? null);
    setModalOpen(true);
    editForm.setFieldsValue(
      record
        ? {
            ...record,
            serviceCode: record.serviceCode ?? undefined,
            templateType: record.templateType ?? 'default',
            description: record.description ?? undefined,
            templatePayload: formatJsonForEditor(record.templatePayload, '{\n  "fields": []\n}'),
            presetParams: formatJsonForEditor(record.presetParams),
          }
        : {
            templateType: 'default',
            templatePayload: '{\n  "fields": ["prompt"]\n}',
            presetParams: '{\n  "ratio": "1:1"\n}',
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

  async function handleToggle(templateCode: string, enabled: boolean) {
    try {
      setTogglingCode(templateCode);
      const result = await toggleParamTemplate(templateCode, enabled);
      message.success(result.message);
      await invalidate();
    } catch (error) {
      announceError(error);
    } finally {
      setTogglingCode(undefined);
    }
  }

  const columns: ColumnsType<ParamTemplate> = [
    {
      title: '模板编码',
      dataIndex: 'templateCode',
      width: 180,
      render: (value: string) => <span className="table-code">{value}</span>,
    },
    {
      title: '模板名称',
      dataIndex: 'templateName',
      width: 220,
      render: (value: string, record) => (
        <div>
          <strong>{value}</strong>
          <div className="table-subtext">{record.templateType || 'default'}</div>
        </div>
      ),
    },
    {
      title: '归属',
      key: 'relation',
      width: 220,
      render: (_, record) => (
        <div>
          <div className="table-subtext">{functionNameMap[record.functionCode] ?? record.functionCode}</div>
          <span className="table-code">{record.serviceCode || '无固定服务'}</span>
          {record.serviceCode ? <div className="table-subtext">{serviceNameMap[record.serviceCode] ?? '未命名服务'}</div> : null}
        </div>
      ),
    },
    {
      title: '模板载荷',
      dataIndex: 'templatePayload',
      width: 280,
      render: (value: string) => <JsonPreview value={value} />,
    },
    {
      title: '预设参数',
      dataIndex: 'presetParams',
      width: 280,
      render: (value: string) => <JsonPreview value={value} />,
    },
    {
      title: '策略',
      key: 'strategy',
      width: 180,
      render: (_, record) => (
        <Space wrap>
          {record.isDefault ? <Tag color="gold">默认模板</Tag> : null}
          <Tag color="processing">排序 {record.sortOrder}</Tag>
          <Tag>{compactText(record.description, '无说明')}</Tag>
        </Space>
      ),
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
      fixed: 'right',
      width: 170,
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => openModal(record)}>
            编辑
          </Button>
          <Switch
            checked={record.enabled}
            loading={togglingCode === record.templateCode}
            onChange={(checked) => handleToggle(record.templateCode, checked)}
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
    filterForm.setFieldsValue({ functionCode: undefined, serviceCode: undefined, templateType: undefined, enabled: 'all' });
    setPagination((prev) => ({ ...prev, pageNum: 1 }));
    setFilters({ enabled: 'all' });
  };

  const handleSubmit = async () => {
    const values = await editForm.validateFields();
    const payload: ParamTemplatePayload = {
      ...values,
      templateCode: editingRecord ? undefined : values.templateCode?.trim(),
      templateName: values.templateName.trim(),
      functionCode: values.functionCode.trim(),
      serviceCode: sanitizeOptionalString(values.serviceCode),
      templateType: sanitizeOptionalString(values.templateType),
      templatePayload: normalizeJsonString(values.templatePayload),
      presetParams: normalizeJsonString(values.presetParams),
      description: sanitizeOptionalString(values.description),
    };
    saveMutation.mutate(payload);
  };

  return (
    <div className="page-stack">
      <PageIntro
        eyebrow="Configuration / Param Templates"
        title="参数模板页"
        description="收口配置中心第四张页面：支持按功能、服务与模板类型筛选，并让 templatePayload / presetParams 直接对接后端 jsonb 字段。"
        stats={[
          { label: '当前记录', value: query.data?.total ?? '--', hint: '支持 templateType 精准筛选' },
          { label: '重点字段', value: '2 个 JSON', hint: '前端校验合法性，后端按字符串入库' },
        ]}
        actions={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新增模板
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
          <Form.Item name="templateType">
            <Select allowClear placeholder="模板类型" style={{ width: 160 }} options={templateTypeOptions.map((value) => ({ label: value, value }))} />
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
          rowKey="templateCode"
          loading={query.isLoading}
          columns={columns}
          dataSource={query.data?.records ?? []}
          className="data-table"
          scroll={{ x: 1720 }}
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
        title={editingRecord ? '编辑参数模板' : '新增参数模板'}
        open={modalOpen}
        width={900}
        destroyOnClose
        onCancel={closeModal}
        onOk={handleSubmit}
        confirmLoading={saveMutation.isPending}
      >
        <Form form={editForm} layout="vertical">
          <div className="form-grid form-grid--two">
            <Form.Item label="模板编码" name="templateCode" rules={[{ required: !editingRecord, message: '请输入模板编码' }]}>
              <Input disabled={Boolean(editingRecord)} placeholder="tpl-img-gen-basic" />
            </Form.Item>
            <Form.Item label="模板名称" name="templateName" rules={[{ required: true, message: '请输入模板名称' }]}>
              <Input placeholder="Image Generation Basic Template" />
            </Form.Item>
            <Form.Item label="功能归属" name="functionCode" rules={[{ required: true, message: '请选择功能归属' }]}>
              <Select showSearch optionFilterProp="label" options={functionOptions} />
            </Form.Item>
            <Form.Item label="服务归属" name="serviceCode">
              <Select allowClear showSearch optionFilterProp="label" options={filteredServiceOptions} />
            </Form.Item>
            <Form.Item label="模板类型" name="templateType" rules={[{ required: true, message: '请选择模板类型' }]}>
              <Select options={templateTypeOptions.map((value) => ({ label: value, value }))} />
            </Form.Item>
            <Form.Item label="排序" name="sortOrder">
              <Input type="number" min={0} />
            </Form.Item>
            <Form.Item label="模板说明" name="description" className="form-grid__full">
              <Input placeholder="说明该模板面向的场景与默认参数" />
            </Form.Item>
            <Form.Item
              label="模板载荷(JSON)"
              name="templatePayload"
              className="form-grid__full"
              rules={[jsonStringRule('模板载荷')]}
            >
              <Input.TextArea rows={8} placeholder='{"fields":["prompt","ratio"]}' />
            </Form.Item>
            <Form.Item
              label="预设参数(JSON)"
              name="presetParams"
              className="form-grid__full"
              rules={[jsonStringRule('预设参数')]}
            >
              <Input.TextArea rows={8} placeholder='{"ratio":"1:1"}' />
            </Form.Item>
          </div>

          <div className="toggle-grid">
            <Form.Item label="启用状态" name="enabled" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="默认模板" name="isDefault" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
};

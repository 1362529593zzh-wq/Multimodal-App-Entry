import { PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Button, Card, Form, Input, Modal, Select, Space, Switch, Table, Tag, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useState } from 'react';
import { createModelService, fetchModelServices, toggleModelService, updateModelService } from '../../api/config';
import { JsonPreview } from '../../components/json-preview';
import { PageIntro } from '../../components/page-intro';
import { StatusBadge } from '../../components/status-badge';
import { useDictionaries } from '../../hooks/use-dictionaries';
import type { ModelService, ModelServicePayload } from '../../types/api';
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
  keyword?: string;
  functionCode?: string;
  enabled?: 'all' | 'enabled' | 'disabled';
}

interface FormValues {
  serviceCode?: string;
  serviceName: string;
  modelCode: string;
  modelName: string;
  modelType: string;
  functionCode: string;
  endpoint?: string;
  authType?: string;
  timeoutMs: number;
  enabled: boolean;
  publishStatus: string;
  isDefault: boolean;
  allowFrontSelect: boolean;
  supportedOptions: string;
  remark?: string;
}

const authTypeOptions = ['none', 'bearer', 'api_key', 'basic'];
const publishStatusOptions = ['PUBLISHED', 'DRAFT', 'OFFLINE'];
const modelTypeOptions = ['image', 'vision', 'audio', 'ppt', 'video', 'text'];

export const ModelServicePage = () => {
  const queryClient = useQueryClient();
  const { functionOptions, functionNameMap } = useDictionaries();
  const [filterForm] = Form.useForm<FilterValues>();
  const [editForm] = Form.useForm<FormValues>();
  const [filters, setFilters] = useState<FilterValues>({ enabled: 'all' });
  const [pagination, setPagination] = useState({ pageNum: 1, pageSize: 10 });
  const [modalOpen, setModalOpen] = useState(false);
  const [editingRecord, setEditingRecord] = useState<ModelService | null>(null);
  const [togglingCode, setTogglingCode] = useState<string>();

  const query = useQuery({
    queryKey: ['model-services', filters, pagination],
    queryFn: () =>
      fetchModelServices({
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        keyword: sanitizeOptionalString(filters.keyword),
        functionCode: sanitizeOptionalString(filters.functionCode),
        enabled: parseEnabledFilter(filters.enabled),
      }),
  });

  const invalidate = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['model-services'] }),
      queryClient.invalidateQueries({ queryKey: ['dictionary', 'services'] }),
    ]);
  };

  const saveMutation = useMutation({
    mutationFn: (payload: ModelServicePayload) =>
      editingRecord ? updateModelService(editingRecord.serviceCode, payload) : createModelService(payload),
    onSuccess: async (result) => {
      message.success(result.message);
      setModalOpen(false);
      setEditingRecord(null);
      editForm.resetFields();
      await invalidate();
    },
    onError: (error) => applyServerFieldErrors(editForm, error),
  });

  function openModal(record?: ModelService) {
    setEditingRecord(record ?? null);
    setModalOpen(true);
    editForm.setFieldsValue(
      record
        ? {
            ...record,
            endpoint: record.endpoint ?? undefined,
            authType: record.authType ?? undefined,
            remark: record.remark ?? undefined,
            supportedOptions: formatJsonForEditor(record.supportedOptions),
          }
        : {
            timeoutMs: 30000,
            enabled: true,
            publishStatus: 'PUBLISHED',
            isDefault: false,
            allowFrontSelect: true,
            supportedOptions: '{\n  "ratio": ["1:1"]\n}',
            authType: 'none',
          },
    );
  }

  function closeModal() {
    setModalOpen(false);
    setEditingRecord(null);
    editForm.resetFields();
  }

  async function handleToggle(serviceCode: string, enabled: boolean) {
    try {
      setTogglingCode(serviceCode);
      const result = await toggleModelService(serviceCode, enabled);
      message.success(result.message);
      await invalidate();
    } catch (error) {
      announceError(error);
    } finally {
      setTogglingCode(undefined);
    }
  }

  const columns: ColumnsType<ModelService> = [
    {
      title: '服务编码',
      dataIndex: 'serviceCode',
      width: 180,
      render: (value: string) => <span className="table-code">{value}</span>,
    },
    {
      title: '服务 / 模型',
      key: 'service',
      width: 240,
      render: (_, record) => (
        <div>
          <strong>{record.serviceName}</strong>
          <div className="table-subtext">
            {record.modelName} · {record.modelCode}
          </div>
        </div>
      ),
    },
    {
      title: '能力归属',
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
      title: '发布信息',
      key: 'publish',
      width: 200,
      render: (_, record) => (
        <Space wrap>
          <Tag color="processing">{record.publishStatus}</Tag>
          <Tag>{record.authType || 'none'}</Tag>
          {record.isDefault ? <Tag color="gold">默认服务</Tag> : null}
          {record.allowFrontSelect ? <Tag color="purple">前台可选</Tag> : null}
        </Space>
      ),
    },
    {
      title: '超时 / 端点',
      key: 'runtime',
      width: 220,
      render: (_, record) => (
        <div>
          <strong>{record.timeoutMs} ms</strong>
          <div className="table-subtext table-subtext--clamp">{compactText(record.endpoint)}</div>
        </div>
      ),
    },
    {
      title: '支持选项',
      dataIndex: 'supportedOptions',
      width: 280,
      render: (value: string) => <JsonPreview value={value} />,
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
            loading={togglingCode === record.serviceCode}
            onChange={(checked) => handleToggle(record.serviceCode, checked)}
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
    filterForm.setFieldsValue({ keyword: undefined, functionCode: undefined, enabled: 'all' });
    setPagination((prev) => ({ ...prev, pageNum: 1 }));
    setFilters({ enabled: 'all' });
  };

  const handleSubmit = async () => {
    const values = await editForm.validateFields();
    const payload: ModelServicePayload = {
      ...values,
      serviceCode: editingRecord ? undefined : values.serviceCode?.trim(),
      serviceName: values.serviceName.trim(),
      modelCode: values.modelCode.trim(),
      modelName: values.modelName.trim(),
      modelType: values.modelType.trim(),
      functionCode: values.functionCode.trim(),
      endpoint: sanitizeOptionalString(values.endpoint),
      authType: sanitizeOptionalString(values.authType),
      publishStatus: values.publishStatus.trim(),
      supportedOptions: normalizeJsonString(values.supportedOptions),
      remark: sanitizeOptionalString(values.remark),
    };
    saveMutation.mutate(payload);
  };

  return (
    <div className="page-stack">
      <PageIntro
        eyebrow="Configuration / Model Services"
        title="模型服务页"
        description="承接功能配置后的第二层能力：按功能筛选模型服务，联调 JSON 字段、发布状态与模型可选策略。"
        stats={[
          { label: '当前记录', value: query.data?.total ?? '--', hint: '支持 functionCode 筛选' },
          { label: 'JSON 字段', value: 'supportedOptions', hint: '直接按字符串提交给后端 jsonb' },
        ]}
        actions={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
            新增服务
          </Button>
        }
      />

      <Card className="surface-card">
        <Form form={filterForm} layout="inline" initialValues={filters} className="filter-toolbar">
          <Form.Item name="keyword" className="filter-toolbar__grow">
            <Input allowClear placeholder="搜索 serviceCode / serviceName / modelName" onPressEnter={submitFilters} />
          </Form.Item>
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
          rowKey="serviceCode"
          loading={query.isLoading}
          columns={columns}
          dataSource={query.data?.records ?? []}
          className="data-table"
          scroll={{ x: 1620 }}
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
        title={editingRecord ? '编辑模型服务' : '新增模型服务'}
        open={modalOpen}
        width={860}
        destroyOnClose
        onCancel={closeModal}
        onOk={handleSubmit}
        confirmLoading={saveMutation.isPending}
      >
        <Form form={editForm} layout="vertical">
          <div className="form-grid form-grid--two">
            <Form.Item label="服务编码" name="serviceCode" rules={[{ required: !editingRecord, message: '请输入服务编码' }]}>
              <Input disabled={Boolean(editingRecord)} placeholder="img-gen-default" />
            </Form.Item>
            <Form.Item label="服务名称" name="serviceName" rules={[{ required: true, message: '请输入服务名称' }]}>
              <Input placeholder="Default Image Generation Service" />
            </Form.Item>
            <Form.Item label="模型编码" name="modelCode" rules={[{ required: true, message: '请输入模型编码' }]}>
              <Input placeholder="sdxl-default" />
            </Form.Item>
            <Form.Item label="模型名称" name="modelName" rules={[{ required: true, message: '请输入模型名称' }]}>
              <Input placeholder="SDXL Default" />
            </Form.Item>
            <Form.Item label="模型类型" name="modelType" rules={[{ required: true, message: '请选择模型类型' }]}>
              <Select showSearch placeholder="选择模型类型" options={modelTypeOptions.map((value) => ({ label: value, value }))} />
            </Form.Item>
            <Form.Item label="功能归属" name="functionCode" rules={[{ required: true, message: '请选择功能归属' }]}>
              <Select showSearch optionFilterProp="label" options={functionOptions} />
            </Form.Item>
            <Form.Item label="请求端点" name="endpoint">
              <Input placeholder="http://mock-api/image-generation" />
            </Form.Item>
            <Form.Item label="鉴权方式" name="authType">
              <Select allowClear options={authTypeOptions.map((value) => ({ label: value, value }))} />
            </Form.Item>
            <Form.Item label="超时时间(ms)" name="timeoutMs">
              <Input type="number" min={1000} />
            </Form.Item>
            <Form.Item label="发布状态" name="publishStatus" rules={[{ required: true, message: '请选择发布状态' }]}>
              <Select options={publishStatusOptions.map((value) => ({ label: value, value }))} />
            </Form.Item>
            <Form.Item label="备注" name="remark" className="form-grid__full">
              <Input placeholder="例如：默认图片生成模型" />
            </Form.Item>
            <Form.Item
              label="支持选项(JSON)"
              name="supportedOptions"
              className="form-grid__full"
              rules={[jsonStringRule('支持选项')]}
            >
              <Input.TextArea rows={8} placeholder='{"ratio":["1:1","16:9"]}' />
            </Form.Item>
          </div>

          <div className="toggle-grid">
            <Form.Item label="启用状态" name="enabled" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="默认服务" name="isDefault" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="前台允许选择" name="allowFrontSelect" valuePropName="checked">
              <Switch />
            </Form.Item>
          </div>
        </Form>
      </Modal>
    </div>
  );
};

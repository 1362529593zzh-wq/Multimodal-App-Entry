import {
  BarChartOutlined,
  CloudDownloadOutlined,
  ExclamationCircleOutlined,
  FileDoneOutlined,
  ReloadOutlined,
  SearchOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons';
import { useQuery } from '@tanstack/react-query';
import { Button, Card, Descriptions, Empty, Input, Modal, Select, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useMemo, useState } from 'react';
import { exportCallRecordsUrl, fetchCallRecordDetail, fetchCallRecords } from '../../api/call-records';
import type { CallRecord } from '../../types/api';
import { formatDateTime } from '../../utils/format';
import { sanitizeOptionalString } from '../../utils/forms';

const statusOptions = [
  { label: '成功', value: 'SUCCESS' },
  { label: '处理中', value: 'RUNNING' },
  { label: '失败', value: 'FAILED' },
  { label: '待处理', value: 'PENDING' },
];

const functionOptions = [
  { label: '图像生成', value: 'image_generation' },
  { label: '图像识别', value: 'image_recognition' },
  { label: '文生视频', value: 'video_generation' },
  { label: '文生语音', value: 'speech_generation' },
  { label: '文生 PPT', value: 'ppt_generation' },
  { label: '文本对话', value: 'chat' },
];

function statusMeta(status?: string) {
  const normalized = (status || '').toUpperCase();
  if (['SUCCESS', 'SUCCEEDED', 'COMPLETED', 'DONE'].includes(normalized)) return { color: 'success', text: '成功', className: 'success' };
  if (['RUNNING', 'PROCESSING', 'IN_PROGRESS'].includes(normalized)) return { color: 'processing', text: '处理中', className: 'running' };
  if (['FAILED', 'FAIL', 'ERROR'].includes(normalized)) return { color: 'error', text: '失败', className: 'fail' };
  return { color: 'default', text: status || '未知', className: 'pending' };
}

function formatDuration(durationMs?: number | null) {
  if (!durationMs) return '-';
  if (durationMs < 1000) return `${durationMs}ms`;
  return `${(durationMs / 1000).toFixed(durationMs >= 10000 ? 0 : 1)}s`;
}

function abilityLabel(record: CallRecord) {
  return record.functionName || functionOptions.find((item) => item.value === record.functionCode)?.label || record.functionCode || '未知能力';
}

export const CallRecordsPage = () => {
  const [keyword, setKeyword] = useState('');
  const [functionCode, setFunctionCode] = useState<string>();
  const [status, setStatus] = useState<string>();
  const [resultType, setResultType] = useState<string>();
  const [selectedId, setSelectedId] = useState<string>();

  const query = useQuery({
    queryKey: ['call-records', keyword, functionCode, status, resultType],
    queryFn: () =>
      fetchCallRecords({
        pageNum: 1,
        pageSize: 100,
        keyword: sanitizeOptionalString(keyword),
        functionCode,
        status,
        resultType,
      }),
  });

  const detailQuery = useQuery({
    queryKey: ['call-record-detail', selectedId],
    queryFn: () => fetchCallRecordDetail(selectedId || ''),
    enabled: Boolean(selectedId),
  });

  const records = useMemo(() => query.data?.records ?? [], [query.data?.records]);

  const stats = useMemo(() => {
    const success = records.filter((item) => statusMeta(item.status).className === 'success').length;
    const failed = records.filter((item) => statusMeta(item.status).className === 'fail').length;
    const finished = records.filter((item) => item.durationMs).map((item) => item.durationMs || 0);
    const avg = finished.length ? finished.reduce((sum, item) => sum + item, 0) / finished.length : 0;
    const files = records.filter((item) => item.resultType && item.resultType !== 'text').length;
    return {
      total: query.data?.total ?? records.length,
      successRate: records.length ? `${((success / records.length) * 100).toFixed(1)}%` : '-',
      avgDuration: avg ? formatDuration(avg) : '-',
      failed,
      files,
    };
  }, [query.data?.total, records]);

  const columns: ColumnsType<CallRecord> = [
    {
      title: '任务',
      dataIndex: 'taskId',
      width: 210,
      render: (_, record) => (
        <div className="record-task-cell">
          <strong>{record.taskId || record.recordId}</strong>
          <span>{(record.requestSummary || record.inputText || record.resolvedIntent || '暂无摘要').slice(0, 32)}</span>
        </div>
      ),
    },
    {
      title: '能力',
      dataIndex: 'functionName',
      render: (_, record) => <span className="ability-cell"><i />{abilityLabel(record)}</span>,
    },
    {
      title: '模型服务',
      dataIndex: 'serviceName',
      render: (_, record) => record.serviceName || record.modelName || record.serviceCode || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (value) => {
        const meta = statusMeta(value);
        return <Tag color={meta.color}>{meta.text}</Tag>;
      },
    },
    {
      title: '耗时',
      dataIndex: 'durationMs',
      width: 100,
      render: formatDuration,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      width: 160,
      render: formatDateTime,
    },
    {
      title: '',
      width: 88,
      render: (_, record) => <Button size="small" shape="round" onClick={() => setSelectedId(record.recordId)}>详情</Button>,
    },
  ];

  function handleExport() {
    window.location.href = exportCallRecordsUrl({ keyword, functionCode, status, resultType, pageNum: 1, pageSize: 1000 });
  }

  const selectedRecord = detailQuery.data;

  return (
    <div className="admin-page call-records-modern-page">
      <section className="admin-hero">
        <div>
          <span className="admin-eyebrow">Invocation Records</span>
          <h1>调用记录</h1>
          <p>把每一次多模态能力调用沉淀为可检索、可分析、可导出的记录，支持按能力、模型、状态、耗时和结果类型快速筛选。</p>
        </div>
        <Space>
          <Button className="admin-white-button" icon={<ReloadOutlined />} onClick={() => query.refetch()}>刷新</Button>
          <Button className="admin-blue-button" icon={<CloudDownloadOutlined />} onClick={handleExport}>导出 Excel</Button>
        </Space>
      </section>

      <section className="admin-stats-grid five">
        <Card><span>调用总数</span><strong>{stats.total}</strong><ThunderboltOutlined /></Card>
        <Card><span>成功率</span><strong>{stats.successRate}</strong><FileDoneOutlined /></Card>
        <Card><span>平均耗时</span><strong>{stats.avgDuration}</strong><BarChartOutlined /></Card>
        <Card><span>失败次数</span><strong>{stats.failed}</strong><ExclamationCircleOutlined /></Card>
        <Card><span>产物文件</span><strong>{stats.files}</strong><CloudDownloadOutlined /></Card>
      </section>

      <section className="admin-two-column records-layout">
        <main className="admin-panel admin-main-panel">
          <div className="admin-panel-head">
            <div>
              <h2>记录列表</h2>
              <p>保留表格效率，同时减少线框密度，突出状态、耗时和异常。</p>
            </div>
            <div className="admin-view-switch"><span className="active">全部</span><span>成功</span><span>失败</span></div>
          </div>

          <div className="admin-filter-row records-filters">
            <Input prefix={<SearchOutlined />} allowClear placeholder="搜索任务 ID、会话标题、模型名称" value={keyword} onChange={(event) => setKeyword(event.target.value)} />
            <Select allowClear placeholder="能力" value={functionCode} onChange={setFunctionCode} options={functionOptions} />
            <Select allowClear placeholder="状态" value={status} onChange={setStatus} options={statusOptions} />
            <Select allowClear placeholder="结果类型" value={resultType} onChange={setResultType} options={['text', 'image', 'video', 'audio', 'ppt', 'file'].map((value) => ({ label: value, value }))} />
          </div>

          <Card className="admin-table-card">
            <Table
              size="small"
              rowKey="recordId"
              loading={query.isLoading}
              columns={columns}
              dataSource={records}
              scroll={{ x: 920 }}
              pagination={{ pageSize: 8, showSizeChanger: false }}
              locale={{ emptyText: <Empty description="暂无调用记录" /> }}
            />
          </Card>
        </main>

        <aside className="admin-panel records-side-panel">
          <div className="admin-detail-title">
            <div>
              <h2>调用分析</h2>
              <p>辅助排查模型稳定性、耗时和失败集中点。</p>
            </div>
          </div>
          <div className="usage-chart-card">
            <span>Capability Usage</span>
            <div className="usage-bars">
              <i style={{ height: '88%' }} />
              <i style={{ height: '62%' }} />
              <i style={{ height: '48%' }} />
              <i style={{ height: '38%' }} />
              <i style={{ height: '28%' }} />
            </div>
          </div>
          <Card className="insight-card">
            <h3>异常提醒</h3>
            <p>失败记录会优先显示在这里，建议检查密钥、服务状态、超时时间和模型路由配置。</p>
            <Space wrap><Tag>查看失败</Tag><Tag>重试任务</Tag><Tag>模型测试</Tag></Space>
          </Card>
          <Card className="insight-card">
            <h3>最常用能力</h3>
            <p>{records[0] ? `${abilityLabel(records[0])} 最近调用活跃，可结合耗时和成功率继续优化。` : '暂无记录，完成调用后会自动统计。'}</p>
          </Card>
        </aside>
      </section>

      <Modal title="调用详情" open={Boolean(selectedId)} width={820} onCancel={() => setSelectedId(undefined)} footer={<Button onClick={() => setSelectedId(undefined)}>关闭</Button>}>
        {selectedRecord ? (
          <Descriptions bordered column={2} size="small">
            <Descriptions.Item label="记录 ID">{selectedRecord.recordId}</Descriptions.Item>
            <Descriptions.Item label="任务 ID">{selectedRecord.taskId}</Descriptions.Item>
            <Descriptions.Item label="能力">{abilityLabel(selectedRecord)}</Descriptions.Item>
            <Descriptions.Item label="模型服务">{selectedRecord.serviceName || selectedRecord.serviceCode}</Descriptions.Item>
            <Descriptions.Item label="状态"><Tag color={statusMeta(selectedRecord.status).color}>{statusMeta(selectedRecord.status).text}</Tag></Descriptions.Item>
            <Descriptions.Item label="耗时">{formatDuration(selectedRecord.durationMs)}</Descriptions.Item>
            <Descriptions.Item label="输入摘要" span={2}>{selectedRecord.requestSummary || selectedRecord.inputText || '-'}</Descriptions.Item>
            <Descriptions.Item label="结果摘要" span={2}>{selectedRecord.resultSummary || '-'}</Descriptions.Item>
            <Descriptions.Item label="错误信息" span={2}>{selectedRecord.errorMessage || '-'}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatDateTime(selectedRecord.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="完成时间">{formatDateTime(selectedRecord.finishedAt)}</Descriptions.Item>
          </Descriptions>
        ) : (
          <Empty description={detailQuery.isLoading ? '正在加载详情' : '暂无详情'} />
        )}
      </Modal>
    </div>
  );
};



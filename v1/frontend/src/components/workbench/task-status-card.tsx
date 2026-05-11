import { CheckCircleOutlined, ClockCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons';
import { Card, Progress, Space, Tag } from 'antd';
import type { WorkbenchTaskCardData } from '../../pages/workbench/types';

interface TaskStatusCardProps {
  task: WorkbenchTaskCardData;
}

const statusMeta = {
  queued: {
    color: 'default',
    icon: <ClockCircleOutlined />,
    percent: 20,
    strokeColor: '#c27b1c',
  },
  running: {
    color: 'processing',
    icon: <LoadingOutlined />,
    percent: 62,
    strokeColor: '#0f766e',
  },
  succeeded: {
    color: 'success',
    icon: <CheckCircleOutlined />,
    percent: 100,
    strokeColor: '#3f8f5d',
  },
  failed: {
    color: 'error',
    icon: <CloseCircleOutlined />,
    percent: 100,
    strokeColor: '#b8493a',
  },
} as const;

export const TaskStatusCard = ({ task }: TaskStatusCardProps) => {
  const meta = statusMeta[task.status];
  return (
    <Card className="message-card message-card--task">
      <Space className="message-card__header" align="center">
        <Tag color={meta.color} icon={meta.icon}>
          {task.status.toUpperCase()}
        </Tag>
        <span className="table-code">{task.taskId}</span>
      </Space>
      <h4>{task.capabilityName}</h4>
      <p>{task.stageText}</p>
      <Progress percent={meta.percent} showInfo={false} strokeColor={meta.strokeColor} trailColor="rgba(27, 38, 50, 0.08)" />
    </Card>
  );
};

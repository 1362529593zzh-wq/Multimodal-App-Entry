import { CompassOutlined, RocketOutlined } from '@ant-design/icons';
import { Button, Empty, Space, Tag } from 'antd';
import type { WorkbenchFunction } from '../../types/api';

interface EmptyConversationProps {
  capabilities: WorkbenchFunction[];
  onSelect: (capabilityCode: string) => void;
}

export const EmptyConversation = ({ capabilities, onSelect }: EmptyConversationProps) => (
  <div className="empty-conversation">
    <Empty
      image={Empty.PRESENTED_IMAGE_SIMPLE}
      description="工作台骨架已经就绪。先从一个能力开始，验证功能条、参数区和消息流的基础体验。"
    />
    <Space wrap>
      {capabilities.slice(0, 4).map((capability) => (
        <Button key={capability.functionCode} icon={<CompassOutlined />} onClick={() => onSelect(capability.functionCode)}>
          {capability.functionName}
        </Button>
      ))}
      <Button type="primary" ghost icon={<RocketOutlined />} onClick={() => onSelect('image_generation')}>
        从文生图开始
      </Button>
    </Space>
    <div className="empty-conversation__tags">
      <Tag color="cyan">Step 4</Tag>
      <Tag>消息流</Tag>
      <Tag>功能条</Tag>
      <Tag>参数快捷区</Tag>
    </div>
  </div>
);

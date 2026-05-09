import { CompassOutlined } from '@ant-design/icons';
import { Button } from 'antd';
import type { WorkbenchFunction } from '../../types/api';

interface EmptyConversationProps {
  capabilities: WorkbenchFunction[];
  onSelect: (capabilityCode: string) => void;
}

const suggestions = ['生成一张产品海报', '识别图片中的内容', '整理一份 PPT'];

export const EmptyConversation = ({ capabilities, onSelect }: EmptyConversationProps) => (
  <div className="empty-conversation">
    <div className="empty-conversation__copy">
      <h1>从一个任务开始</h1>
      <p>选择下方能力或直接输入需求，工作台会根据内容匹配模型与参数。</p>
      <div className="empty-conversation__suggestions">
        {suggestions.map((item) => (
          <span key={item}>试试：{item}</span>
        ))}
      </div>
    </div>
    <div className="empty-conversation__bubbles">
      <div className="empty-conversation__bubble empty-conversation__bubble--user">
        <strong>你的输入</strong>
        <span>帮我生成一张适合公众号首图的科技海报。</span>
      </div>
      <div className="empty-conversation__bubble empty-conversation__bubble--ai">
        <strong>系统响应</strong>
        <span>已匹配图像生成能力，可继续调整尺寸、风格和模型服务。</span>
      </div>
    </div>
    <div className="empty-conversation__capabilities">
      {capabilities.slice(0, 5).map((capability) => (
        <Button key={capability.functionCode} icon={<CompassOutlined />} onClick={() => onSelect(capability.functionCode)}>
          {capability.functionName}
        </Button>
      ))}
    </div>
  </div>
);

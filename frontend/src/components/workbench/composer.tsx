import { ClearOutlined, SendOutlined } from '@ant-design/icons';
import { Alert, Button, Card, Input, Space } from 'antd';
import type { WorkbenchFunction } from '../../types/api';
import type { WorkbenchBlueprint, WorkbenchDraftOptions, WorkbenchFollowUpState } from '../../pages/workbench/types';
import { CapabilityBar } from './capability-bar';
import { OptionBar } from './option-bar';

const { TextArea } = Input;

interface ComposerProps {
  capabilities: WorkbenchFunction[];
  selectedCapability: string;
  blueprint: WorkbenchBlueprint;
  draftText: string;
  options: WorkbenchDraftOptions;
  followUp: WorkbenchFollowUpState | null;
  sending: boolean;
  onSelectCapability: (capabilityCode: string) => void;
  onDraftTextChange: (value: string) => void;
  onOptionChange: (key: keyof WorkbenchDraftOptions, value: string | undefined) => void;
  onClearDraft: () => void;
  onClearFollowUp: () => void;
  onSend: () => void;
}

export const Composer = ({
  capabilities,
  selectedCapability,
  blueprint,
  draftText,
  options,
  followUp,
  sending,
  onSelectCapability,
  onDraftTextChange,
  onOptionChange,
  onClearDraft,
  onClearFollowUp,
  onSend,
}: ComposerProps) => (
  <Card className="surface-card composer-panel">
    <CapabilityBar capabilities={capabilities} selectedCapability={selectedCapability} onSelect={onSelectCapability} />
    <OptionBar blueprint={blueprint} draft={options} onChange={onOptionChange} />
    {followUp ? (
      <Alert
        className="composer-panel__follow-up"
        type="info"
        showIcon
        closable
        onClose={onClearFollowUp}
        message="继续追问已启用"
        description={`当前输入将基于结果“${followUp.sourceLabel}”继续生成。`}
      />
    ) : null}
    <div className="composer-panel__input">
      <TextArea
        value={draftText}
        rows={4}
        placeholder={blueprint.placeholder}
        onChange={(event) => onDraftTextChange(event.target.value)}
      />
      <Space className="composer-panel__actions">
        <Button icon={<ClearOutlined />} onClick={onClearDraft}>
          清空草稿
        </Button>
        {followUp ? (
          <Button type="default" onClick={onClearFollowUp}>
            新任务
          </Button>
        ) : null}
        <Button type="primary" icon={<SendOutlined />} loading={sending} onClick={onSend}>
          发送
        </Button>
      </Space>
    </div>
  </Card>
);

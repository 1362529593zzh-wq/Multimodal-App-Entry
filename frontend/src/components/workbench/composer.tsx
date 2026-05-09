import { AudioOutlined, ClearOutlined, SendOutlined } from '@ant-design/icons';
import { Alert, Button, Input, Tooltip } from 'antd';
import { useState } from 'react';
import type { WorkbenchBlueprint, WorkbenchDraftOptions, WorkbenchFollowUpState } from '../../pages/workbench/types';
import type { WorkbenchFunction } from '../../types/api';
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
  onOptionChange: (key: string, value: string | undefined) => void;
  onUploadReference?: (file: File) => Promise<void>;
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
  onUploadReference,
  onClearDraft,
  onClearFollowUp,
  onSend,
}: ComposerProps) => {
  const placeholderMap: Record<string, string> = {
    image_generation: '\u63cf\u8ff0\u4f60\u60f3\u8981\u7684\u56fe\u7247',
    image_recognition: '\u4e0a\u4f20\u56fe\u7247\u5e76\u8f93\u5165\u4f60\u60f3\u5206\u6790\u7684\u95ee\u9898',
    text_to_speech: '\u8f93\u5165\u8981\u5408\u6210\u8bed\u97f3\u7684\u6587\u672c',
    text_to_video: '\u63cf\u8ff0\u4f60\u60f3\u8981\u7684\u89c6\u9891',
    text_to_ppt: '\u8f93\u5165 PPT \u4e3b\u9898\u548c\u63d0\u7eb2',
  };
  const [capabilityPinned, setCapabilityPinned] = useState(false);
  const placeholder = placeholderMap[selectedCapability] ?? blueprint.placeholder;

  return (
    <section className="composer-panel">
      {followUp ? (
        <Alert
          className="composer-panel__follow-up"
          type="info"
          showIcon
          closable
          onClose={onClearFollowUp}
          message="\u7ee7\u7eed\u8ffd\u95ee\u5df2\u542f\u7528"
          description={`\u5f53\u524d\u8f93\u5165\u5c06\u57fa\u4e8e\u7ed3\u679c\u201c${followUp.sourceLabel}\u201d\u7ee7\u7eed\u751f\u6210\u3002`}
        />
      ) : null}
      <div className="composer-panel__box">
        <div className="composer-panel__input-row">
          <TextArea
            value={draftText}
            autoSize={{ minRows: 1, maxRows: 3 }}
            placeholder={placeholder}
            onChange={(event) => onDraftTextChange(event.target.value)}
            onPressEnter={(event) => {
              if (!event.shiftKey) {
                event.preventDefault();
                onSend();
              }
            }}
          />
          <Tooltip title="\u53d1\u9001">
            <Button
              className="composer-panel__send"
              type="primary"
              shape="circle"
              size="large"
              icon={<SendOutlined />}
              loading={sending}
              onClick={onSend}
            />
          </Tooltip>
        </div>
        <div className="composer-panel__toolbar">
          <CapabilityBar
            capabilities={capabilities}
            selectedCapability={selectedCapability}
            pinned={capabilityPinned}
            onSelect={(capabilityCode) => {
              setCapabilityPinned(true);
              onSelectCapability(capabilityCode);
            }}
            onClear={() => setCapabilityPinned(false)}
          />
          <OptionBar
            capabilityCode={selectedCapability}
            blueprint={blueprint}
            draft={options}
            onChange={onOptionChange}
            onUploadReference={onUploadReference}
          />
          <div className="composer-panel__tools">
            <Tooltip title="\u8bed\u97f3\u8f93\u5165">
              <Button type="text" shape="circle" icon={<AudioOutlined />} />
            </Tooltip>
            <Tooltip title="\u6e05\u7a7a">
              <Button type="text" shape="circle" icon={<ClearOutlined />} onClick={onClearDraft} />
            </Tooltip>
          </div>
        </div>
      </div>
    </section>
  );
};

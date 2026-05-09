import { AudioOutlined, CloseOutlined, FilePptOutlined, PictureOutlined, VideoCameraOutlined } from '@ant-design/icons';
import { Button, Tooltip } from 'antd';
import type { ReactNode } from 'react';
import type { WorkbenchFunction } from '../../types/api';

interface CapabilityBarProps {
  capabilities: WorkbenchFunction[];
  selectedCapability: string;
  pinned: boolean;
  onSelect: (capabilityCode: string) => void;
  onClear: () => void;
}

const capabilityOrder = ['image_generation', 'text_to_speech', 'text_to_video', 'text_to_ppt'];

const iconMap: Record<string, ReactNode> = {
  image_generation: <PictureOutlined />,
  text_to_speech: <AudioOutlined />,
  text_to_video: <VideoCameraOutlined />,
  text_to_ppt: <FilePptOutlined />,
};

const labelMap: Record<string, string> = {
  image_generation: '\u56fe\u50cf\u751f\u6210',
  text_to_speech: '\u6587\u751f\u8bed\u97f3',
  text_to_video: '\u89c6\u9891\u751f\u6210',
  text_to_ppt: '\u6587\u751f PPT',
};

export const CapabilityBar = ({ capabilities, selectedCapability, pinned, onSelect, onClear }: CapabilityBarProps) => {
  const orderedCapabilities = capabilityOrder
    .map((code) => capabilities.find((item) => item.functionCode === code))
    .filter((item): item is WorkbenchFunction => Boolean(item));
  const visibleCapabilities = pinned
    ? orderedCapabilities.filter((item) => item.functionCode === selectedCapability)
    : orderedCapabilities;

  return (
    <div className="capability-bar" aria-label="capability selector">
      {visibleCapabilities.map((capability) => {
        const selected = selectedCapability === capability.functionCode;
        const label = labelMap[capability.functionCode] ?? capability.functionName;

        return (
          <Tooltip key={capability.functionCode} title={label}>
            <Button
              type={selected ? 'primary' : 'text'}
              icon={iconMap[capability.functionCode] ?? <PictureOutlined />}
              onClick={() => onSelect(capability.functionCode)}
            >
              {label}
              {pinned && selected ? (
                <CloseOutlined
                  className="capability-bar__close"
                  onClick={(event) => {
                    event.stopPropagation();
                    onClear();
                  }}
                />
              ) : null}
            </Button>
          </Tooltip>
        );
      })}
    </div>
  );
};

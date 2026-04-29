import { AppstoreAddOutlined, AudioOutlined, FilePptOutlined, PictureOutlined, VideoCameraOutlined } from '@ant-design/icons';
import { Button, Dropdown, Space, Tag } from 'antd';
import type { MenuProps } from 'antd';
import type { ReactNode } from 'react';
import type { WorkbenchFunction } from '../../types/api';

interface CapabilityBarProps {
  capabilities: WorkbenchFunction[];
  selectedCapability: string;
  onSelect: (capabilityCode: string) => void;
}

const iconMap: Record<string, ReactNode> = {
  image_generation: <PictureOutlined />,
  image_recognition: <AppstoreAddOutlined />,
  text_to_speech: <AudioOutlined />,
  text_to_ppt: <FilePptOutlined />,
  text_to_video: <VideoCameraOutlined />,
};

export const CapabilityBar = ({ capabilities, selectedCapability, onSelect }: CapabilityBarProps) => {
  const mainItems = capabilities.filter((item) => item.showInMainBar || !item.showInMoreMenu);
  const moreItems = capabilities.filter((item) => item.showInMoreMenu && !item.showInMainBar);

  const menuItems: MenuProps['items'] = moreItems.map((item) => ({
    key: item.functionCode,
    label: item.functionName,
  }));

  return (
    <div className="capability-bar">
      <div className="capability-bar__header">
        <Tag color="cyan">Capabilities</Tag>
        <span className="table-subtext">手动选择优先，后续第 5 步再接自动意图识别。</span>
      </div>
      <Space wrap>
        {mainItems.map((capability) => (
          <Button
            key={capability.functionCode}
            type={selectedCapability === capability.functionCode ? 'primary' : 'default'}
            ghost={selectedCapability !== capability.functionCode}
            icon={iconMap[capability.functionCode] ?? <AppstoreAddOutlined />}
            onClick={() => onSelect(capability.functionCode)}
          >
            {capability.functionName}
          </Button>
        ))}
        {menuItems.length > 0 ? (
          <Dropdown
            menu={{
              items: menuItems,
              onClick: ({ key }) => onSelect(String(key)),
            }}
          >
            <Button>更多能力</Button>
          </Dropdown>
        ) : null}
      </Space>
    </div>
  );
};

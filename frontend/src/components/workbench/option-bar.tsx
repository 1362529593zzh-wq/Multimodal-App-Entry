import {
  BorderOutlined,
  DownOutlined,
  FormatPainterOutlined,
  PaperClipOutlined,
  PictureOutlined,
  ProductOutlined,
  SoundOutlined,
} from '@ant-design/icons';
import { Button, Dropdown, Upload } from 'antd';
import type { MenuProps, UploadProps } from 'antd';
import type { ReactNode } from 'react';
import type { WorkbenchBlueprint, WorkbenchDraftOptions, WorkbenchFieldDefinition } from '../../pages/workbench/types';

interface OptionBarProps {
  capabilityCode: string;
  blueprint: WorkbenchBlueprint;
  draft: WorkbenchDraftOptions;
  onChange: (key: string, value: string | undefined) => void;
  onUploadReference?: (file: File) => Promise<void>;
}

const labelMap: Record<string, string> = {
  model: '\u6a21\u578b',
  ratio: '\u6bd4\u4f8b',
  style: '\u98ce\u683c',
  template: '\u6a21\u677f',
  voice: '\u97f3\u8272',
  format: '\u683c\u5f0f',
  pages: '\u9875\u6570',
  duration: '\u65f6\u957f',
};

const hasGarbledText = (text?: string | null) =>
  Boolean(text && (/\?{2,}|�|鏂|鎼|鏈|褰|绛|鐢|å|ç|æ|ï¼|ä¸|é/.test(text)));

const iconMap: Record<string, ReactNode> = {
  model: <ProductOutlined />,
  ratio: <BorderOutlined />,
  style: <FormatPainterOutlined />,
  template: <PictureOutlined />,
  voice: <SoundOutlined />,
  format: <ProductOutlined />,
  pages: <PictureOutlined />,
  duration: <BorderOutlined />,
};

const imageGenerationDefaults: WorkbenchFieldDefinition[] = [
  {
    key: 'ratio',
    label: '\u6bd4\u4f8b',
    placeholder: '\u9009\u62e9\u6bd4\u4f8b',
    options: [
      { label: '1:1 \u6b63\u65b9\u5f62\uff0c\u5934\u50cf', value: '1:1' },
      { label: '2:3 \u793e\u4ea4\u5a92\u4f53\uff0c\u81ea\u62cd', value: '2:3' },
      { label: '3:4 \u7ecf\u5178\u6bd4\u4f8b\uff0c\u62cd\u7167', value: '3:4' },
      { label: '4:3 \u6587\u7ae0\u914d\u56fe\uff0c\u63d2\u753b', value: '4:3' },
      { label: '9:16 \u624b\u673a\u58c1\u7eb8\uff0c\u4eba\u50cf', value: '9:16' },
      { label: '16:9 \u684c\u9762\u58c1\u7eb8\uff0c\u98ce\u666f', value: '16:9' },
    ],
  },
  {
    key: 'style',
    label: '\u98ce\u683c',
    placeholder: '\u9009\u62e9\u98ce\u683c',
    options: [
      { label: '\u6e05\u65b0', value: '\u6e05\u65b0' },
      { label: '\u5199\u5b9e', value: '\u5199\u5b9e' },
      { label: 'anime', value: 'anime' },
      { label: '\u7535\u5f71\u611f', value: '\u7535\u5f71\u611f' },
    ],
  },
  {
    key: 'template',
    label: '\u6a21\u677f',
    placeholder: '\u9009\u62e9\u6a21\u677f',
    options: [
      { label: '\u6587\u751f\u56fe\u57fa\u7840\u6a21\u677f', value: 'tpl-img-gen-basic' },
      { label: '\u7535\u5546\u6d77\u62a5', value: 'poster' },
      { label: '\u4ea7\u54c1\u4e3b\u89c6\u89c9', value: 'product' },
    ],
  },
];

const videoGenerationDefaults: WorkbenchFieldDefinition[] = [
  {
    key: 'ratio',
    label: '\u6bd4\u4f8b',
    placeholder: '\u9009\u62e9\u6bd4\u4f8b',
    options: [
      { label: '16:9 \u6a2a\u7248\u89c6\u9891', value: '16:9' },
      { label: '9:16 \u7ad6\u7248\u89c6\u9891', value: '9:16' },
      { label: '1:1 \u65b9\u5f62\u89c6\u9891', value: '1:1' },
    ],
  },
];

const menuLabel = (label: string, selected: boolean) => (
  <span className="option-bar__menu-label">
    <span className={selected ? 'option-bar__check is-checked' : 'option-bar__check'} />
    <span>{label}</span>
  </span>
);

const buildMenu = (field: WorkbenchFieldDefinition, draft: WorkbenchDraftOptions, onChange: OptionBarProps['onChange']): MenuProps => ({
  selectable: true,
  selectedKeys: draft[field.key] ? [draft[field.key] as string] : [],
  items: field.options.map((option) => ({
    key: option.value,
    label: menuLabel(option.label, draft[field.key] === option.value),
  })),
  onClick: ({ key }) => onChange(field.key, String(key)),
});

const mergeFields = (fields: WorkbenchFieldDefinition[], defaults: WorkbenchFieldDefinition[]) => {
  const fieldMap = new Map(fields.map((field) => [field.key, field]));
  const defaultKeys = new Set(defaults.map((field) => field.key));
  const modelField = fieldMap.get('model');
  const mergedDefaults = defaults.map((fallback) => {
    const fromSchema = fieldMap.get(fallback.key);
    if (!fromSchema || fromSchema.options.length === 0) {
      return fallback;
    }
    return {
      ...fallback,
      ...fromSchema,
    };
  });
  const extraSchemaFields = fields.filter(
    (field) => field.key !== 'model' && !defaultKeys.has(field.key) && field.options.length > 0,
  );

  return [...(modelField?.options.length ? [modelField] : []), ...mergedDefaults, ...extraSchemaFields];
};

export const OptionBar = ({ capabilityCode, blueprint, draft, onChange, onUploadReference }: OptionBarProps) => {
  const fields =
    capabilityCode === 'image_generation'
      ? mergeFields(blueprint.fields, imageGenerationDefaults)
      : capabilityCode === 'text_to_video'
        ? mergeFields(blueprint.fields, videoGenerationDefaults)
        : blueprint.fields;

  const uploadProps: UploadProps = {
    beforeUpload: (file) => {
      if (onUploadReference) {
        void onUploadReference(file);
      } else {
        onChange('referenceImage', file.name);
      }
      return false;
    },
    showUploadList: false,
    accept: 'image/*',
  };

  return (
    <div className="option-bar" aria-label="option selector">
      {capabilityCode === 'image_generation' || capabilityCode === 'image_recognition' || capabilityCode === 'text_to_video' ? (
        <Upload {...uploadProps}>
          <Button
            type="text"
            icon={<PaperClipOutlined />}
            className={draft.referenceImage ? 'is-selected' : undefined}
            title={draft.referenceImage ? `\u53c2\u8003\u56fe\uff1a${draft.referenceImage}` : '\u53c2\u8003\u56fe'}
          >
            {'\u53c2\u8003\u56fe'}
          </Button>
        </Upload>
      ) : null}
      {fields.map((field) => {
        const label = labelMap[field.key] ?? (hasGarbledText(field.label) ? '参数' : field.label);
        const selectedValue = draft[field.key];
        const rawSelectedOptionLabel = field.options.find((option) => option.value === selectedValue)?.label;
        const selectedOptionLabel = hasGarbledText(rawSelectedOptionLabel) ? undefined : rawSelectedOptionLabel;
        const buttonLabel = field.key === 'model' ? selectedOptionLabel ?? label : label;

        return (
          <Dropdown
            key={field.key}
            trigger={['click']}
            menu={buildMenu(field, draft, onChange)}
            placement="top"
            overlayClassName="option-bar__dropdown"
          >
            <Button
              type="text"
              icon={iconMap[field.key]}
              className={selectedValue ? 'is-selected' : undefined}
              title={selectedValue ? `${label}\uff1a${selectedValue}` : label}
            >
              {buttonLabel}
              <DownOutlined />
            </Button>
          </Dropdown>
        );
      })}
    </div>
  );
};

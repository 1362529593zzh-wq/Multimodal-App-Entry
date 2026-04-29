import { Card, Empty, Select } from 'antd';
import type { WorkbenchBlueprint, WorkbenchDraftOptions } from '../../pages/workbench/types';

interface OptionBarProps {
  blueprint: WorkbenchBlueprint;
  draft: WorkbenchDraftOptions;
  onChange: (key: keyof WorkbenchDraftOptions, value: string | undefined) => void;
}

export const OptionBar = ({ blueprint, draft, onChange }: OptionBarProps) => {
  if (blueprint.fields.length === 0) {
    return (
      <Card className="option-bar option-bar--empty">
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前能力暂未配置快捷参数。" />
      </Card>
    );
  }

  return (
    <div className="option-bar">
      <div className="option-bar__header">
        <strong>参数快捷区</strong>
        <span className="table-subtext">{blueprint.helper}</span>
      </div>
      <div className="option-bar__grid">
        {blueprint.fields.map((field) => (
          <label key={field.key} className="option-bar__field">
            <span>{field.label}</span>
            <Select
              allowClear
              showSearch
              optionFilterProp="label"
              value={draft[field.key]}
              placeholder={field.placeholder}
              options={field.options}
              onChange={(value) => onChange(field.key, value)}
            />
          </label>
        ))}
      </div>
    </div>
  );
};

import { ArrowRightOutlined, FileImageOutlined } from '@ant-design/icons';
import { Button, Card, Space, Tag } from 'antd';
import type { WorkbenchResultData } from '../../pages/workbench/types';

interface ResultCardProps {
  result: WorkbenchResultData;
  onFollowUp: () => void;
}

export const ResultCard = ({ result, onFollowUp }: ResultCardProps) => (
  <Card className="message-card message-card--result">
    <Space className="message-card__header" align="center">
      <Tag color="success" icon={<FileImageOutlined />}>
        RESULT
      </Tag>
    </Space>
    <h4>{result.title}</h4>
    <p>{result.summary}</p>
    {result.previewImageUrl ? (
      <div className="result-card__preview-wrap">
        <img className="result-card__preview" src={result.previewImageUrl} alt={result.title} />
      </div>
    ) : null}
    <div className="result-card__chips">
      {result.chips.map((chip) => (
        <Tag key={chip}>{chip}</Tag>
      ))}
    </div>
    <Space wrap className="result-card__actions">
      {result.downloadUrl ? (
        <Button href={result.downloadUrl} target="_blank">
          下载结果
        </Button>
      ) : null}
      <Button type="primary" ghost icon={<ArrowRightOutlined />} onClick={onFollowUp}>
        {result.actionLabel}
      </Button>
    </Space>
  </Card>
);

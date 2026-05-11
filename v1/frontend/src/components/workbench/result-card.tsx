import { ArrowRightOutlined, FileImageOutlined } from '@ant-design/icons';
import { Button, Card, Space, Tag } from 'antd';
import type { WorkbenchResultData } from '../../pages/workbench/types';

interface ResultCardProps {
  result: WorkbenchResultData;
  onFollowUp: () => void;
}

const downloadLabel = '\u4e0b\u8f7d\u7ed3\u679c';
const followUpLabel = '\u57fa\u4e8e\u7ed3\u679c\u7ee7\u7eed';

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
    {result.kind === 'audio' && result.downloadUrl ? (
      <audio className="result-card__audio" controls src={result.downloadUrl}>
        {downloadLabel}
      </audio>
    ) : null}
    {result.kind === 'video' && (result.previewVideoUrl || result.downloadUrl) ? (
      <video
        className="result-card__video"
        controls
        playsInline
        preload="metadata"
        src={result.previewVideoUrl ?? result.downloadUrl}
      >
        {downloadLabel}
      </video>
    ) : null}
    {result.fileName ? <div className="result-card__filename">{result.fileName}</div> : null}
    <div className="result-card__chips">
      {result.chips.map((chip) => (
        <Tag key={chip}>{chip}</Tag>
      ))}
    </div>
    <Space wrap className="result-card__actions">
      {result.downloadUrl ? (
        <Button href={result.downloadUrl} target="_blank">
          {downloadLabel}
        </Button>
      ) : null}
      <Button type="primary" ghost icon={<ArrowRightOutlined />} onClick={onFollowUp}>
        {followUpLabel}
      </Button>
    </Space>
  </Card>
);

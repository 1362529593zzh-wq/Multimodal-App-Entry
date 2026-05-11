import { formatJsonPreview } from '../utils/format';

interface JsonPreviewProps {
  value?: string | null;
  emptyText?: string;
}

export const JsonPreview = ({ value, emptyText = '--' }: JsonPreviewProps) => {
  if (!value) {
    return <span>{emptyText}</span>;
  }

  return <pre className="json-preview">{formatJsonPreview(value)}</pre>;
};

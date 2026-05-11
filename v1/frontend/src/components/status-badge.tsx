import { Tag } from 'antd';

interface StatusBadgeProps {
  active: boolean;
  activeLabel?: string;
  inactiveLabel?: string;
}

export const StatusBadge = ({
  active,
  activeLabel = '启用中',
  inactiveLabel = '已停用',
}: StatusBadgeProps) => <Tag color={active ? 'success' : 'default'}>{active ? activeLabel : inactiveLabel}</Tag>;

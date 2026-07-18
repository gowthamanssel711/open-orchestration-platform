import { Badge } from './Badge';

const STATUS_MAP: Record<string, { variant: 'success' | 'warning' | 'error' | 'info' | 'default'; label: string }> = {
  RUNNING: { variant: 'info', label: 'Running' },
  COMPLETED: { variant: 'success', label: 'Completed' },
  CANCELLED: { variant: 'error', label: 'Cancelled' },
  SUSPENDED: { variant: 'warning', label: 'Suspended' },
  CREATED: { variant: 'default', label: 'Created' },
  ASSIGNED: { variant: 'info', label: 'Assigned' },
  ACTIVE: { variant: 'info', label: 'Active' },
  FAILED: { variant: 'error', label: 'Failed' },
};

interface StatusBadgeProps {
  status: string;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const config = STATUS_MAP[status] || { variant: 'default' as const, label: status };
  return <Badge variant={config.variant}>{config.label}</Badge>;
}

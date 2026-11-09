import { type ComponentType, type SVGProps } from 'react';

import {
  IconCheck,
  IconClock,
  IconDelete,
  IconHourglass,
  IconLock,
} from '@edifice.io/react/icons';
import Chip from '@mui/material/Chip';
import { lighten } from '@mui/material/styles';

import { useI18n } from '~/hooks/useI18n';
import { type Status } from '~/models';

type StatusColor = 'error' | 'warning' | 'success' | 'info';

/**
 * Color and icon of each case status.
 * Only OPEN is defined in the Figma design: the other statuses follow the same pattern.
 */
const STATUS_CHIPS: Record<
  Status,
  { color: StatusColor; Icon: ComponentType<SVGProps<SVGSVGElement>> }
> = {
  OPEN: { color: 'error', Icon: IconHourglass },
  PENDING: { color: 'warning', Icon: IconClock },
  QUARANTINE: { color: 'warning', Icon: IconLock },
  RESOURCE_DELETED: { color: 'success', Icon: IconDelete },
  RESOURCE_KEPT: { color: 'success', Icon: IconCheck },
  CLOSED: { color: 'info', Icon: IconCheck },
};

export interface ReportCaseStatusChipProps {
  status: Status;
}

/** Processing state of a reported case. */
export const ReportCaseStatusChip = ({ status }: ReportCaseStatusChipProps) => {
  const { t } = useI18n();
  const { color, Icon } = STATUS_CHIPS[status];

  return (
    <Chip
      variant="outlined"
      color={color}
      icon={<Icon width={20} height={20} />}
      label={t(`reports.status.${status}`)}
      data-testid="reports-case-status"
      sx={(theme) => ({
        // Same light background as the matching Alert (Figma _components/alert/*/background)
        bgcolor: lighten(theme.palette[color].light, 0.9),
        borderColor: theme.palette[color].main,
        ...theme.typography.body2,
        fontWeight: theme.typography.fontWeightBold,
      })}
    />
  );
};

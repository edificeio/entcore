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

interface StatusChip {
  color: StatusColor;
  Icon: ComponentType<SVGProps<SVGSVGElement>>;
}

/**
 * Color and icon of each case status.
 * Only OPEN is defined in the Figma design: the other statuses follow the same pattern.
 */
const STATUS_CHIPS: Record<Status, StatusChip> = {
  OPEN: { color: 'error', Icon: IconHourglass },
  PENDING: { color: 'warning', Icon: IconClock },
  QUARANTINE: { color: 'warning', Icon: IconLock },
  RESOURCE_DELETED: { color: 'success', Icon: IconDelete },
  RESOURCE_KEPT: { color: 'success', Icon: IconCheck },
  CLOSED: { color: 'info', Icon: IconCheck },
};

/** Neutral chip for a status the backend sends but the front does not know yet. */
const UNKNOWN_STATUS_CHIP: StatusChip = { color: 'info', Icon: IconClock };

export interface ReportCaseStatusChipProps {
  status: Status;
}

/** Processing state of a reported case. */
export const ReportCaseStatusChip = ({ status }: ReportCaseStatusChipProps) => {
  const { t } = useI18n();
  // The lookup misses when the backend adds a status: keep the list rendering
  const chip: StatusChip | undefined = STATUS_CHIPS[status];
  const { color, Icon } = chip ?? UNKNOWN_STATUS_CHIP;

  return (
    <Chip
      variant="outlined"
      color={color}
      icon={<Icon width={20} height={20} />}
      label={t(`reports.status.${status}`, { defaultValue: status })}
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

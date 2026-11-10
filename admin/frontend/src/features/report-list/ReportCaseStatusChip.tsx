import CheckCircleOutlinedIcon from '@mui/icons-material/CheckCircleOutlined';
import DeleteIcon from '@mui/icons-material/Delete';
import HelpOutlinedIcon from '@mui/icons-material/HelpOutlined';
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty';
import ScheduleIcon from '@mui/icons-material/Schedule';
import Chip from '@mui/material/Chip';
import { type SvgIconComponent } from '@mui/icons-material';
import { lighten, type Theme } from '@mui/material/styles';

import { useI18n } from '~/hooks/useI18n';
import { type Status } from '~/models';

interface StatusChipColors {
  background: string;
  border: string;
  text: string;
}

interface StatusChip {
  Icon: SvgIconComponent;
  colors: (theme: Theme) => StatusChipColors;
}

/**
 * Colored chip of the Figma "Chips_state" component: light Alert background
 * (Figma _components/alert/<color>/background), main border, given text shade.
 */
const alertColors =
  (color: 'error' | 'warning' | 'success' | 'info', text: 'main' | 'dark') =>
  (theme: Theme): StatusChipColors => ({
    background: lighten(theme.palette[color].light, 0.9),
    border: theme.palette[color].main,
    text: theme.palette[color][text],
  });

/** Neutral chip of the Figma "Chips_state" component (State=Supprimé). */
const neutralColors = (theme: Theme): StatusChipColors => ({
  background: theme.palette.text._states.hover,
  border: theme.palette.text.secondary,
  text: theme.palette.text.primary,
});

/** Figma State=Attente, shared by OPEN and PENDING. */
const OPEN_CHIP: StatusChip = {
  Icon: HourglassEmptyIcon,
  colors: alertColors('error', 'main'),
};

/**
 * Icon and colors of each case status, from the Figma "Chips_state" component.
 * PENDING has no variant in the design: it shows as OPEN until the backend
 * settles the final statuses.
 */
const STATUS_CHIPS: Record<Status, StatusChip> = {
  OPEN: OPEN_CHIP,
  PENDING: OPEN_CHIP,
  QUARANTINE: { Icon: ScheduleIcon, colors: alertColors('warning', 'dark') },
  RESOURCE_DELETED: { Icon: DeleteIcon, colors: neutralColors },
  RESOURCE_KEPT: {
    Icon: CheckCircleOutlinedIcon,
    colors: alertColors('success', 'dark'),
  },
};

/** Neutral chip for a status the backend sends but the front does not know yet. */
const UNKNOWN_STATUS_CHIP: StatusChip = {
  Icon: HelpOutlinedIcon,
  colors: neutralColors,
};

export interface ReportCaseStatusChipProps {
  status: Status;
}

/** Processing state of a reported case. */
export const ReportCaseStatusChip = ({ status }: ReportCaseStatusChipProps) => {
  const { t } = useI18n();
  // The lookup misses when the backend adds a status: keep the list rendering
  const chip: StatusChip | undefined = STATUS_CHIPS[status];
  const { Icon, colors } = chip ?? UNKNOWN_STATUS_CHIP;

  return (
    <Chip
      variant="outlined"
      icon={<Icon sx={{ fontSize: 20 }} />}
      label={t(`reports.status.${status}`, { defaultValue: status })}
      data-testid="reports-case-status"
      sx={(theme) => {
        const { background, border, text } = colors(theme);
        return {
          'bgcolor': background,
          'borderColor': border,
          'color': text,
          ...theme.typography.body2,
          'fontWeight': theme.typography.fontWeightBold,
          '& .MuiChip-icon': { color: 'inherit' },
        };
      }}
    />
  );
};

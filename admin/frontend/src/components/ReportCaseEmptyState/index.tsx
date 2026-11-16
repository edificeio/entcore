import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import emptyState from '~/assets/emptystate.svg';
import { useI18n } from '~/hooks/useI18n';

export interface ReportCaseEmptyStateProps {
  /** Bold line displayed above the message. */
  title?: string;
  /** Text displayed under the illustration, defaults to the generic "no report" message. */
  message?: string;
}

/** Empty state displayed when there is no reported case to show. */
export const ReportCaseEmptyState = ({
  title,
  message,
}: ReportCaseEmptyStateProps) => {
  const { t } = useI18n();

  return (
    <Stack
      spacing={2}
      sx={{ alignItems: 'center', py: 4, textAlign: 'center' }}
      data-testid="reports-empty-state"
    >
      <img src={emptyState} alt="" />
      {title && (
        <Typography variant="h4" component="p" sx={{ fontWeight: 'bold' }}>
          {title}
        </Typography>
      )}
      <Typography>{message ?? t('reports.no.report')}</Typography>
    </Stack>
  );
};

export default ReportCaseEmptyState;

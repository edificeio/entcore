import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import emptyState from '~/assets/emptystate.svg';
import { useI18n } from '~/hooks/useI18n';

export interface ReportCaseEmptyStateProps {
  /** Bold line displayed above the message. */
  title?: string;
  /** Illustration URL, defaults to the generic empty state picture. */
  image?: string;
  /** Display width of the illustration in px, for pictures exported at 2x. */
  imageWidth?: number;
  /** Text displayed under the illustration, defaults to the generic "no report" message. */
  message?: string;
}

/** Empty state displayed when there is no reported case to show. */
export const ReportCaseEmptyState = ({
  title,
  message,
  image = emptyState,
  imageWidth,
}: ReportCaseEmptyStateProps) => {
  const { t } = useI18n();

  return (
    <Stack
      spacing={2}
      sx={{ alignItems: 'center', py: 4, textAlign: 'center' }}
      data-testid="reports-empty-state"
    >
      <Box
        component="img"
        src={image}
        alt=""
        sx={{ width: imageWidth, maxWidth: '100%' }}
      />
      <Stack spacing={0.5}>
        {title && (
          <Typography variant="h6" component="p">
            {title}
          </Typography>
        )}
        <Typography>{message ?? t('reports.no.report')}</Typography>
      </Stack>
    </Stack>
  );
};

export default ReportCaseEmptyState;

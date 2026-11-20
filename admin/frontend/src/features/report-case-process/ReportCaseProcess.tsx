import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import visuallyHidden from '@mui/utils/visuallyHidden';
import { Link as RouterLink } from 'react-router-dom';

import { ReportCaseCard } from '~/components/ReportCaseCard';
import { useI18n } from '~/hooks/useI18n';
import { useCase } from '~/services';

export interface ReportCaseProcessProps {
  caseId: string;
  /** Structure the case is moderated for. */
  structureId: string;
}

/**
 * Card of a reported case followed by the decision and comments areas used to
 * process it. The areas only hold their heading for now.
 */
export function ReportCaseProcess({
  caseId,
  structureId,
}: ReportCaseProcessProps) {
  const { t } = useI18n();
  const { data: reportCase, isPending, isError } = useCase(caseId);

  const renderContent = () => {
    if (isPending) {
      return (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          {/* Decorative: loading is announced by the status region */}
          <CircularProgress aria-hidden />
        </Box>
      );
    }

    if (isError) {
      return <Alert severity="error">{t('reports.detail.error')}</Alert>;
    }

    return (
      <Stack spacing={4}>
        <ReportCaseCard
          reportCase={reportCase}
          titleId={`report-case-title-${reportCase.id}`}
          structureId={structureId}
          titleComponent="h2"
        />

        {/* As in Figma: decision on the left (wider), comments on the right */}
        <Grid container spacing={3}>
          <Grid size={{ xs: 12, md: 7 }}>
            <Box
              sx={{ p: 3, bgcolor: 'background.paper', height: '100%' }}
              data-testid="report-case-process-decision"
            >
              <Typography
                component="h2"
                variant="subtitle2"
                sx={{ fontWeight: 'fontWeightBold' }}
              >
                {t('reports.process.decision.title')}
              </Typography>
              <Typography variant="body2" sx={{ mt: 1 }}>
                {t('reports.process.decision.help')}
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 12, md: 5 }}>
            <Box
              sx={{ p: 3, bgcolor: 'background.paper', height: '100%' }}
              data-testid="report-case-process-comments"
            >
              <Typography component="h2" variant="h4">
                {t('reports.process.comments.title')}
              </Typography>
              <Typography variant="body2" sx={{ mt: 1 }}>
                {t('reports.process.comments.help')}
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </Stack>
    );
  };

  return (
    <Stack spacing={4}>
      {/* Status message (RGAA 7.5): always rendered so that updates are announced */}
      <Box role="status" sx={visuallyHidden}>
        {isPending ? t('reports.detail.loading') : ''}
      </Box>
      {renderContent()}

      {/* Always rendered: cancel is the only way back, even when loading fails */}
      <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
        <Button
          component={RouterLink}
          to={`/reports/${structureId}/cases/${caseId}`}
          variant="text"
          data-testid="report-case-process-cancel"
        >
          {t('reports.process.cancel')}
        </Button>
        {reportCase && (
          // Enabled once a decision can be chosen (next iteration)
          <Button
            variant="contained"
            disabled
            data-testid="report-case-process-submit"
          >
            {t('reports.process.submit')}
          </Button>
        )}
      </Stack>
    </Stack>
  );
}

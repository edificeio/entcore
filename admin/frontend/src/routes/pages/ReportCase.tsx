import Button from '@mui/material/Button';
import Container from '@mui/material/Container';
import Stack from '@mui/material/Stack';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import { Link as RouterLink, useParams } from 'react-router-dom';

import { ReportCaseDetails } from '~/features/report-details';
import { useI18n } from '~/hooks/useI18n';
import { usePageTitle } from '~/hooks/usePageTitle';
import { useCase } from '~/services';

export const loader = async () => {
  return null;
};

/** Detail page of a reported case. */
export const Component = () => {
  const { t } = useI18n();
  // Always set: the route is reports/:structureid/cases/:caseId
  const { structureid = '', caseId = '' } = useParams();
  // Shares its cache with ReportCaseDetails: no extra request
  const { data: reportCase } = useCase(caseId);

  usePageTitle(
    [reportCase?.title, t('reports.page.title'), t('admin.title')]
      .filter(Boolean)
      .join(' - '),
  );

  return (
    <Container component="main" sx={{ py: 6 }}>
      <Stack spacing={4} sx={{ alignItems: 'stretch' }}>
        <Button
          component={RouterLink}
          to={`/reports/${structureid}`}
          variant="text"
          startIcon={<ChevronLeftIcon />}
          sx={{ alignSelf: 'flex-start' }}
          data-testid="report-case-back-link"
        >
          {t('reports.detail.back')}
        </Button>

        <ReportCaseDetails caseId={caseId} structureId={structureid} />
      </Stack>
    </Container>
  );
};

import Container from '@mui/material/Container';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useParams } from 'react-router-dom';

import { ReportCaseProcess } from '~/features/report-case-process';
import { useI18n } from '~/hooks/useI18n';
import { usePageTitle } from '~/hooks/usePageTitle';
import { useCase } from '~/services';

export const loader = async () => {
  return null;
};

/** Processing page of a reported case. */
export const Component = () => {
  const { t } = useI18n();
  // Always set: the route is reports/:structureid/cases/:caseId/process
  const { structureid = '', caseId = '' } = useParams();
  // Shares its cache with ReportCaseProcess: no extra request
  const { data: reportCase } = useCase(caseId);

  usePageTitle(
    [reportCase?.title, t('reports.process.title'), t('admin.title')]
      .filter(Boolean)
      .join(' - '),
  );

  return (
    <Container component="main" sx={{ py: 6 }}>
      <Stack spacing={4}>
        <Typography component="h1" variant="h1">
          {t('reports.process.title')}
        </Typography>

        <ReportCaseProcess caseId={caseId} structureId={structureid} />
      </Stack>
    </Container>
  );
};

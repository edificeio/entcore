import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { ReportCard } from '~/components/ReportCard';
import { useI18n } from '~/hooks/useI18n';
import { type Report } from '~/models';

export interface ReportListProps {
  /** Reports received on the case, as loaded with it. */
  reports: Report[];
}

/** Reports received on a case, from the most recent. */
export function ReportList({ reports }: ReportListProps) {
  const { t } = useI18n();
  // Sorted copy: the reports belong to the cached case and must not be mutated
  const sortedReports = [...reports].sort(
    (a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt),
  );

  return (
    <Stack spacing={2}>
      <Typography variant="h3" component="h2">
        {t('reports.detail.tab.received')}
      </Typography>
      <Stack component="ul" spacing={2} sx={{ listStyle: 'none', p: 0, m: 0 }}>
        {sortedReports.map((report) => (
          <li key={report.id}>
            <ReportCard report={report} />
          </li>
        ))}
      </Stack>
    </Stack>
  );
}

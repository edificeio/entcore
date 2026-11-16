import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import emptyHistory from '~/assets/emptystate-history.png';
import { DecisionCard } from '~/components/DecisionCard';
import { ReportCaseEmptyState } from '~/components/ReportCaseEmptyState';
import { useI18n } from '~/hooks/useI18n';
import { type Decision } from '~/models';

export interface DecisionsHistoryProps {
  /** Decisions taken on the case, as loaded with it. */
  decisions: Decision[];
}

/** Decisions taken on a case, from the oldest, or an empty state. */
export function DecisionsHistory({ decisions }: DecisionsHistoryProps) {
  const { t } = useI18n();

  if (decisions.length === 0) {
    return (
      <ReportCaseEmptyState
        title={t('reports.history.empty.title')}
        message={t('reports.history.empty.text')}
        image={emptyHistory}
        // Exported at 2x for sharp rendering on high density screens
        imageWidth={310}
      />
    );
  }

  // Sorted copy: the decisions belong to the cached case and must not be mutated
  const sortedDecisions = [...decisions].sort(
    (a, b) => Date.parse(a.createdAt) - Date.parse(b.createdAt),
  );

  return (
    <Stack spacing={2}>
      <div>
        <Typography variant="h3" component="h2">
          {t('reports.history.title')}
        </Typography>
        <Typography sx={{ mt: 0.5 }}>{t('reports.history.subtitle')}</Typography>
      </div>
      {/* Explicit role: Safari drops the list semantics of a list without bullets */}
      <Stack
        component="ul"
        role="list"
        spacing={2}
        sx={{ listStyle: 'none', p: 0, m: 0 }}
      >
        {sortedDecisions.map((decision) => (
          <li key={decision.id}>
            <DecisionCard decision={decision} />
          </li>
        ))}
      </Stack>
    </Stack>
  );
}

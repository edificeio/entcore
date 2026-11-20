import { useDate } from '@edifice.io/react';
import Typography from '@mui/material/Typography';

import { CollapsibleCard } from '~/components/CollapsibleCard';
import { DecisionComment } from '~/components/DecisionComment';
import { useI18n } from '~/hooks/useI18n';
import { type Decision } from '~/models';

export interface DecisionCardProps {
  decision: Decision;
}

/** Card of one moderation decision, whose comments unfold on demand. */
export const DecisionCard = ({ decision }: DecisionCardProps) => {
  const { t } = useI18n();
  const { formatDate } = useDate();

  const dateTime = t('reports.report.datetime', {
    date: formatDate(decision.createdAt, 'L'),
    time: formatDate(decision.createdAt, 'LT'),
  });

  return (
    <CollapsibleCard
      header={
        <>
          <Typography sx={{ fontWeight: 'bold' }}>
            {t(`reports.decision.status.${decision.status}`, {
              defaultValue: decision.status,
            })}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            {`${dateTime} • ${decision.displayName}`}
          </Typography>
        </>
      }
      expandLabel={t('reports.decision.show.comments')}
      collapseLabel={t('reports.decision.hide.comments')}
      data-testid="reports-decision-card"
    >
      <DecisionComment
        title={t('reports.decision.comment.internal')}
        comment={decision.comment}
      />
    </CollapsibleCard>
  );
};

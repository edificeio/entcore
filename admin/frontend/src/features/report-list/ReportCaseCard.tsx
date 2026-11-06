import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Typography from '@mui/material/Typography';

import { type Case } from '~/models';

export interface ReportCaseCardProps {
  reportCase: Case;
  /** Id of the title, used to label the enclosing feed article. */
  titleId: string;
}

/**
 * Minimal case card.
 * TODO: complete content and layout (dedicated branch).
 */
export const ReportCaseCard = ({
  reportCase,
  titleId,
}: ReportCaseCardProps) => (
  <Card variant="outlined" data-testid="reports-case-card">
    <CardContent>
      <Typography id={titleId} variant="subtitle1" component="h2">
        {reportCase.title}
      </Typography>
      <Typography variant="body2" color="text.secondary">
        {reportCase.module} · {reportCase.senderDisplayName} ·{' '}
        {new Date(reportCase.createAt).toLocaleDateString()}
      </Typography>
    </CardContent>
  </Card>
);

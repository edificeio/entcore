import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';

import { useI18n } from '~/hooks/useI18n';

export interface DecisionCommentProps {
  /** Who the comment is for, e.g. "Note interne". */
  title: string;
  /** Names of the recipients, for comments sent to people. */
  recipients?: string;
  comment: string;
}

/** Grey block of one comment left with a moderation decision. */
export const DecisionComment = ({
  title,
  recipients,
  comment,
}: DecisionCommentProps) => {
  const { t } = useI18n();

  return (
    <Box
      sx={{ bgcolor: 'grey.100', borderRadius: 1, p: 1 }}
      data-testid="reports-decision-comment"
    >
      <Typography sx={{ fontWeight: 'bold' }}>{title}</Typography>
      {recipients && (
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ mt: 0.5 }}
          data-testid="reports-decision-comment-recipients"
        >
          {recipients}
        </Typography>
      )}
      {/* Plain text: unlike report comments, typed line breaks are not kept */}
      <Typography
        sx={{ mt: 1.5, whiteSpace: 'normal', overflowWrap: 'anywhere' }}
        data-testid="reports-decision-comment-text"
      >
        {comment || t('reports.decision.comment.empty')}
      </Typography>
    </Box>
  );
};

import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';

import { useI18n } from '~/hooks/useI18n';

const TITLE_ID = 'report-case-process-leave-title';
const TEXT_ID = 'report-case-process-leave-text';

export interface LeaveProcessDialogProps {
  open: boolean;
  /** Leave the page: the decision is dropped. */
  onLeave: () => void;
  /** Stay on the page with the decision and comments untouched. */
  onStay: () => void;
}

/** Warns that a chosen decision is not saved when leaving the processing page. */
export function LeaveProcessDialog({
  open,
  onLeave,
  onStay,
}: LeaveProcessDialogProps) {
  const { t } = useI18n();

  return (
    <Dialog
      open={open}
      // Escape or a click outside keeps the administrator on the page
      onClose={onStay}
      aria-labelledby={TITLE_ID}
      aria-describedby={TEXT_ID}
      maxWidth="xs"
      data-testid="report-case-process-leave-dialog"
    >
      <DialogTitle id={TITLE_ID}>
        {t('reports.process.leave.title')}
      </DialogTitle>
      <DialogContent>
        <DialogContentText id={TEXT_ID}>
          {t('reports.process.leave.text')}
        </DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button
          variant="text"
          onClick={onLeave}
          data-testid="report-case-process-leave-confirm"
        >
          {t('reports.process.leave.confirm')}
        </Button>
        {/* Staying is the safe choice: it gets the focus */}
        <Button
          variant="contained"
          onClick={onStay}
          autoFocus
          data-testid="report-case-process-leave-stay"
        >
          {t('reports.process.leave.stay')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

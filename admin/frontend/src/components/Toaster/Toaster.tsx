import Alert from '@mui/material/Alert';
import Snackbar, { type SnackbarCloseReason } from '@mui/material/Snackbar';

import { useI18n } from '~/hooks/useI18n';
import { useToastStore } from '~/store/toast';

/** Time a toast stays on screen, in milliseconds. */
const AUTO_HIDE_DURATION = 6000;

/** Shows the global toast at the top right of the page, as in Figma. */
export function Toaster() {
  const { t } = useI18n();
  const { toast, open, closeToast } = useToastStore();

  const handleClose = (
    _event?: React.SyntheticEvent | Event | null,
    reason?: SnackbarCloseReason,
  ) => {
    // A click elsewhere on the page must not dismiss the confirmation
    if (reason === 'clickaway') return;
    closeToast();
  };

  return (
    <Snackbar
      open={open && toast !== null}
      autoHideDuration={AUTO_HIDE_DURATION}
      onClose={handleClose}
      anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
    >
      {/* Snackbar needs a child that holds a ref, even when there is no toast yet */}
      <Alert
        severity={toast?.severity ?? 'success'}
        variant="standard"
        onClose={handleClose}
        closeText={t('toast.close')}
        sx={{ boxShadow: 3 }}
        data-testid="toaster-alert"
      >
        {toast?.message}
      </Alert>
    </Snackbar>
  );
}

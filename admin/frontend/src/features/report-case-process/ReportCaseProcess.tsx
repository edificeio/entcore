import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { flushSync } from 'react-dom';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import visuallyHidden from '@mui/utils/visuallyHidden';
import {
  Navigate,
  Link as RouterLink,
  useBlocker,
  useNavigate,
} from 'react-router-dom';

import { ReportCaseCard } from '~/components/ReportCaseCard';
import { useI18n } from '~/hooks/useI18n';
import { reportsTabParam } from '~/routes/pages/reportsTab';
import { useAddDecision, useCase } from '~/services';
import { showToast } from '~/store/toast';
import { DecisionOptions } from './DecisionOptions';
import { LeaveProcessDialog } from './LeaveProcessDialog';
import {
  PROCESS_OPTIONS,
  canProcess,
  processOptions,
  type ProcessOption,
} from './processOptions';

/** Id of the "Décision" heading, which labels the radio group. */
const DECISION_TITLE_ID = 'report-case-process-decision-title';

export interface ReportCaseProcessProps {
  caseId: string;
  /** Structure the case is moderated for. */
  structureId: string;
}

/**
 * Card of a reported case followed by the form that processes it: a decision,
 * an optional internal note, then cancel or submit.
 */
export function ReportCaseProcess({
  caseId,
  structureId,
}: ReportCaseProcessProps) {
  const { t } = useI18n();
  const navigate = useNavigate();
  const { data: reportCase, isPending, isError } = useCase(caseId);
  const addDecision = useAddDecision();
  const [decision, setDecision] = useState<ProcessOption['status'] | null>(
    null,
  );
  const [note, setNote] = useState('');
  // Set synchronously: the mutation state only updates after a re-render, too
  // late to ignore a second click on submit
  const submittingRef = useRef(false);
  // Set once the decision is saved: the ref is read synchronously by the
  // navigation that follows, the state by the next renders
  const savedRef = useRef(false);
  const [saved, setSaved] = useState(false);
  const detailHref = `/reports/${structureId}/cases/${caseId}`;
  const processable = !reportCase || canProcess(reportCase.status);
  // A chosen decision is lost when leaving the page (unless the case can no
  // longer be processed: the page then redirects to the case detail)
  const hasUnsavedDecision = decision !== null && processable && !saved;

  // useBlocker only picks up a new condition in a passive effect, after the
  // <Navigate> below has already navigated: the live condition goes through a
  // ref, updated by a layout effect, which runs before any passive effect
  const shouldBlockRef = useRef(false);
  useLayoutEffect(() => {
    shouldBlockRef.current = hasUnsavedDecision;
  }, [hasUnsavedDecision]);

  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      shouldBlockRef.current &&
      !savedRef.current &&
      currentLocation.pathname !== nextLocation.pathname,
  );

  // Leaving the app (reload, closed tab, console links) only allows the
  // browser's own confirmation
  useEffect(() => {
    if (!hasUnsavedDecision) return;
    const warn = (event: BeforeUnloadEvent) => {
      if (savedRef.current) return;
      event.preventDefault();
      // Still required by some browsers to show the confirmation
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [hasUnsavedDecision]);

  // Reached by URL on a case that can no longer be processed (deleted content).
  // Not for our own decision: the mutation awaits the refetch it triggers
  // before its success callback, so the case may already show as deleted while
  // it is still pending; redirecting then would land on the detail page
  // instead of the reports list
  if (!processable && !saved && !addDecision.isPending) {
    return <Navigate to={detailHref} replace />;
  }

  const handleSubmit = () => {
    const option = PROCESS_OPTIONS.find(({ status }) => status === decision);
    if (!option || submittingRef.current) return;
    submittingRef.current = true;

    addDecision.mutate(
      { id: caseId, payload: { status: option.status, comment: note.trim() } },
      {
        onSuccess: () => {
          savedRef.current = true;
          // Committed right away: the refetch of the now processed case comes
          // through a synchronous store update, which would otherwise render
          // before this state and redirect to the case detail
          flushSync(() => setSaved(true));
          showToast({
            severity: 'success',
            message: t(`reports.process.success.${option.status}`),
          });
          navigate(
            `/reports/${structureId}?tab=${reportsTabParam(option.tab)}`,
          );
        },
        // The form stays filled, so that the administrator can try again
        onError: () => {
          submittingRef.current = false;
          showToast({ severity: 'error', message: t('reports.process.error') });
        },
      },
    );
  };

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
                id={DECISION_TITLE_ID}
                component="h2"
                variant="subtitle2"
                sx={{ fontWeight: 'fontWeightBold' }}
              >
                {t('reports.process.decision.title')}
              </Typography>
              <Typography variant="body2" sx={{ mt: 1, mb: 2 }}>
                {t('reports.process.decision.help')}
              </Typography>
              <DecisionOptions
                options={processOptions(reportCase.status)}
                value={decision}
                onChange={setDecision}
                labelledBy={DECISION_TITLE_ID}
              />
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
                {decision
                  ? t('reports.process.comments.intro')
                  : t('reports.process.comments.help')}
              </Typography>
              {/* Only the internal note for now; messages to users come later */}
              {decision && (
                <TextField
                  label={t('reports.process.note.label')}
                  placeholder={t('reports.process.note.placeholder')}
                  helperText={t('reports.process.note.help')}
                  value={note}
                  onChange={(event) => setNote(event.target.value)}
                  multiline
                  minRows={2}
                  fullWidth
                  sx={{ mt: 3 }}
                  slotProps={{
                    inputLabel: { shrink: true },
                    htmlInput: { 'data-testid': 'report-case-process-note' },
                  }}
                />
              )}
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

      <LeaveProcessDialog
        open={blocker.state === 'blocked'}
        onLeave={() => blocker.proceed?.()}
        onStay={() => blocker.reset?.()}
      />

      {/* Always rendered: cancel is the only way back, even when loading fails */}
      <Stack direction="row" spacing={2} sx={{ justifyContent: 'flex-end' }}>
        <Button
          component={RouterLink}
          to={detailHref}
          variant="text"
          // Once sent, the decision is applied: leaving cannot cancel it anymore
          disabled={addDecision.isPending}
          data-testid="report-case-process-cancel"
        >
          {t('reports.process.cancel')}
        </Button>
        {reportCase && (
          <Button
            variant="contained"
            disabled={!decision || addDecision.isPending}
            onClick={handleSubmit}
            data-testid="report-case-process-submit"
          >
            {t('reports.process.submit')}
          </Button>
        )}
      </Stack>
    </Stack>
  );
}

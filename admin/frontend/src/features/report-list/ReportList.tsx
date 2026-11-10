import { type KeyboardEvent, useCallback, useRef } from 'react';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Stack from '@mui/material/Stack';
import visuallyHidden from '@mui/utils/visuallyHidden';

import { ReportCaseEmptyState } from '~/components/ReportCaseEmptyState';
import { useI18n } from '~/hooks/useI18n';
import { useIntersectionObserver } from '~/hooks/useIntersectionObserver';
import { type WorkflowState } from '~/models';
import { useSearchCases } from '~/services';
import { ReportCaseCard } from './ReportCaseCard';

/** Fetch the next page as soon as focus reaches one of the last N cards. */
const PREFETCH_THRESHOLD = 3;

export interface ReportListProps {
  workflowState: WorkflowState;
}

/** Decorative spinner: loading is announced by the status region. */
const Spinner = ({ size, py }: { size?: number; py: number }) => (
  <Box sx={{ display: 'flex', justifyContent: 'center', py }}>
    <CircularProgress size={size} aria-hidden />
  </Box>
);

/**
 * Infinite list of reported cases, implemented as an ARIA feed:
 * - mouse/touch: next page loads when the end of the list becomes visible;
 * - keyboard: each card is focusable (Tab, or Page Down / Page Up between cards),
 *   and the next page loads when focus gets close to the end of the list.
 */
export function ReportList({ workflowState }: ReportListProps) {
  const { t } = useI18n();
  const feedRef = useRef<HTMLDivElement>(null);
  const {
    data,
    isPending,
    isError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useSearchCases(workflowState);

  const loadMore = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const sentinelRef = useIntersectionObserver(loadMore, {
    enabled: !!hasNextPage,
  });

  const isLoading = isPending || isFetchingNextPage;

  const renderContent = () => {
    if (isPending) return <Spinner py={4} />;

    if (isError) {
      return <Alert severity="error">{t('reports.list.error')}</Alert>;
    }

    const cases = data.pages.flatMap((page) => page.items ?? []);
    const totalCount = data.pages[0]?.totalCount ?? 0;

    if (cases.length === 0) return <ReportCaseEmptyState />;

    const focusArticle = (index: number) => {
      const articles =
        feedRef.current?.querySelectorAll<HTMLElement>('[role="article"]');
      articles?.item(index)?.focus();
    };

    const handleKeyDown = (
      event: KeyboardEvent<HTMLElement>,
      index: number,
    ) => {
      if (event.key === 'PageDown') {
        event.preventDefault();
        focusArticle(index + 1);
      } else if (event.key === 'PageUp') {
        event.preventDefault();
        focusArticle(index - 1);
      }
    };

    const handleFocus = (index: number) => {
      if (index >= cases.length - PREFETCH_THRESHOLD) loadMore();
    };

    return (
      <>
        <Stack
          ref={feedRef}
          role="feed"
          aria-label={t('reports.list.label')}
          aria-busy={isFetchingNextPage}
          spacing={2}
          data-testid="reports-list-feed"
        >
          {cases.map((reportCase, index) => {
            const titleId = `reports-case-title-${reportCase.id}`;
            return (
              <Box
                key={reportCase.id}
                role="article"
                tabIndex={0}
                aria-posinset={index + 1}
                aria-setsize={totalCount}
                aria-labelledby={titleId}
                onFocus={() => handleFocus(index)}
                onKeyDown={(event) => handleKeyDown(event, index)}
                sx={{
                  'borderRadius': 1,
                  '&:focus-visible': {
                    outline: 2,
                    outlineColor: 'primary.main',
                    outlineOffset: 2,
                  },
                }}
              >
                <ReportCaseCard reportCase={reportCase} titleId={titleId} />
              </Box>
            );
          })}
        </Stack>

        {hasNextPage && (
          <Box ref={sentinelRef} aria-hidden sx={{ height: 1 }} />
        )}

        {isFetchingNextPage && <Spinner size={24} py={2} />}
      </>
    );
  };

  return (
    <>
      {/* Status message (RGAA 7.5): always rendered so that updates are announced */}
      <Box role="status" sx={visuallyHidden}>
        {isLoading ? t('reports.list.loading') : ''}
      </Box>
      {renderContent()}
    </>
  );
}

import { useState, type SyntheticEvent } from 'react';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import CircularProgress from '@mui/material/CircularProgress';
import Stack from '@mui/material/Stack';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import visuallyHidden from '@mui/utils/visuallyHidden';

import { ReportCaseCard } from '~/components/ReportCaseCard';
import { useI18n } from '~/hooks/useI18n';
import { useCase } from '~/services';

/** Tabs of the case detail page; the received reports one is open by default. */
const TABS = [
  { key: 'received', labelKey: 'reports.detail.tab.received' },
  { key: 'content', labelKey: 'reports.detail.tab.content' },
  { key: 'history', labelKey: 'reports.detail.tab.history' },
] as const;

type TabKey = (typeof TABS)[number]['key'];

const tabId = (key: TabKey) => `report-case-tab-${key}`;
const panelId = (key: TabKey) => `report-case-tabpanel-${key}`;

export interface ReportCaseDetailsProps {
  caseId: string;
  /** Structure the case is moderated for. */
  structureId: string;
}

/** Card of a reported case followed by the tabs that detail it. */
export function ReportCaseDetails({
  caseId,
  structureId,
}: ReportCaseDetailsProps) {
  const { t } = useI18n();
  const { data: reportCase, isPending, isError } = useCase(caseId);
  const [currentTab, setCurrentTab] = useState(0);

  const handleChange = (_event: SyntheticEvent, newValue: number) => {
    setCurrentTab(newValue);
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

    const titleId = `report-case-title-${reportCase.id}`;

    return (
      <Stack spacing={4}>
        <ReportCaseCard
          reportCase={reportCase}
          titleId={titleId}
          structureId={structureId}
          titleComponent="h1"
        />

        <Box>
          <Tabs
            value={currentTab}
            onChange={handleChange}
            indicatorColor="primary"
            textColor="primary"
            // Scrolls on narrow screens rather than overflowing the page
            variant="scrollable"
            scrollButtons="auto"
            aria-label={t('reports.detail.tabs.label')}
            // As in Figma: the white bar stops after the last tab
            sx={{ width: 'fit-content', maxWidth: '100%' }}
          >
            {TABS.map(({ key, labelKey }) => (
              <Tab
                key={key}
                id={tabId(key)}
                aria-controls={panelId(key)}
                label={
                  key === 'received'
                    ? `${t(labelKey)} (${reportCase.reports.length})`
                    : t(labelKey)
                }
                data-testid={`report-case-tab-${key}`}
              />
            ))}
          </Tabs>

          {/* Empty shells for now: each tab content comes with its own feature */}
          {TABS.map(({ key }, index) => (
            <div
              key={key}
              role="tabpanel"
              hidden={currentTab !== index}
              id={panelId(key)}
              aria-labelledby={tabId(key)}
              data-testid={`report-case-tabpanel-${key}`}
            />
          ))}
        </Box>
      </Stack>
    );
  };

  return (
    <>
      {/* Status message (RGAA 7.5): always rendered so that updates are announced */}
      <Box role="status" sx={visuallyHidden}>
        {isPending ? t('reports.detail.loading') : ''}
      </Box>
      {renderContent()}
    </>
  );
}

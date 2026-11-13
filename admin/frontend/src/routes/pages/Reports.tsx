import { useState, type SyntheticEvent } from 'react';

import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Grid from '@mui/material/Grid';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';

import { ReportList } from '~/features/report-list';
import { useI18n } from '~/hooks/useI18n';
import { usePageTitle } from '~/hooks/usePageTitle';
import { type WorkflowState } from '~/models';
import { useSearchCases } from '~/services';

/** Check old format URL and redirect if needed */
export const loader = async () => {
  return null;
};

const TABS: { workflowState: WorkflowState; labelKey: string }[] = [
  { workflowState: 'PENDING', labelKey: 'reports.tab.pending' },
  { workflowState: 'SETTLE', labelKey: 'reports.tab.settled' },
];

const tabId = (workflowState: WorkflowState) => `reports-tab-${workflowState}`;
const panelId = (workflowState: WorkflowState) =>
  `reports-tabpanel-${workflowState}`;

/** Total number of cases for a workflow state, read from the first page of its list. */
const useCasesCount = (workflowState: WorkflowState) =>
  useSearchCases(workflowState).data?.pages[0]?.totalCount;

export const Component = () => {
  const { t } = useI18n();
  const [currentTab, setCurrentTab] = useState(0);
  const counts: Record<WorkflowState, number | undefined> = {
    PENDING: useCasesCount('PENDING'),
    SETTLE: useCasesCount('SETTLE'),
  };

  usePageTitle(`${t('reports.page.title')} - ${t('admin.title')}`);

  const handleChange = (_event: SyntheticEvent, newValue: number) => {
    setCurrentTab(newValue);
  };

  return (
    <Container component="main">
      <Grid>
        <Box>
          <Tabs
            value={currentTab}
            onChange={handleChange}
            indicatorColor="primary"
            textColor="primary"
            variant="fullWidth"
            aria-label={t('reports.tabs.label')}
          >
            {TABS.map(({ workflowState, labelKey }) => {
              const count = counts[workflowState];
              return (
                <Tab
                  key={workflowState}
                  id={tabId(workflowState)}
                  aria-controls={panelId(workflowState)}
                  label={
                    count === undefined
                      ? t(labelKey)
                      : `${t(labelKey)} (${count})`
                  }
                  data-testid={`reports-tab-${workflowState.toLowerCase()}`}
                />
              );
            })}
          </Tabs>

          <Box sx={{ px: 3, pt: 3, bgcolor: 'background.paper' }}>
            <Alert
              severity="info"
              variant="outlined"
              // Static information, not a live alert
              role="note"
              data-testid="reports-retention-alert"
            >
              {t('reports.retention.info')}
            </Alert>
          </Box>

          {TABS.map(({ workflowState }, index) => (
            <div
              key={workflowState}
              role="tabpanel"
              hidden={currentTab !== index}
              id={panelId(workflowState)}
              aria-labelledby={tabId(workflowState)}
            >
              {currentTab === index && (
                <Box sx={{ p: 3, bgcolor: 'background.paper' }}>
                  <ReportList workflowState={workflowState} />
                </Box>
              )}
            </div>
          ))}
        </Box>
      </Grid>
    </Container>
  );
};

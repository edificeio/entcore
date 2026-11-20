import { type WorkflowState } from '~/models';

/** Query parameter holding the open tab of the reports list. */
export const REPORTS_TAB_PARAM = 'tab';

/** Value of the tab parameter for each workflow state. */
const TAB_PARAMS: Record<WorkflowState, string> = {
  PENDING: 'pending',
  SETTLE: 'settled',
};

/** Tab parameter of a workflow state, e.g. to open the processed tab. */
export const reportsTabParam = (workflowState: WorkflowState) =>
  TAB_PARAMS[workflowState];

/** Workflow state of a tab parameter; the pending tab when missing or unknown. */
export const workflowStateOfTab = (param: string | null): WorkflowState =>
  (Object.keys(TAB_PARAMS) as WorkflowState[]).find(
    (workflowState) => TAB_PARAMS[workflowState] === param,
  ) ?? 'PENDING';

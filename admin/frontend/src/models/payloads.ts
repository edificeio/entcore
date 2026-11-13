import { type Status } from './shared';

/** Workflow state used to split cases between "to process" and "processed". */
export type WorkflowState = 'PENDING' | 'SETTLE';

/**
 * Body of `POST /moderation/api/v1/cases/_search`.
 * Only `workflowState` is mandatory, other criteria are still to be defined by the back-end.
 */
export interface CaseSearchPayload {
  workflowState: WorkflowState;
}

/** Body of `POST /moderation/api/v1/cases/:id/decisions`. */
export interface DecisionPayload {
  status: Status;
  comment: string;
}

/** Response of `POST /moderation/api/v1/cases/:id/decisions`. */
export interface DecisionCreatedResponse {
  id: string;
}

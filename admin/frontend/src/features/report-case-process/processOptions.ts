import { type Status, type WorkflowState } from '~/models';

/** A moderation decision the administrator can take on a case. */
export interface ProcessOption {
  status: Extract<Status, 'RESOURCE_KEPT' | 'QUARANTINE' | 'RESOURCE_DELETED'>;
  /** Tab of the reports list to land on once processed. */
  tab: WorkflowState;
  /** False until the option ships (quarantine). */
  enabled: boolean;
}

/** Every decision, in the order of the Figma design. */
export const PROCESS_OPTIONS: readonly ProcessOption[] = [
  { status: 'RESOURCE_KEPT', tab: 'SETTLE', enabled: true },
  // A quarantined case still has to be processed: it stays in the pending tab
  { status: 'QUARANTINE', tab: 'PENDING', enabled: false },
  { status: 'RESOURCE_DELETED', tab: 'SETTLE', enabled: true },
];

/** Decisions still available on a case with this status. */
export function processOptions(status: Status): ProcessOption[] {
  // A deleted content cannot be processed again
  if (status === 'RESOURCE_DELETED') return [];

  return PROCESS_OPTIONS.filter(
    (option) =>
      option.enabled &&
      // A kept content can still be deleted (or quarantined), not kept again
      !(status === 'RESOURCE_KEPT' && option.status === 'RESOURCE_KEPT'),
  );
}

/** Whether a case with this status can still be processed. */
export function canProcess(status: Status): boolean {
  return processOptions(status).length > 0;
}

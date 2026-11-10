import { type Status } from './shared';

export interface Decision {
  id: string;
  userId: string;
  displayName: string;
  status: Status;
  comment: string;
  /** When the decision was taken. */
  createdAt: string;
}

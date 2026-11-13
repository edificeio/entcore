import { type Reason } from './shared';

export interface Report {
  id: string;
  reporterId: string;
  displayName: string;
  reason: Reason;
  comment: string;
  createdAt: string;
}

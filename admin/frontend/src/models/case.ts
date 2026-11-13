import { type Decision } from './decision.ts';
import { type Report } from './report';
import { type JsonObject, type Reason, type Status } from './shared';

export interface Case {
  id: string;
  module: string;
  type: string;
  path: string;
  reason: Reason;
  createdAt: string;
  modifiedAt: string;
  status: Status;
  title: string;
  authorId: string;
  authorDisplayName: string;
  authorFirstName: string;
  authorLastName: string;
  references: string;
  body: JsonObject;
  reports: Report[];
  decisions?: Decision[];
}

import { type Decision } from './decision.ts';
import { type Report } from './report';
import { type JsonObject, type Reason, type Status } from './shared';

export interface Case {
  id: string;
  module: string;
  type: string;
  resource: string;
  subResource?: string;
  reason: Reason;
  createAt: string;
  modifiedAt: string;
  status: Status;
  title: string;
  senderId: string;
  senderDisplayName: string;
  senderFirstName: string;
  senderLastName: string;
  references: string;
  body: JsonObject;
  reports: Report[];
  decisions?: Decision[];
}

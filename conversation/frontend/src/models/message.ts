import { Attachment } from './attachment';
import { Recipients } from './recipient';
import { User } from './user';

export type Message = {
  id: string;
  date: number;
  subject: string;
  from: User;
  to: Recipients;
  cc: Recipients;
  cci?: Recipients;
  attachments: Attachment[];
  state: 'DRAFT' | 'SENT';
  body: string;
  language: string;
  folder_id: string;
  parent_id: string;
  thread_id: string;
  unread: boolean;
  trashed: boolean;
  responded: boolean;
  forwarded: boolean;
};

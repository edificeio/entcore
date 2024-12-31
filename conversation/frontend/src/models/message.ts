import { Attachment } from './attachment';
import { Recipients } from './recipient';
import { User } from './user';

type MessageBase = {
  id: string;
  date: number;
  subject: string;
  from: User;
  to: Recipients;
  cc: Recipients;
  cci?: Recipients;
  state: 'DRAFT' | 'SENT';
  unread: boolean;
  trashed: boolean;
  responded: boolean;
  forwarded: boolean;
};

export type MessageMetadata = MessageBase & {
  hasAttachments: boolean;
};

export type Message = MessageBase & {
  attachments: Attachment[];
  body: string;
  language: string;
  folder_id: string;
  parent_id: string;
  thread_id: string;
};

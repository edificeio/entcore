import { Attachment } from './attachment';
import { Recipients } from './recipient';
import { User } from './user';

export interface MessageBase {
  id: string;
  date?: number;
  subject: string;
  from: User;
  to: Recipients;
  cc: Recipients;
  cci?: Recipients;
  state?: 'DRAFT' | 'SENT' | 'RECALL';
  unread?: boolean;
  trashed?: boolean;
  response?: boolean;
  forwarded?: boolean;
}

export interface MessageMetadata extends MessageBase {
  hasAttachment: boolean;
}

export interface Message extends MessageBase {
  attachments: Attachment[];
  body: string;
  language?: string;
  folder_id?: string;
  parent_id?: string;
  thread_id?: string;
  original_format_exists: boolean;
}

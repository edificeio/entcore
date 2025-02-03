export const SYSTEM_FOLDER_ID = {
  INBOX: 'inbox',
  OUTBOX: 'outbox',
  DRAFT: 'draft',
  TRASH: 'trash',
} as const;
export const SYSTEM_FOLDER_IDS = Object.values(SYSTEM_FOLDER_ID) as string[];
export type SystemFolder =
  (typeof SYSTEM_FOLDER_ID)[keyof typeof SYSTEM_FOLDER_ID];

export type Folder = {
  id: string;
  parent_id: string | null;
  name: string;
  depth: number;
  subFolders?: Folder[];
  nbMessages: number;
  nbUnread: number;
};

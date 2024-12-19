export type Folder = {
  id: string;
  name: string;
  depth: number;
  subFolders?: Folder[];
  nbMessages: number;
  nbUnread: number;
  trashed: boolean;
};

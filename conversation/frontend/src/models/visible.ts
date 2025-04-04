export type VisibleType = 'User' | 'Group' | 'ShareBookmark';

export type Visible = {
  id: string;
  displayName: string;
  profile: string;
  nbUsers?: number;
  groupType?: string;
  usedIn: ('TO' | 'CC' | 'CCI')[];
  type: VisibleType;
  children?: { id: string; displayName: string }[];
  relatives?: { id: string; displayName: string }[];
};

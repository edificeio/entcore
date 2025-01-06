import { Group } from './group';
import { User } from './user';

export type Recipients = {
  users: User[];
  groups: Group[];
};

export type RecipientIds = {
  users: string[];
  groups: string[];
};

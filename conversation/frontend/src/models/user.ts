import { Structure } from './structure';

export type User = {
  id: string;
  displayName: string;
  profile: string;
  displayStructure?: Structure;
};

import { type Case } from './case';

export interface CaseSearch {
  totalCount: number;
  page: number;
  size: number;
  items?: Case[];
}

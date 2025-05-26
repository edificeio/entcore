export interface Category {
  id: CategoryId;
  name: string;
}

export type CategoryId =
  | 'all'
  | 'favorites'
  | 'communication'
  | 'pedagogy'
  | 'organisation'
  | 'connector'
  | 'search'
  | 'none';

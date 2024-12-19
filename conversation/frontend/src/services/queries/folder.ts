import { queryOptions } from '@tanstack/react-query';
import { loadFoldersTree } from '../api/folder';

const TREE_DEPTH = 2;

/**
 * Query options to load folders tree.
 * @returns queryOptions with key, fn, and selected data
 */
export const foldersTreeQueryOptions = () => {
  return queryOptions({
    queryKey: ['foldersTree'],
    queryFn: () => loadFoldersTree(TREE_DEPTH),
    staleTime: Infinity,
  });
};

import { queryOptions } from '@tanstack/react-query';
import { folderService } from '..';

const TREE_DEPTH = 2;

/**
 * Query options to load folders tree.
 * @returns queryOptions with key, fn, and selected data
 */
export const foldersTreeQueryOptions = () => {
  return queryOptions({
    queryKey: ['foldersTree'],
    queryFn: () => folderService.loadTree(TREE_DEPTH),
    staleTime: Infinity,
  });
};

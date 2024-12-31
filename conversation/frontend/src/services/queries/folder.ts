import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { folderService } from '..';
import { Folder } from '~/models';

/**
 * Folder Query Options Factory.
 */
export const folderQueryOptions = {
  base: ['folder'] as const,

  loadFoldersTree() {
    const TREE_DEPTH = 2;
    return queryOptions({
      queryKey: [...folderQueryOptions.base, 'tree'] as const,
      queryFn: () => folderService.loadTree(TREE_DEPTH),
      staleTime: 5000,
    });
  },

  loadMessages(
    folderId: string,
    options?: {
      /** (optional) Search string */
      search?: string;
      /** (optional) 0-based Page number */
      page?: number;
      /** (optional) Page size */
      pageSize?: number;
      /** (optional) Load un/read message only ? */
      unread?: boolean;
    },
  ) {
    return queryOptions({
      queryKey: [
        ...folderQueryOptions.base,
        folderId,
        'messages',
        options,
      ] as const,
      queryFn: () => folderService.loadMessages(folderId, options),
      staleTime: 5000,
    });
  },
};

/*
 * All queries and mutations
 */
export const useFoldersTree = () => {
  return useQuery(folderQueryOptions.loadFoldersTree());
};

export const useFolderMessages = (
  folderId: string,
  options?: {
    /** (optional) Search string */
    search?: string;
    /** (optional) 0-based Page number */
    page?: number;
    /** (optional) Page size */
    pageSize?: number;
    /** (optional) Load un/read message only ? */
    unread?: boolean;
  },
) => {
  return useQuery(folderQueryOptions.loadMessages(folderId, options));
};

export const useCreateFolder = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: { name: string; parentID?: string }) =>
      folderService.create(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: folderQueryOptions.loadFoldersTree().queryKey,
      });
    },
  });
};

export const useUpdateFolder = () => {
  //  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id: folderId, name }: Pick<Folder, 'id' | 'name'>) =>
      folderService.rename(folderId, name),
    onSuccess: (_data, _context) => {
      // TODO live update
    },
  });
};

export const useTrashFolder = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: Pick<Folder, 'id'>) => folderService.trash(id),
    onSuccess: (_data, _context) => {
      queryClient.invalidateQueries({
        queryKey: folderQueryOptions.loadFoldersTree().queryKey,
      });
    },
  });
};

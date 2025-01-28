import {
  infiniteQueryOptions,
  queryOptions,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { folderService, recursiveSearch } from '..';
import { Folder, MessageMetadata } from '~/models';
import {
  useAppActions,
  useFoldersTree as useFoldersTreeFromStore,
} from '~/store';
import { useSearchParams } from 'react-router-dom';

/**
 * Provides query options for folder-related operations.
 */
export const folderQueryOptions = {
  /**
   * Base query key for folder-related queries.
   */
  base: ['folder'] as const,

  getMessagesQuerykey(
    folderId: string,
    options?: { search?: string; unread?: boolean },
  ) {
    return [...folderQueryOptions.base, folderId, 'messages', options] as const;
  },

  /**
   * Retrieves the folder tree with a predefined depth.
   *
   * @returns Query options for fetching the folder tree.
   */
  getFoldersTree() {
    const TREE_DEPTH = 2;
    return queryOptions({
      queryKey: [...folderQueryOptions.base, 'tree'] as const,
      queryFn: () => folderService.getTree(TREE_DEPTH),
      staleTime: 5 * 60 * 1000, // 5 minutes
    });
  },

  /**
   * Retrieves the count of messages in a specific folder.
   *
   * @param folderId - The ID of the folder.
   * @param options - Optional parameters to filter the count.
   * @param options.unread - If true, only count unread messages.
   * @returns Query options for fetching the message count.
   */
  getMessagesCount(
    folderId: string,
    options?: {
      /** (optional) Load un/read message only ? */
      unread?: boolean;
    },
  ) {
    return queryOptions({
      queryKey: [
        ...folderQueryOptions.base,
        folderId,
        'count',
        options,
      ] as const,
      queryFn: () => folderService.getCount(folderId, options?.unread),
      staleTime: 5 * 60 * 1000, // 5 minutes
    });
  },

  /**
   * Retrieves messages from a specific folder with pagination support.
   *
   * @param folderId - The ID of the folder.
   * @param options - Optional parameters to filter the messages.
   * @param options.search - A search string to filter messages.
   * @param options.unread - If true, only load unread messages.
   * @returns Query options for fetching messages with pagination.
   */
  getMessages(
    folderId: string,
    options?: {
      /** (optional) Search string */
      search?: string;
      /** (optional) Load un/read message only ? */
      unread?: boolean;
    },
  ) {
    const pageSize = 20;

    return infiniteQueryOptions({
      queryKey: this.getMessagesQuerykey(folderId, options),
      queryFn: ({ pageParam = 0 }) => {
        return folderService.getMessages(folderId, {
          ...options,
          page: pageParam,
          pageSize,
        });
      },
      staleTime: 5 * 60 * 1000, // 5 minutes
      initialPageParam: 0,
      getNextPageParam: (lastPage: any, _allPages: any, lastPageParam: any) => {
        if (
          (pageSize && lastPage?.length < pageSize) ||
          (!pageSize && lastPage?.length === 0)
        ) {
          return undefined;
        }
        return lastPageParam + 1;
      },
    });
  },
};

/**
 * Hook to fetch messages from a specific folder with pagination support.
 *
 * @param folderId - The ID of the folder.
 * @returns Query result for fetching messages with pagination.
 */
export const useFolderMessages = (folderId: string) => {
  const [searchParams] = useSearchParams();
  const search = searchParams.get('search');
  const filterUnread = searchParams.get('unread');

  const query = useInfiniteQuery(
    folderQueryOptions.getMessages(folderId, {
      search: search && search !== '' ? search : undefined,
      unread: filterUnread ? true : undefined,
    }),
  );
  return {
    ...query,
    messages: query.data?.pages.flatMap((page) => page) as MessageMetadata[],
  };
};

/**
 * Hook to fetch the count of messages in a specific folder.
 *
 * @param folderId - The ID of the folder.
 * @param options - Optional parameters to filter the count.
 * @param options.unread - If true, only count unread messages.
 * @returns Query result for fetching the message count.
 */
export const useMessagesCount = (
  folderId: string,
  options?: {
    /** (optional) Load un/read message only ? */
    unread?: boolean;
  },
) => {
  return useQuery(folderQueryOptions.getMessagesCount(folderId, options));
};

/**
 * Hook to create a new folder.
 *
 * @returns Mutation result for creating a new folder.
 */
export const useCreateFolder = () => {
  const queryClient = useQueryClient();
  const foldersTree = useFoldersTreeFromStore();
  const { setFoldersTree } = useAppActions();

  return useMutation({
    mutationFn: (payload: { name: string; parentId?: string }) =>
      folderService.create(payload),
    onSuccess: async ({ id }, { name, parentId }) => {
      if (parentId) {
        // try optimistic update...
        const parent = recursiveSearch(parentId, foldersTree);
        if (!parent) {
          // ...or fallback to full refreshing the whole folders tree.
          return queryClient.invalidateQueries(
            folderQueryOptions.getFoldersTree(),
          );
        } else {
          if (!parent.subFolders) parent.subFolders = [];
          parent.subFolders.push({
            id,
            depth: 2,
            name,
            nbMessages: 0,
            nbUnread: 0,
            trashed: false,
          });
        }
      } else {
        // optimistic update : push new folder at root level (depth=1)
        foldersTree.push({
          id,
          depth: 1,
          name,
          nbMessages: 0,
          nbUnread: 0,
          trashed: false,
        });
      }
      setFoldersTree(foldersTree);
    },
  });
};

/**
 * Hook to update an existing folder.
 *
 * @returns Mutation result for updating a folder.
 */
export const useUpdateFolder = () => {
  //  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id: folderId, name }: Pick<Folder, 'id' | 'name'>) =>
      folderService.rename(folderId, name),
    onSuccess: (_data, _context) => {
      // TODO optimistic update
    },
  });
};

/**
 * Hook to move a folder to trash.
 *
 * @returns Mutation result for trashing a folder.
 */
export const useTrashFolder = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: Pick<Folder, 'id'>) => folderService.trash(id),
    onSuccess: (_data, _context) => {
      queryClient.invalidateQueries({
        queryKey: folderQueryOptions.getFoldersTree().queryKey,
      });
    },
  });
};

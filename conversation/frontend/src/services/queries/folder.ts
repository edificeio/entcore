import {
  FetchNextPageOptions,
  InfiniteData,
  infiniteQueryOptions,
  queryOptions,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Folder, MessageMetadata } from '~/models';
import { useConfig } from '~/store';
import { folderService, searchFolder } from '..';
import { queryClient } from '~/providers';

export const PAGE_SIZE = 20;

export const folderQueryKeys = {
  all: () => ['folder'] as const,
  messages: (
    folderId?: string,
    options?: { search?: string; unread?: boolean },
  ) => {
    const queryKey: any = [...folderQueryKeys.all(), 'messages'];
    if (folderId) queryKey.push(folderId);
    if (options) queryKey.push(options);
    return queryKey;
  },
  count: (folderId?: string) =>
    [...folderQueryKeys.all(), 'count', folderId] as const,
  tree: () => [...folderQueryKeys.all(), 'tree'] as const,
};

/**
 * Provides query options for folder-related operations.
 */
export const folderQueryOptions = {
  /**
   * Retrieves the folder tree with a predefined depth.
   * Limit specified depth to 5, whatever.
   *
   * @returns Query options for fetching the folder tree.
   */
  getFoldersTree(maxDepth?: number) {
    const TREE_DEPTH =
      typeof maxDepth === 'number' ? Math.min(5, Math.round(maxDepth)) : 3;
    return queryOptions({
      queryKey: folderQueryKeys.tree(),
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
      queryKey: folderQueryKeys.count(folderId),
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
    options: {
      /** (optional) Search string */
      search?: string;
      /** (optional) Load un/read message only ? */
      unread?: boolean;
    },
  ) {
    return infiniteQueryOptions({
      queryKey: folderQueryKeys.messages(folderId, options),
      queryFn: ({ pageParam = 0 }) => {
        return folderService.getMessages(folderId, {
          ...options,
          page: pageParam,
          pageSize: PAGE_SIZE,
        });
      },
      staleTime: 5 * 60 * 1000, // 5 minutes
      initialPageParam: 0,
      getNextPageParam: (
        _lastPage: MessageMetadata[],
        allPages: MessageMetadata[][],
        lastPageParam: number,
      ) => {
        const totalMessages = allPages[0]?.[0]?.count;
        const loadedMessages = allPages.flat().length;
        if (loadedMessages < totalMessages) {
          return lastPageParam + 1;
        }
        return undefined;
      },
    });
  },
};

/**
 * Hook to fetch the folder tree.
 *
 * @returns Query result for fetching the folder tree.
 */
export const useFoldersTree = () => {
  const { maxDepth } = useConfig();
  return useQuery(folderQueryOptions.getFoldersTree(maxDepth));
};

/**
 * Hook providing utility functions for working with folders.
 *
 * This hook offers helper functions related to folder operations, such as
 * retrieving a folder name based on its ID, or updating its date in queries data.
 *
 * @returns An object containing folder utility methods.
 */
export const useFolderUtils = () => {
  const { data: foldersTree } = useFoldersTree();
  const queryClient = useQueryClient();

  /** Recursively search the folders tree for a folder's name, knowing its id. */
  const getFolderNameById = useCallback(
    (id: string) => {
      if (!foldersTree) return 'Unknown';

      const result = searchFolder(id, foldersTree);
      return result?.folder.name;
    },
    [foldersTree],
  );

  /** Update some messages metadata in the list of a folder's messages. */
  function updateFolderMessagesQueryCache(
    folderId: string,
    updater: (oldMessage: MessageMetadata) => MessageMetadata,
    reOrder: boolean = false,
  ) {
    queryClient.setQueriesData<InfiniteData<MessageMetadata[]>>(
      { queryKey: folderQueryKeys.messages(folderId) },
      (oldData) => {
        if (!oldData?.pages) return undefined;

        if (reOrder) {
          const allMessages = oldData.pages.flatMap((page) => page);
          const updatedMessages = allMessages.map(updater);
          updatedMessages.sort((a, b) => (b.date || 0) - (a.date || 0));
          const pages = [],
            pageSize = oldData?.pages[0].length || PAGE_SIZE;
          for (let i = 0; i < allMessages.length; i += pageSize) {
            pages.push(updatedMessages.slice(i, i + pageSize));
          }
          return {
            ...oldData,
            pages,
          };
        }

        return {
          ...oldData,
          pages: oldData.pages.map((page) => page.map(updater)),
        };
      },
    );
  }

  return { getFolderNameById, updateFolderMessagesQueryCache };
};

/**
 * Hook to fetch messages from a specific folder with pagination support.
 *
 * @param folderId - The ID of the folder.
 * @param enabled - If false, only returns cached data without triggering a new request
 * @returns Query result for fetching messages with pagination.
 */
export const useFolderMessages = (folderId: string, enabled = true) => {
  const [searchParams] = useSearchParams();
  const search = searchParams.get('search');
  const filterUnread = searchParams.get('unread');

  const queryOptions = folderQueryOptions.getMessages(folderId, {
    search: search && search !== '' ? search : undefined,
    unread: filterUnread ? true : undefined,
  });
  queryOptions.enabled = enabled;

  const query = useInfiniteQuery(queryOptions);

  const fetchNextPageOrInvalidateCachedPageWithDeletedMessages = useCallback(
    async (options?: FetchNextPageOptions) => {
      const pages = query.data?.pages;
      const totalMessagesCount = pages?.[0]?.[0]?.count || 0;
      const totalPageCount = Math.ceil(totalMessagesCount / PAGE_SIZE);
      const hasDeletedMessages = pages?.some((page, index) => {
        const pageLoadedMessagesCount = page.length;
        const noMorePages = index >= totalPageCount - 1;
        if (noMorePages) return false;
        if (pageLoadedMessagesCount < PAGE_SIZE) return true;
      });
      if (hasDeletedMessages) {
        await queryClient.invalidateQueries({
          queryKey: folderQueryOptions.getMessagesQuerykey(folderId, {}),
        });
      } else {
        return query.fetchNextPage(options);
      }
    },
    [query],
  );

  return {
    ...query,
    fetchNextPage: fetchNextPageOrInvalidateCachedPageWithDeletedMessages,

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
  const foldersTreeQuery = useFoldersTree();

  return useMutation({
    mutationFn: (payload: { name: string; parentId?: string }) =>
      folderService.create(payload),
    onSuccess: async ({ id }, { name, parentId }) => {
      const foldersTree = foldersTreeQuery.data;
      do {
        if (!foldersTree) break;

        const newFolder = {
          id,
          name,
          nbMessages: 0,
          nbUnread: 0,
        };
        if (parentId) {
          // Look for the parent folder in the tree.
          const parent = searchFolder(parentId, foldersTree);
          if (!parent?.folder) break;

          if (!parent.folder.subFolders) {
            parent.folder.subFolders = [];
          }
          // Update parent folder data.
          parent.folder.subFolders.push({
            parent_id: parentId,
            depth: 2,
            ...newFolder,
          });
        } else {
          // Push new folder at root level (depth=1)
          foldersTree.push({
            parent_id: null,
            depth: 1,
            ...newFolder,
          });
        }
        // sort the foldersTree by name
        foldersTree.sort((a, b) => a.name.localeCompare(b.name));

        queryClient.setQueryData(folderQueryKeys.tree(), [...foldersTree]);
      } while (false);

      // ...or full refresh the whole folders tree as a fallback.
      return queryClient.invalidateQueries(folderQueryOptions.getFoldersTree());
    },
  });
};

/**
 * Hook to rename an existing folder.
 *
 * @returns Mutation result for renaming a folder.
 */
export const useRenameFolder = () => {
  const queryClient = useQueryClient();
  const foldersTreeQuery = useFoldersTree();

  return useMutation({
    mutationFn: ({ id: folderId, name }: Pick<Folder, 'id' | 'name'>) =>
      folderService.rename(folderId, name),
    onSuccess: async (_data, { id, name }) => {
      const foldersTree = foldersTreeQuery.data;
      // Try optimistic update...
      do {
        if (!foldersTree) break;
        // Look for the parent folder in the tree.
        const found = searchFolder(id, foldersTree);
        if (!found?.folder) break;

        found.folder.name = name;

        // Optimistic update
        queryClient.setQueryData(folderQueryKeys.tree(), [...foldersTree]);

        return;
        // eslint-disable-next-line no-constant-condition
      } while (false);

      // ...or full refresh the whole folders tree as a fallback.
      return queryClient.invalidateQueries(folderQueryOptions.getFoldersTree());
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
  const foldersTreeQuery = useFoldersTree();

  return useMutation({
    mutationFn: ({ id }: Pick<Folder, 'id'>) => folderService.trash(id),
    onSuccess: async (_data, { id }) => {
      const foldersTree = foldersTreeQuery.data;

      // Try optimistic update...
      do {
        if (!foldersTree) break;

        const found = searchFolder(id, foldersTree);
        if (!found) break;

        if (found.parent) {
          // This is a sub-folder. Remove it from its parent sub-folders list.
          found.parent.subFolders = found.parent.subFolders?.filter(
            (f) => f.id !== id,
          );
          // Optimistic update
          queryClient.setQueryData(folderQueryKeys.tree(), [...foldersTree]);
        } else {
          // Optimistic update
          queryClient.setQueryData(
            folderQueryKeys.tree(),
            foldersTree.filter((f) => f.id !== id),
          );
        }

        return;
        // eslint-disable-next-line no-constant-condition
      } while (false);

      // ...or full refresh the whole folders tree as a fallback.
      return queryClient.refetchQueries({
        queryKey: folderQueryKeys.tree(),
      });
    },
  });
};

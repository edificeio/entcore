import {
  InfiniteData,
  infiniteQueryOptions,
  queryOptions,
  useInfiniteQuery,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import { Folder, MessageMetadata } from '~/models';
import { folderService, searchFolder } from '..';
import { useCallback } from 'react';
import { useConfig } from '~/store';

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
    options: { search?: string; unread?: boolean },
  ) {
    return [...folderQueryOptions.base, folderId, 'messages', options] as const;
  },

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
    options: {
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
  function updateFolderMessagesQueryData(
    folderId: string,
    updater: (oldMessage: MessageMetadata) => MessageMetadata,
  ) {
    queryClient.setQueriesData<InfiniteData<MessageMetadata[]>>(
      { queryKey: folderQueryOptions.getMessagesQuerykey(folderId, {}) },
      (oldData) => {
        if (!oldData?.pages) return undefined;
        return {
          ...oldData,
          pages: oldData.pages.map((page) => page.map(updater)),
        };
      },
    );
  }

  return { getFolderNameById, updateFolderMessagesQueryData };
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
  const foldersTreeQuery = useFoldersTree();

  return useMutation({
    mutationFn: (payload: { name: string; parentId?: string }) =>
      folderService.create(payload),
    onSuccess: async ({ id }, { name, parentId }) => {
      const foldersTree = foldersTreeQuery.data;
      // Try optimistic update...
      do {
        if (!foldersTree) break;

        if (parentId) {
          // Look for the parent folder in the tree.
          const parent = searchFolder(parentId, foldersTree);
          if (!parent?.folder) break;

          if (!parent.folder.subFolders) {
            parent.folder.subFolders = [];
          }
          // Update parent folder data.
          parent.folder.subFolders.push({
            id,
            parent_id: parentId,
            depth: 2,
            name,
            nbMessages: 0,
            nbUnread: 0,
          });
        } else {
          // Push new folder at root level (depth=1)
          foldersTree.push({
            id,
            parent_id: null,
            depth: 1,
            name,
            nbMessages: 0,
            nbUnread: 0,
          });

          // Optimistic update
          queryClient.setQueryData(
            folderQueryOptions.getFoldersTree().queryKey,
            [...foldersTree],
          );

          return;
        }
        // eslint-disable-next-line no-constant-condition
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
        queryClient.setQueryData(folderQueryOptions.getFoldersTree().queryKey, [
          ...foldersTree,
        ]);

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
          queryClient.setQueryData(
            folderQueryOptions.getFoldersTree().queryKey,
            [...foldersTree],
          );
        } else {
          // Optimistic update
          queryClient.setQueryData(
            folderQueryOptions.getFoldersTree().queryKey,
            foldersTree.filter((f) => f.id !== id),
          );
        }

        return;
        // eslint-disable-next-line no-constant-condition
      } while (false);

      // ...or full refresh the whole folders tree as a fallback.
      return queryClient.refetchQueries({
        queryKey: folderQueryOptions.getFoldersTree().queryKey,
      });
    },
  });
};

export const useUpdateFolderBadgeCountLocal = () => {
  const queryClient = useQueryClient();
  const updateFolderBadgeCountLocal = (
    folderId: string,
    countDelta: number,
  ) => {
    if (folderId === 'inbox') {
      // Update inbox count unread
      queryClient.setQueryData(
        ['folder', 'inbox', 'count', { unread: true }],
        ({ count }: { count: number }) => {
          return { count: count + countDelta };
        },
      );

      // Update conversation navbar count unread
      queryClient.setQueryData(
        ['conversation-navbar-count'],
        ({ count }: { count: number }) => {
          return { count: count + countDelta };
        },
      );
    } else if (folderId === 'draft') {
      // Update draft count
      queryClient.setQueryData(
        ['folder', 'draft', 'count', null],
        ({ count }: { count: number }) => {
          return { count: count + countDelta };
        },
      );
    } else if (!['inbox', 'trash', 'draft', 'outbox'].includes(folderId)) {
      // Update custom folder count unread
      queryClient.setQueryData(['folder', 'tree'], (folders: Folder[]) => {
        // go trow the folder tree to find the folder to update
        const result = searchFolder(folderId, folders);
        if (!result?.parent) {
          return folders.map((folder) => {
            if (folder.id === folderId) {
              return { ...folder, nbUnread: folder.nbUnread + countDelta };
            }
            return folder;
          });
        } else if (result?.folder) {
          result.folder = {
            ...result.folder,
            nbUnread: result.folder.nbUnread + countDelta,
          };
          return [...folders];
        }
      });
    }
  };
  return { updateFolderBadgeCountLocal };
};

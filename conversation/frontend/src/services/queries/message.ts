import {
  InfiniteData,
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { folderQueryOptions, messageService } from '..';
import { useParams, useSearchParams } from 'react-router-dom';
import { Message } from '~/models';

/**
 * Message Query Options Factory.
 */
export const messageQueryOptions = {
  base: ['message'] as const,
  /**
   * Generates query options for fetching a message by its ID.
   * @param messageId - The ID of the message to fetch.
   * @returns Query options for fetching the message.
   */
  getById(messageId: string) {
    return queryOptions({
      queryKey: [...messageQueryOptions.base, messageId] as const,
      queryFn: () => messageService.getById(messageId),
      staleTime: 5000,
    });
  },
};

/**
 * Hook to fetch a message by its ID.
 * @param messageId - The ID of the message to fetch.
 * @returns Query result for the message.
 */
export const useMessage = (messageId: string) => {
  return useQuery(messageQueryOptions.getById(messageId));
};

/**
 * Hook to toggle the unread status of a message.
 * @param unread - The unread status to set.
 * @returns Mutation result for toggling the unread status.
 */
const useToggleUnread = (unread: boolean) => {
  const { folderId } = useParams() as { folderId: string };
  const [searchParams] = useSearchParams();
  const search = searchParams.get('search');
  const unreadFilter = searchParams.get('unread');
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.toggleUnread(id, unread),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      queryClient.setQueryData(
        [
          'folder',
          folderId,
          'count',
          folderId !== 'trash' ? { unread: true } : null,
        ],
        ({ count }: { count: number }) => {
          if (count !== undefined) {
            return {
              count: count + (unread ? messageIds.length : -messageIds.length),
            };
          }
          return { count: unread ? messageIds.length : 0 };
        },
      );
      queryClient.setQueryData(
        ['conversation-navbar-count'],
        ({ count }: { count: number }) => {
          if (count !== undefined) {
            return {
              count: count + (unread ? messageIds.length : -messageIds.length),
            };
          }
          return { count: unread ? messageIds.length : 0 };
        },
      );
      queryClient.setQueryData(
        folderQueryOptions.getMessagesQuerykey(folderId, {
          search: search === '' ? undefined : search || undefined,
          unread: !unreadFilter ? undefined : true,
        }),
        (data: InfiniteData<Message>) => {
          data.pages.forEach((page: any) => {
            page.forEach((message: any) => {
              if (messageIds.includes(message.id)) {
                message.unread = unread;
              }
            });
          });
          return data;
        },
      );
      messageIds.map((messageId) => {
        queryClient.setQueryData(
          messageQueryOptions.getById(messageId).queryKey,
          (message: Message | undefined) => {
            return message ? { ...message, unread } : undefined;
          },
        );
      });
    },
  });
};

/**
 * Hook to mark a message as read.
 * @returns Mutation result for marking the message as read.
 */
export const useMarkRead = () => {
  return useToggleUnread(false);
};

/**
 * Hook to mark a message as unread.
 * @returns Mutation result for marking the message as unread.
 */
export const useMarkUnread = () => {
  return useToggleUnread(true);
};

/**
 * Hook to move a message to the trash folder.
 * @returns Mutation result for moving the message to the trash.
 */
export const useTrashMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.moveToFolder('trash', id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

/**
 * Hook to restore a message from the trash.
 * @returns Mutation result for restoring the message.
 */
export const useRestoreMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.restore(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

/**
 * Hook to delete a message.
 * @returns Mutation result for deleting the message.
 */
export const useDeleteMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.delete(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

/**
 * Hook to move a message to a different folder.
 * @returns Mutation result for moving the message.
 */
export const useMoveMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      folderId,
      id,
    }: {
      folderId: string;
      id: string | string[];
    }) => messageService.moveToFolder(folderId, id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryOptions.getById(messageId).queryKey,
        });
      });
    },
  });
};

/**
 * Hook to create a draft message.
 * @returns Mutation result for creating the draft.
 */
export const useCreateDraft = () => {
  return useMutation({
    mutationFn: ({
      payload,
    }: {
      payload: {
        subject?: string;
        body?: string;
        to?: string[];
        cc?: string[];
        cci?: string[];
      };
    }) => messageService.createDraft(payload),
  });
};

/**
 * Hook to update a draft message.
 * @returns Mutation result for updating the draft.
 */
export const useUpdateDraft = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      draftId,
      payload,
    }: {
      draftId: string;
      payload: {
        subject?: string;
        body?: string;
        to?: string[];
        cc?: string[];
        cci?: string[];
      };
    }) => messageService.updateDraft(draftId, payload),
    onSuccess: (_data, { draftId }) => {
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(draftId).queryKey,
      });
    },
  });
};

/**
 * Hook to send a draft message.
 * @returns Mutation result for sending the draft.
 */
export const useSendDraft = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      draftId,
      payload,
    }: {
      draftId: string;
      payload?: {
        subject?: string;
        body?: string;
        to?: string[];
        cc?: string[];
        cci?: string[];
      };
    }) => messageService.send(draftId, payload),
    onSuccess: (_data /*, { draftId }*/) => {
      // TODO optimistic update ?
      queryClient.invalidateQueries({
        queryKey: folderQueryOptions.getFoldersTree().queryKey,
      });
    },
  });
};

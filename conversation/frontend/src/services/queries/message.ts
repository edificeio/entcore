import { useToast } from '@edifice.io/react';
import {
  InfiniteData,
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useParams, useSearchParams } from 'react-router-dom';
import { Message, MessageBase, MessageMetadata } from '~/models';
import {
  folderQueryOptions,
  messageService,
  useUpdateFolderBadgeCountLocal,
} from '..';
const appCodeName = 'conversation';
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
  const result = useQuery(messageQueryOptions.getById(messageId));
  const queryClient = useQueryClient();
  const { folderId } = useParams() as { folderId: string };
  const [searchParams] = useSearchParams();
  const search = searchParams.get('search');
  const unreadFilter = searchParams.get('unread');

  if (result.isSuccess) {
    queryClient.setQueryData(
      folderQueryOptions.getMessagesQuerykey(folderId, {
        search: search === '' ? undefined : search || undefined,
        unread: !unreadFilter ? undefined : true,
      }),
      (data: InfiniteData<MessageMetadata>) => {
        if (data?.pages) {
          data.pages.forEach((page: any) => {
            page.forEach((message: MessageMetadata) => {
              if (result.data.id === message.id) {
                message.unread = false;
              }
            });
          });
        }
        return data;
      },
    );
  }
  return result;
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
  const { updateFolderBadgeCountLocal } = useUpdateFolderBadgeCountLocal();

  return useMutation({
    mutationFn: ({ messages }: { messages: MessageBase[] }) =>
      messageService.toggleUnread(
        messages.map((m) => m.id),
        unread,
      ),
    onSuccess: (_data, { messages: message }) => {
      if (!Array.isArray(message)) {
        message = [message];
      }
      const messageIds = message.map((m) => m.id);

      if (folderId !== 'draft') {
        const countMessageUpdated = message.filter(
          (m) => m.unread !== unread,
        ).length;
        // Update the unread count in the folder except for the draft folder wich count all messages and not only unread
        updateFolderBadgeCountLocal(
          folderId,
          unread ? countMessageUpdated : -countMessageUpdated,
        );
      }

      // Update the message unread status in the list
      queryClient.setQueryData(
        folderQueryOptions.getMessagesQuerykey(folderId, {
          search: search === '' ? undefined : search || undefined,
          unread: !unreadFilter ? undefined : true,
        }),
        (data: InfiniteData<MessageMetadata>) => {
          data.pages.forEach((page: any) => {
            page.forEach((msg: MessageMetadata) => {
              if (messageIds.includes(msg.id)) {
                msg.unread = unread;
              }
            });
          });
          return data;
        },
      );

      // Update the message unread status in the message details
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
  const { folderId } = useParams() as { folderId: string };
  const [searchParams] = useSearchParams();
  const search = searchParams.get('search');
  const unreadFilter = searchParams.get('unread');
  const queryClient = useQueryClient();
  const { t } = useTranslation(appCodeName);
  const toast = useToast();
  const { updateFolderBadgeCountLocal } = useUpdateFolderBadgeCountLocal();

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

      queryClient.invalidateQueries({
        queryKey: ['folder', 'trash'],
      });

      let unreadTrashedCount = 0;
      // Update list message
      queryClient.setQueryData(
        folderQueryOptions.getMessagesQuerykey(folderId, {
          search: search === '' ? undefined : search || undefined,
          unread: !unreadFilter ? undefined : true,
        }),
        // Remove deleted message from pages
        (data: InfiniteData<Message[]>) => {
          if (!['trash', 'draft', 'outbox'].includes(folderId)) {
            unreadTrashedCount = data.pages.reduce((count, page) => {
              return (
                count +
                page.filter(
                  (message) =>
                    message.unread && messageIds.includes(message.id),
                ).length
              );
            }, 0);
          }

          return {
            ...data,
            pages: data.pages.map((page: Message[]) =>
              page.filter(
                (message: Message) => !messageIds.includes(message.id),
              ),
            ),
          };
        },
      );

      // Update unread inbox count
      // Update custom folder count
      if (
        !['trash', 'draft', 'outbox'].includes(folderId) &&
        unreadTrashedCount
      ) {
        updateFolderBadgeCountLocal(folderId, -unreadTrashedCount);
      }

      // Update draft count if It's a draft message
      if (folderId === 'draft') {
        updateFolderBadgeCountLocal(folderId, -messageIds.length);
      }

      // Toast
      toast.success(
        t(messageIds.length > 1 ? 'messages.trash' : 'message.trash'),
      );
    },
  });
};

/**
 * Hook to restore a message from the trash.
 * @returns Mutation result for restoring the message.
 */
export const useRestoreMessage = () => {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation(appCodeName);
  return useMutation({
    mutationFn: async ({ id }: { id: string | string[] }) =>
      messageService.restore(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;

      queryClient.invalidateQueries({
        queryKey: ['folder'],
      });

      // Toast
      toast.success(
        t(messageIds.length > 1 ? 'messages.restore' : 'message.restore'),
      );
    },
  });
};

/**
 * Hook to delete a message.
 * @returns Mutation result for deleting the message.
 */
export const useDeleteMessage = () => {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation(appCodeName);
  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.delete(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;

      queryClient.invalidateQueries({
        queryKey: ['folder', 'trash'],
      });

      toast.success(
        t(messageIds.length > 1 ? 'messages.delete' : 'message.delete'),
      );
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
    onSuccess: () => {
      // TODO optimistic update ?
      queryClient.invalidateQueries({
        queryKey: folderQueryOptions.getFoldersTree().queryKey,
      });
    },
  });
};

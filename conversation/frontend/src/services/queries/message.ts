import { odeServices } from '@edifice.io/client';
import { useEdificeClient, useToast } from '@edifice.io/react';
import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { useI18n } from '~/hooks/useI18n';
import { useMessageIdAndAction } from '~/hooks/useMessageIdAndAction';
import { Message, MessageBase } from '~/models';
import {
  baseUrl,
  createDefaultMessage,
  folderQueryKeys,
  messageService,
} from '~/services';
import { useMessageStore } from '~/store/messageStore';
import { useDeleteMessagesFromQueryCache } from './hooks/useDeleteMessageFromQueryCache';
import { useMessageListOnMutate } from './hooks/useMessageListOnMutate';
import { useToggleUnreadMessagesFromQueryCache } from './hooks/useToggleUnreadMessageFromQueryCache';
import { useUpdateFolderBadgeCountQueryCache } from './hooks/useUpdateFolderBadgeCountQueryCache';
import { invalidateQueriesWithFirstPage } from './utils';
const appCodeName = 'conversation';

export const messageQueryKeys = {
  all: () => ['message'] as const,
  config: () => [...messageQueryKeys.all(), 'config'] as const,
  byId: (messageId: string) =>
    [...messageQueryKeys.all(), 'byId', messageId] as const,
  originalFormat: (messageId: string) =>
    [...messageQueryKeys.all(), 'originalFormat', messageId] as const,
};

/**
 * Message Query Options Factory.
 */
export const messageQueryOptions = {
  /**
   * Generates query options for fetching message application configuration.
   * @returns Query options for fetching the configuration.
   */
  getConfig() {
    return queryOptions({
      queryKey: messageQueryKeys.config(),
      queryFn: (): Promise<{ 'debounce-time-to-auto-save'?: number }> =>
        odeServices.conf().getPublicConf('conversation'),
      staleTime: Infinity,
    });
  },
  /**
   * Generates query options for fetching a message by its ID.
   * @param messageId - The ID of the message to fetch.
   * @returns Query options for fetching the message.
   */
  getById(messageId: string) {
    return queryOptions({
      queryKey: messageQueryKeys.byId(messageId),
      queryFn: () => messageService.getById(messageId),
      staleTime: Infinity, // The message must be invalidated by mutations, no need to reload it twice in between.
    });
  },
  /**
   * Generates query options for fetching a message by its ID.
   * @param messageId - The ID of the message to fetch.
   * @returns Query options for fetching the message.
   */
  getOriginalFormat(messageId: string) {
    return queryOptions({
      queryKey: messageQueryKeys.originalFormat(messageId),
      queryFn: () => messageService.getOriginalFormat(messageId),
      staleTime: Infinity,
    });
  },
};

/**
 * Hook to fetch the message application configuration.
 * @returns Query result for the message application configuration.
 */
export const useConversationConfig = () => {
  return useQuery(messageQueryOptions.getConfig());
};

/**
 * Hook to fetch a message by its ID.
 * @param messageId - The ID of the message to fetch.
 * @returns Query result for the message.
 */
export const useMessageQuery = (messageId: string) => {
  const result = useQuery(messageQueryOptions.getById(messageId));
  const { currentLanguage, user, userProfile } = useEdificeClient();

  if (result.isSuccess && result.data) {
    let message: Message = result.data;
    if (message.id) {
      // Fix issue when back retrun subject and body to null
      if (message.subject === null) {
        message.subject = '';
      }
      if (message.body === null) {
        message.body = '';
      }
    } else {
      message = {
        ...createDefaultMessage(),
        language: currentLanguage,
        from: {
          id: user?.userId || '',
          displayName: user?.username || '',
          profile: (userProfile || '') as string,
        },
      };
    }
  }
  return result;
};

/**
 * Hook to toggle the unread status of a message.
 * @param unread - The unread status to set.
 * @returns Mutation result for toggling the unread status.
 */
const useToggleUnread = (unread: boolean) => {
  const { toggleUnreadMessagesFromQueryCache } =
    useToggleUnreadMessagesFromQueryCache();
  return useMutation({
    mutationFn: ({ messages }: { messages: MessageBase[] }) =>
      messageService.toggleUnread(
        messages.map((m) => m.id),
        unread,
      ),
    onMutate: ({ messages }) => {
      toggleUnreadMessagesFromQueryCache(messages, unread);
    },
    onError: (_error, { messages }) => {
      toggleUnreadMessagesFromQueryCache(messages, !unread);
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

  const queryClient = useQueryClient();
  const { t } = useTranslation(appCodeName);
  const toast = useToast();
  const { deleteMessagesFromQueryCache } = useDeleteMessagesFromQueryCache();
  const { messageListOnMutate } = useMessageListOnMutate();

  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.moveToFolder('trash', id),
    onMutate: ({ id }) => messageListOnMutate(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;

      // Invalidate message details
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryKeys.byId(messageId),
        });
      });

      // Delete messages from query cache
      deleteMessagesFromQueryCache(folderId, messageIds);

      invalidateQueriesWithFirstPage(queryClient, {
        queryKey: folderQueryKeys.messages('trash'),
      });

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
  const { deleteMessagesFromQueryCache } = useDeleteMessagesFromQueryCache();
  const { messageListOnMutate } = useMessageListOnMutate();
  const toast = useToast();
  const { t } = useTranslation(appCodeName);

  return useMutation({
    mutationFn: async ({ id }: { id: string | string[] }) =>
      messageService.restore(id),
    onMutate: ({ id }) => messageListOnMutate(id),
    onSuccess: async (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;

      // Invalidate message details
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryKeys.byId(messageId),
        });
      });

      deleteMessagesFromQueryCache('trash', messageIds);
      // Reset all queries except trash folder
      invalidateQueriesWithFirstPage(queryClient, {
        queryKey: folderQueryKeys.messages(),
        predicate: (query) => {
          const queryKey = query.queryKey as string[];
          return !queryKey.includes('trash');
        },
      });

      queryClient.invalidateQueries({
        queryKey: folderQueryKeys.tree(),
      });
      queryClient.invalidateQueries({
        queryKey: folderQueryKeys.count(),
      });

      // Toast
      toast.success(
        t(messageIds.length > 1 ? 'messages.restore' : 'message.restore'),
      );
    },
  });
};

export const useEmptyTrash = () => {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { t } = useTranslation(appCodeName);

  return useMutation({
    mutationFn: () => messageService.emptyTrash(),
    onSuccess: () => {
      invalidateQueriesWithFirstPage(queryClient, {
        queryKey: folderQueryKeys.messages('trash'),
      });

      toast.success(t('trash.empty.success'));
    },
  });
};

/**
 * Hook to delete a message.
 * @returns Mutation result for deleting the message.
 */
export const useDeleteMessage = () => {
  const toast = useToast();
  const { t } = useTranslation(appCodeName);
  const queryClient = useQueryClient();
  const { deleteMessagesFromQueryCache } = useDeleteMessagesFromQueryCache();

  const { messageListOnMutate } = useMessageListOnMutate();

  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.delete(id),
    onMutate: ({ id }) => messageListOnMutate(id),
    onSuccess: (_data, { id }) => {
      const messageIds = typeof id === 'string' ? [id] : id;

      // Invalidate message details
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryKeys.byId(messageId),
        });
      });

      deleteMessagesFromQueryCache('trash', messageIds);

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
  const { folderId: currentFolderId } = useParams() as { folderId: string };

  const { deleteMessagesFromQueryCache } = useDeleteMessagesFromQueryCache();
  const { messageListOnMutate } = useMessageListOnMutate();
  return useMutation({
    mutationFn: ({
      folderId,
      id,
    }: {
      folderId: string;
      id: string | string[];
    }) => messageService.moveToFolder(folderId, id),
    onMutate: ({ id }) => messageListOnMutate(id),
    onSuccess: (_data, { id, folderId }) => {
      const messageIds = typeof id === 'string' ? [id] : id;
      // invalidate messages details
      messageIds.forEach((messageId) => {
        queryClient.invalidateQueries({
          queryKey: messageQueryKeys.byId(messageId),
        });
      });

      // Delete messages from query cache
      if (folderId === 'inbox') {
        queryClient.invalidateQueries({
          queryKey: folderQueryKeys.messages(currentFolderId),
        });
        queryClient.invalidateQueries({
          queryKey: folderQueryKeys.messages('inbox'),
        });
      } else {
        deleteMessagesFromQueryCache(currentFolderId, messageIds);
        queryClient.invalidateQueries({
          queryKey: folderQueryKeys.messages(folderId),
        });
      }

      queryClient.invalidateQueries({
        queryKey: folderQueryKeys.tree(),
      });
      queryClient.invalidateQueries({
        queryKey: folderQueryKeys.count(),
      });
    },
  });
};

/**
 * Hook that generates a function to create a new, or update an existing, draft message.
 * When the draft message is created, the function will return a Promise of its id.
 * @returns undefined, or a Promise of the id af a newly created draft
 */
export const useCreateOrUpdateDraft = () => {
  const updateDraft = useUpdateDraft();
  const createDraft = useCreateDraft();
  const messageUpdated = useMessageStore.use.message();
  const toast = useToast();
  const { t } = useI18n();
  const { transferMessageId } = useMessageIdAndAction();

  return (withNotification = false) => {
    if (!messageUpdated) return;

    const payload = {
      subject: messageUpdated.subject,
      body: messageUpdated.body,
      to: [
        ...new Set([
          ...messageUpdated.to.users.map((u) => u.id),
          ...messageUpdated.to.groups.map((g) => g.id),
        ]),
      ],
      cc: [
        ...new Set([
          ...messageUpdated.cc.users.map((u) => u.id),
          ...messageUpdated.cc.groups.map((g) => g.id),
        ]),
      ],
      cci: [
        ...new Set([
          ...(messageUpdated.cci?.users.map((u) => u.id) ?? []),
          ...(messageUpdated.cci?.groups?.map((g) => g.id) ?? []),
        ]),
      ],
    };

    if (messageUpdated.id && messageUpdated.state === 'DRAFT') {
      return updateDraft.mutateAsync(
        {
          draftId: messageUpdated.id,
          payload,
        },
        {
          onSuccess: () => {
            if (withNotification) {
              toast.success(t('message.draft.saved'));
            }
          },
        },
      );
    } else {
      return createDraft.mutateAsync(
        {
          payload,
          inReplyToId: messageUpdated.parent_id,
        },
        {
          onSuccess: async ({ id }) => {
            if (transferMessageId) {
              // Forward the message to the new draft (copy attachments)
              await messageService.forward(id, transferMessageId);
            }
            if (withNotification) {
              toast.success(t('message.draft.saved'));
            }

            // Update the URL to point to the draft message
            // We can't use useNavigate because it will lose editor focus if there is any
            window.history.replaceState(
              null,
              '',
              `${baseUrl}/draft/message/${id}`,
            );
          },
        },
      );
    }
  };
};

/**
 * Hook to create a draft message.
 * @returns Mutation result for creating the draft.
 */
export const useCreateDraft = () => {
  const setMessage = useMessageStore.use.setMessage();
  const message = useMessageStore.use.message();
  const { updateFolderBadgeCountQueryCache } =
    useUpdateFolderBadgeCountQueryCache();

  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      payload,
      inReplyToId,
    }: {
      payload: {
        subject?: string;
        body?: string;
        to?: string[];
        cc?: string[];
        cci?: string[];
      };
      inReplyToId?: string;
    }) => messageService.createDraft(payload, inReplyToId),
    onSuccess: ({ id }) => {
      if (!message) return;

      message.date = new Date().getTime();
      message.id = id;
      setMessage({ ...message });
      updateFolderBadgeCountQueryCache('draft', 1);

      invalidateQueriesWithFirstPage(queryClient, {
        queryKey: folderQueryKeys.messages('draft'),
      });
    },
  });
};

/**
 * Hook to update a draft message.
 * @returns Mutation result for updating the draft.
 */
export const useUpdateDraft = () => {
  const setMessage = useMessageStore.use.setMessage();
  const queryClient = useQueryClient();
  const messageUpdated = useMessageStore.use.message();

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
      if (!messageUpdated) return;
      messageUpdated.date = new Date().getTime();

      setMessage({ ...messageUpdated });

      //update message details query cache
      queryClient.setQueryData(
        messageQueryKeys.byId(draftId),
        (message: Message | undefined) => {
          return message ? { ...message, ...messageUpdated } : undefined;
        },
      );

      invalidateQueriesWithFirstPage(queryClient, {
        queryKey: folderQueryKeys.messages('draft'),
      });
    },
  });
};

/**
 * Hook to send a draft message.
 * @returns Mutation result for sending the draft.
 */
export const useSendDraft = () => {
  const { updateFolderBadgeCountQueryCache } =
    useUpdateFolderBadgeCountQueryCache();
  const { deleteMessagesFromQueryCache } = useDeleteMessagesFromQueryCache();
  const queryClient = useQueryClient();

  const { user } = useEdificeClient();
  const toast = useToast();
  const { t } = useI18n();

  return useMutation({
    mutationFn: ({
      draftId,
      payload,
      inReplyToId,
    }: {
      draftId: string;
      payload?: {
        subject?: string;
        body?: string;
        to?: string[];
        cc?: string[];
        cci?: string[];
      };
      inReplyToId?: string;
    }) => messageService.send(draftId, payload, inReplyToId),
    onSuccess: (_response, { payload, draftId }) => {
      toast.success(t('message.sent'));

      if (
        payload &&
        user &&
        [
          ...(payload.cc || []),
          ...(payload.to || []),
          ...(payload.cci || []),
        ].includes(user.userId)
      ) {
        updateFolderBadgeCountQueryCache('inbox', +1);
        invalidateQueriesWithFirstPage(queryClient, {
          queryKey: folderQueryKeys.messages('inbox'),
        });
      }

      invalidateQueriesWithFirstPage(queryClient, {
        queryKey: folderQueryKeys.messages('outbox'),
      });

      // Delete message from draft list in query cache
      deleteMessagesFromQueryCache('draft', [draftId]);

      // Invalidate message details
      queryClient.invalidateQueries({
        queryKey: messageQueryKeys.byId(draftId),
      });
    },
  });
};

/**
 * Hook to recall a sent message.
 * @returns void
 */
export const useRecallMessage = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ messageId }: { messageId: string }) =>
      messageService.recall(messageId),
    onSuccess: (_data, { messageId }) => {
      queryClient.invalidateQueries({
        queryKey: messageQueryKeys.byId(messageId),
      });
    },
  });
};

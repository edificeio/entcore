import { odeServices } from '@edifice.io/client';
import { useEdificeClient, useToast } from '@edifice.io/react';
import {
  InfiniteData,
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useParams, useSearchParams } from 'react-router-dom';
import { useI18n, useSelectedFolder } from '~/hooks';
import { Message, MessageBase } from '~/models';
import {
  baseUrl,
  folderQueryOptions,
  messageService,
  useFolderMessages,
  useFolderUtils,
  useUpdateFolderBadgeCountLocal,
} from '~/services';
import { useAppActions, useMessageUpdated } from '~/store';
const appCodeName = 'conversation';
/**
 * Message Query Options Factory.
 */
export const messageQueryOptions = {
  base: ['message'] as const,
  /**
   * Generates query options for fetching message application configuration.
   * @returns Query options for fetching the configuration.
   */
  getConfig() {
    return queryOptions({
      queryKey: [messageQueryOptions.base, 'config'] as const,
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
      queryKey: [...messageQueryOptions.base, messageId] as const,
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
      queryKey: [
        ...messageQueryOptions.base,
        messageId,
        'originalFormat',
      ] as const,
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
export const useMessage = (messageId: string) => {
  const result = useQuery(messageQueryOptions.getById(messageId));
  const { folderId } = useSelectedFolder();
  const { updateFolderMessagesQueryData } = useFolderUtils();
  const { currentLanguage, user, userProfile } = useEdificeClient();

  if (result.isSuccess && result.data) {
    const message = result.data;
    if (message.id) {
      // Fix issue when back retrun sbuject and body to null
      if (message.subject === null) {
        message.subject = '';
      }
      if (message.body === null) {
        message.body = '';
      }

      // Update the message unread status in the list
      if (folderId) {
        updateFolderMessagesQueryData(folderId, (oldMessage) =>
          oldMessage.id === message.id
            ? { ...oldMessage, unread: false }
            : oldMessage,
        );
      }
    } else {
      message.language = currentLanguage;
      message.from = {
        id: user?.userId || '',
        displayName: user?.username || '',
        profile: (userProfile || '') as string,
      };
    }
  }
  return result;
};

export const useOriginalMessage = (messageId: string) => {
  return useQuery(messageQueryOptions.getOriginalFormat(messageId));
};

/**
 * Hook to toggle the unread status of a message.
 * @param unread - The unread status to set.
 * @returns Mutation result for toggling the unread status.
 */
const useToggleUnread = (unread: boolean) => {
  const { folderId } = useSelectedFolder();
  const { updateFolderMessagesQueryData } = useFolderUtils();
  const queryClient = useQueryClient();
  const { updateFolderBadgeCountLocal } = useUpdateFolderBadgeCountLocal();

  return useMutation({
    mutationFn: ({ messages }: { messages: MessageBase[] }) =>
      messageService.toggleUnread(
        messages.map((m) => m.id),
        unread,
      ),
    onSuccess: (_data, { messages }) => {
      const messageIds = messages.map((m) => m.id);

      if (folderId) {
        if (folderId !== 'draft') {
          const countMessageUpdated = messages.filter(
            (m) => m.unread !== unread,
          ).length;
          // Update the unread count in the folder except for the draft folder wich count all messages and not only unread
          updateFolderBadgeCountLocal(
            folderId,
            unread ? countMessageUpdated : -countMessageUpdated,
          );
        }

        // Update the message unread status in the list
        updateFolderMessagesQueryData(folderId, (oldMessage) =>
          messageIds.includes(oldMessage.id)
            ? { ...oldMessage, unread }
            : oldMessage,
        );
      }

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
  const { messages, fetchNextPage, hasNextPage } = useFolderMessages(
    folderId,
    false,
  );

  return useMutation({
    mutationFn: ({ id }: { id: string | string[] }) =>
      messageService.moveToFolder('trash', id),
    onMutate: async ({ id }: { id: string | string[] }) => {
      // avoid to display placeholder if have next page
      if (messages.length === id.length && hasNextPage) {
        await fetchNextPage();
      }
    },
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

export const useEmptyTrash = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => messageService.emptyTrash(),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: ['folder', 'trash'],
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
      queryClient.invalidateQueries({
        queryKey: ['folder'],
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
  const messageUpdated = useMessageUpdated();
  const toast = useToast();
  const { t } = useI18n();

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
        { payload },
        {
          onSuccess: ({ id }) => {
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
  const { setMessageUpdated } = useAppActions();
  const messageUpdated = useMessageUpdated();
  const { updateFolderBadgeCountLocal } = useUpdateFolderBadgeCountLocal();
  const queryClient = useQueryClient();

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
    onSuccess: ({ id }) => {
      if (!messageUpdated) return;

      messageUpdated.date = new Date().getTime();
      messageUpdated.id = id;
      setMessageUpdated({ ...messageUpdated });
      updateFolderBadgeCountLocal('draft', 1);
      // Update the message unread status in the list
      queryClient.setQueryData(
        messageQueryOptions.getById(id).queryKey,
        (message: Message | undefined) => {
          return message ? { ...message, ...messageUpdated } : undefined;
        },
      );

      // Update de draft list (we can setData because we don't know if the filtre set will display it or not).
      queryClient.invalidateQueries({
        queryKey: [...folderQueryOptions.base, 'draft', 'messages'],
      });
    },
  });
};

/**
 * Hook to update a draft message.
 * @returns Mutation result for updating the draft.
 */
export const useUpdateDraft = () => {
  const { setMessageUpdated } = useAppActions();
  const queryClient = useQueryClient();
  const messageUpdated = useMessageUpdated();

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
      setMessageUpdated({ ...messageUpdated });
      queryClient.setQueryData(
        messageQueryOptions.getById(draftId).queryKey,
        (message: Message | undefined) => {
          return message ? { ...message, ...messageUpdated } : undefined;
        },
      );
      queryClient.setQueryData(
        [...folderQueryOptions.base, 'draft', 'messages'],
        (data: InfiniteData<Message[]>) => {
          if (!data) return data;
          const pages = data.pages.map((page: Message[]) =>
            page.map((message: Message) => {
              if (message.id === draftId) {
                return { ...message, ...messageUpdated };
              }
              return message;
            }),
          );
          return { ...data, pages };
        },
      );
    },
  });
};

/**
 * Hook to send a draft message.
 * @returns Mutation result for sending the draft.
 */
export const useSendDraft = () => {
  const queryClient = useQueryClient();
  const { updateFolderBadgeCountLocal } = useUpdateFolderBadgeCountLocal();
  const { user } = useEdificeClient();
  const toast = useToast();
  const { t } = useI18n();

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
    onSuccess: (_response, { payload }) => {
      toast.success(t('message.sent'));
      updateFolderBadgeCountLocal('draft', -1);
      if (
        payload &&
        user &&
        [
          ...(payload.cc || []),
          ...(payload.to || []),
          ...(payload.cci || []),
        ].includes(user.userId)
      ) {
        updateFolderBadgeCountLocal('inbox', +1);
        queryClient.invalidateQueries({
          queryKey: ['folder', 'inbox', 'messages'],
        });
      }
      queryClient.invalidateQueries({
        queryKey: ['folder', 'draft', 'messages'],
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
      // TODO optimistic update ?
      queryClient.invalidateQueries({
        queryKey: messageQueryOptions.getById(messageId).queryKey,
      });
    },
  });
};

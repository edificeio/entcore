import { InfiniteData, useQueryClient } from '@tanstack/react-query';
import { Message } from '~/models';
import { folderQueryOptions } from '~/services';
import { useUpdateFolderBadgeCountQueryCache } from './useUpdateFolderBadgeCountQueryCache';

export const useDeleteMessagesFromQueryCache = () => {
  const queryClient = useQueryClient();
  const { updateFolderBadgeCountQueryCache } =
    useUpdateFolderBadgeCountQueryCache();

  const deleteMessagesFromQueryCache = (
    folderId: string,
    messageIds: string[],
  ) => {
    let unreadTrashedCount = 0;

    // Update list message
    queryClient.setQueriesData(
      { queryKey: folderQueryOptions.getMessagesQuerykey(folderId, {}) },
      (data: InfiniteData<Message[]>) => {
        if (!data) return;
        const countUnreadMessages = (
          messages: Message[],
          messageIds: string[],
        ): number => {
          return messages.filter(
            (message) => message.unread && messageIds.includes(message.id),
          ).length;
        };

        if (!['trash', 'draft', 'outbox'].includes(folderId!)) {
          unreadTrashedCount = data.pages.reduce(
            (count, page) => count + countUnreadMessages(page, messageIds),
            0,
          );
        }

        // Filter out deleted messages
        const pages = data.pages.map((page: Message[]) =>
          page.filter((message: Message) => !messageIds.includes(message.id)),
        );

        return {
          ...data,
          pages,
        };
      },
    );

    // Update unread inbox count
    // Update custom folder count
    if (
      !['trash', 'draft', 'outbox'].includes(folderId!) &&
      unreadTrashedCount
    ) {
      updateFolderBadgeCountQueryCache(folderId!, -unreadTrashedCount);
    }

    // Update draft count if It's a draft message
    if (folderId === 'draft') {
      updateFolderBadgeCountQueryCache(folderId, -messageIds.length);
    }
  };
  return { deleteMessagesFromQueryCache };
};

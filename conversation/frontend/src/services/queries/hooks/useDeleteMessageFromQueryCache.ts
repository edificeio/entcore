import { InfiniteData, useQueryClient } from '@tanstack/react-query';
import { Message, MessageMetadata } from '~/models';
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
      (data: InfiniteData<MessageMetadata[]>) => {
        if (!data) return;
        const countUnreadMessages = (
          messages: MessageMetadata[],
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

        //total message count
        const totalItems = data.pages[0][0].count;
        const newTotalItems = totalItems - messageIds.length;

        const pages = data.pages.map((page: MessageMetadata[]) => {
          return (
            page
              // Filter out deleted messages
              .filter(
                (message: MessageMetadata) => !messageIds.includes(message.id),
              )
              // update count
              .map((message: MessageMetadata) => ({
                ...message,
                count: newTotalItems,
              }))
          );
        });

        //update message count

        return {
          ...data,
          pages,
        };
      },
    );

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

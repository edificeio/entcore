import { InfiniteData } from '@tanstack/react-query';
import { useQueryClient } from '@tanstack/react-query';
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
      // Remove deleted message from pages
      (data: InfiniteData<Message[]>) => {
        if (!['trash', 'draft', 'outbox'].includes(folderId!)) {
          unreadTrashedCount = data.pages.reduce((count, page) => {
            return (
              count +
              page.filter(
                (message) => message.unread && messageIds.includes(message.id),
              ).length
            );
          }, 0);
        }

        return {
          ...data,
          pages: data.pages.map((page: Message[]) =>
            page.filter((message: Message) => !messageIds.includes(message.id)),
          ),
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

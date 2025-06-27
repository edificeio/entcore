import { Message, MessageBase } from '~/models';
import { messageQueryKeys, useFolderUtils } from '~/services';
import { useUpdateFolderBadgeCountQueryCache } from './useUpdateFolderBadgeCountQueryCache';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { queryClient } from '~/providers';

export const useToggleUnreadMessagesFromQueryCache = () => {
  const { updateFolderMessagesQueryCache } = useFolderUtils();
  const { updateFolderBadgeCountQueryCache } =
    useUpdateFolderBadgeCountQueryCache();
  const { folderId } = useSelectedFolder();

  const toggleUnreadMessagesFromQueryCache = (
    messages: MessageBase[],
    unread: boolean,
  ) => {
    const messageIds = messages.map((m) => m.id);

    if (folderId) {
      if (folderId !== 'draft') {
        const countMessageUpdated = messages.filter(
          (m) => m.unread !== unread,
        ).length;
        // Update the unread count in the folder except for the draft folder wich count all messages and not only unread
        updateFolderBadgeCountQueryCache(
          folderId,
          unread ? countMessageUpdated : -countMessageUpdated,
        );
      }

      // Update the message unread status in the list
      updateFolderMessagesQueryCache(folderId, (oldMessage) =>
        messageIds.includes(oldMessage.id)
          ? { ...oldMessage, unread }
          : oldMessage,
      );

      // Update the message unread status in outbox list if from me
      updateFolderMessagesQueryCache('outbox', (oldMessage) =>
        messageIds.includes(oldMessage.id)
          ? { ...oldMessage, unread }
          : oldMessage,
      );
    }
    // Update message details (unread status)
    messageIds.forEach((messageId) => {
      queryClient.setQueryData(
        messageQueryKeys.byId(messageId),
        (message: Message | undefined) => {
          return message ? { ...message, unread } : undefined;
        },
      );
    });
  };

  return {
    toggleUnreadMessagesFromQueryCache,
  };
};

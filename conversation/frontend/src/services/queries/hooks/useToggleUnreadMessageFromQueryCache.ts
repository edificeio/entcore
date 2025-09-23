import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { Message, MessageBase } from '~/models';
import { queryClient } from '~/providers';
import { messageQueryKeys, useFolderUtils } from '~/services';
import { useUpdateFolderBadgeCountQueryCache } from './useUpdateFolderBadgeCountQueryCache';

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
      updateFolderMessagesQueryCache(
        (oldMessage) =>
          messageIds.includes(oldMessage.id)
            ? { ...oldMessage, unread }
            : oldMessage,
        folderId,
      );

      // Update the message unread status in outbox list if from me
      updateFolderMessagesQueryCache(
        (oldMessage) =>
          messageIds.includes(oldMessage.id)
            ? { ...oldMessage, unread }
            : oldMessage,
        folderId === 'outbox' ? 'inbox' : 'outbox',
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

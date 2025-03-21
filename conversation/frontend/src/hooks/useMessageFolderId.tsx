import { useEdificeClient } from '@edifice.io/react';
import { MessageMetadata } from '~/models';
import { useSelectedFolder } from './useSelectedFolder';
import { isInRecipient } from '~/services';

export function useMessageFolderId(message: MessageMetadata) {
  const { folderId } = useSelectedFolder();
  const { user } = useEdificeClient();

  const isInUserFolder =
    folderId && !['draft', 'outbox', 'trash', 'inbox'].includes(folderId);

  if (!isInUserFolder || !user) {
    return {
      messageFolderId: folderId,
      isInUserFolder,
    };
  }

  const isUserAuthor = message.from.id === user.userId;
  const isCurrentUserInRecipient = isInRecipient(message, user.userId);

  const originFolderId =
    message.state === 'DRAFT'
      ? 'draft'
      : isCurrentUserInRecipient
        ? 'inbox'
        : isUserAuthor
          ? 'outbox'
          : 'inbox';

  return {
    messageFolderId: originFolderId,
    isInUserFolder,
  };
}

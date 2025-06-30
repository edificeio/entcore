import { useEdificeClient } from '@edifice.io/react';
import { MessageMetadata } from '~/models';
import { isInRecipient } from '~/services';
import { useSelectedFolder } from './useSelectedFolder';

export function useMessageFolderId(message: MessageMetadata) {
  const { folderId } = useSelectedFolder();
  const { user } = useEdificeClient();

  const isInUserFolderOrTrash =
    !!folderId && !['draft', 'outbox', 'inbox'].includes(folderId);

  if (!isInUserFolderOrTrash || !user) {
    return {
      messageFolderId: folderId,
      isInUserFolderOrTrash,
    };
  }

  const isUserAuthor = message.from?.id === user.userId;
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
    isInUserFolderOrTrash,
  };
}

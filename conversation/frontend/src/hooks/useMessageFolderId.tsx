import { useEdificeClient } from '@edifice.io/react';
import { MessageMetadata } from '~/models';
import { getMessageOriginFolderId } from '~/services';
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

  const originFolderId = getMessageOriginFolderId(message, user.userId);

  return {
    messageFolderId: originFolderId,
    isInUserFolderOrTrash,
  };
}

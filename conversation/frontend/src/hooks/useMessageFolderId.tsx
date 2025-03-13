import { useEdificeClient } from '@edifice.io/react';
import { MessageMetadata } from '~/models';
import { useSelectedFolder } from './useSelectedFolder';

export function useMessageFolderId(message: MessageMetadata) {
  const { folderId } = useSelectedFolder();
  const { user } = useEdificeClient();

  const isUserAuthor = message.from.id === user?.userId;
  const originFolderId =
    message.state === 'DRAFT' ? 'draft' : isUserAuthor ? 'outbox' : 'inbox';

  const isInUserFolder =
    folderId && !['draft', 'outbox', 'trash', 'inbox'].includes(folderId);
  const messageFolderId = isInUserFolder ? originFolderId : folderId;

  return {
    messageFolderId,
    isInUserFolder,
  };
}

import { useEdificeClient } from '@edifice.io/react';
import { useCallback, useMemo } from 'react';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { MessageMetadata } from '~/models';
import { isInRecipient } from '~/services';
import useSelectedMessages from './useSelectedMessages';

export default function useToolbarVisibility(
  messages: MessageMetadata[],
): Record<string, 'show' | 'hide'> {
  const { folderId } = useSelectedFolder();
  const selectedMessages = useSelectedMessages(messages);
  const { user } = useEdificeClient();

  const isInTrash = folderId === 'trash';
  const isInDraft = folderId === 'draft';

  const canShowMarkActions = useCallback(
    (unread: boolean) => {
      return (
        !['draft', 'outbox', 'trash'].includes(folderId!) &&
        // Check if the selected messages are not drafts and are unread or read depending on the action
        selectedMessages.some(
          (message) => message.unread === unread && message.state !== 'DRAFT',
        ) &&
        // Check if the selected messages are not sent by the user
        !selectedMessages.some(
          (message) =>
            message.from?.id === user?.userId &&
            !isInRecipient(message, user ? user.userId : ''),
        )
      );
    },
    [folderId, selectedMessages, user],
  );

  const canEmptyTrash = useMemo(() => {
    if (!isInTrash) return false;
    return messages.length > 0 && selectedMessages.length === 0;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [folderId, messages]);

  const isInFolder = useMemo(() => {
    if (folderId && ['trash', 'inbox', 'outbox', 'draft'].includes(folderId))
      return;
    return selectedMessages.length > 0;
  }, [folderId, selectedMessages]);

  const canBeMovetoTrash = useMemo(() => {
    if (isInTrash) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, selectedMessages]);

  const canBeMoveToFolder = useMemo(() => {
    if (isInTrash || isInDraft) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, isInDraft, selectedMessages]);

  const canMarkAsReadMessages = useMemo(() => {
    return canShowMarkActions(true);
  }, [canShowMarkActions]);

  const canMarkAsUnReadMessages = useMemo(() => {
    return canShowMarkActions(false);
  }, [canShowMarkActions]);

  const isTrashMessage = useMemo(() => {
    if (!isInTrash) return false;
    return selectedMessages.length > 0;
  }, [isInTrash, selectedMessages]);

  return {
    showEmptyTrash: canEmptyTrash ? 'show' : 'hide',
    showFolderActions: isInFolder ? 'show' : 'hide',
    showMarkAsReadMessages: canMarkAsReadMessages ? 'show' : 'hide',
    showMarkAsUnReadMessages: canMarkAsUnReadMessages ? 'show' : 'hide',
    showMoveToFolder: canBeMoveToFolder ? 'show' : 'hide',
    showMoveToTrash: canBeMovetoTrash ? 'show' : 'hide',
    showTrashActions: isTrashMessage ? 'show' : 'hide',
  };
}

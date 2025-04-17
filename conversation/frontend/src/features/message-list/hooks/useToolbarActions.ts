import { useToast } from '@edifice.io/react';
import { useTranslation } from 'react-i18next';
import { MessageMetadata } from '~/models';
import {
  useDeleteMessage,
  useEmptyTrash,
  useMarkRead,
  useMarkUnread,
  useMoveMessage,
  useRestoreMessage,
  useTrashMessage,
} from '~/services';
import { useConfirmModalStore, useSelectedMessageIds } from '~/store';
import { useFolderHandlers } from '../../menu/hooks/useFolderHandlers';
import useSelectedMessages from './useSelectedMessages';

export default function useToolbarActions(messages: MessageMetadata[]) {
  const selectedMessages = useSelectedMessages(messages);

  const selectedIds = useSelectedMessageIds();
  const { openModal } = useConfirmModalStore();
  const { t } = useTranslation('conversation');
  const { success } = useToast();

  const restoreQuery = useRestoreMessage();
  const moveToTrashQuery = useTrashMessage();
  const deleteMessage = useDeleteMessage();
  const moveMessage = useMoveMessage();
  const emptyTrashQuery = useEmptyTrash();
  const markAsReadQuery = useMarkRead();
  const markAsUnreadQuery = useMarkUnread();

  const { handleMoveMessage } = useFolderHandlers();

  const handleEmptyTrash = () => {
    openModal({
      id: 'empty-trash-modal',
      header: t('trash.empty.confirm.title'),
      body: t('trash.empty.confirm.description'),
      okText: t('delete'),
      koText: t('cancel'),
      size: 'sm',
      onSuccess: () => {
        emptyTrashQuery.mutate();
      },
    });
  };

  const handleDelete = () => {
    openModal({
      id: 'delete-modal',
      header: t('delete.definitely'),
      body: t('delete.definitely.confirm'),
      okText: t('delete'),
      koText: t('cancel'),
      size: 'sm',
      onSuccess: () => {
        deleteMessage.mutate({ id: selectedIds });
      },
    });
  };

  const handleMarkAsReadClick = () => {
    markAsReadQuery.mutate({ messages: selectedMessages });
  };

  const handleMarkAsUnreadClick = () => {
    markAsUnreadQuery.mutate({ messages: selectedMessages });
  };

  const handleMoveToFolder = () => {
    handleMoveMessage();
  };

  const handleMoveToTrash = () => {
    moveToTrashQuery.mutate({ id: selectedIds });
  };

  const handleRemoveFromFolder = () => {
    openModal({
      id: 'remove-from-folder-modal',
      header: t('remove.from.folder'),
      body: t('remove.from.folder.confirm'),
      okText: t('confirm'),
      koText: t('cancel'),
      onSuccess: async () => {
        const confirmMessage =
          selectedIds.length > 1
            ? t('messages.remove.from.folder')
            : t('message.remove.from.folder');

        await Promise.allSettled(
          selectedIds.map((id) =>
            moveMessage.mutateAsync({ folderId: 'inbox', id }),
          ),
        );

        success(confirmMessage);
      },
    });
  };

  const handleRestore = () => {
    restoreQuery.mutate({ id: selectedIds });
  };

  return {
    handleDelete,
    handleEmptyTrash,
    handleMarkAsReadClick,
    handleMarkAsUnreadClick,
    handleMoveToFolder,
    handleMoveToTrash,
    handleRemoveFromFolder,
    handleRestore,
  };
}

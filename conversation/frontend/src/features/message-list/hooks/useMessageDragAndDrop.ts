import { DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { useEdificeClient, useToast } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import {
  getMessageOriginFolderId,
  useFolderMessages,
  useFolderUtils,
  useMoveMessage,
  useTrashMessage,
} from '~/services';
import { useConfirmModalStore } from '~/store';
import { useActionsStore } from '~/store/actions';

const SYSTEM_FOLDERS = ['inbox', 'outbox', 'draft', 'trash'];

/** System folders a message can be put back to, once removed from a user's folder. */
const ORIGIN_FOLDERS = ['inbox', 'outbox', 'draft'];

/**
 * Returns the messages carried by a drag :
 * the whole selection if the dragged message is part of it, the dragged message alone otherwise.
 */
export function getDraggedMessageIds(
  draggedMessageId: string,
  selectedMessageIds: string[],
) {
  return selectedMessageIds.includes(draggedMessageId)
    ? selectedMessageIds
    : [draggedMessageId];
}

/**
 * Same rules as the "Move", "Delete" and "Remove from folder" toolbar buttons :
 * messages can be trashed from anywhere but the trash,
 * moved to a user's folder from anywhere but the trash and the drafts,
 * and put back from a user's folder to the system folder one of them originates from.
 * @param originFolderIds the system folders the dragged messages originate from.
 */
export function canDropMessages(
  sourceFolderId: string | undefined,
  targetFolderId: string,
  originFolderIds: string[],
) {
  if (!sourceFolderId || sourceFolderId === 'trash') return false;
  if (sourceFolderId === targetFolderId) return false;
  if (targetFolderId === 'trash') return true;
  if (ORIGIN_FOLDERS.includes(targetFolderId))
    return (
      !SYSTEM_FOLDERS.includes(sourceFolderId) &&
      originFolderIds.includes(targetFolderId)
    );
  return sourceFolderId !== 'draft';
}

/** Handlers of the drag & drop of messages onto the folders of the menu. */
export function useMessageDragAndDrop() {
  const { t } = useI18n();
  const toast = useToast();
  const { user } = useEdificeClient();
  const { folderId } = useSelectedFolder();
  // Cached messages of the listed folder only : the dragged messages are among them.
  const { messages } = useFolderMessages(folderId ?? '', false);
  const selectedMessageIds = useActionsStore.use.selectedMessageIds();
  const { openModal } = useConfirmModalStore();
  const moveMessage = useMoveMessage();
  const trashMessage = useTrashMessage();
  const { getFolderNameById } = useFolderUtils();
  const [draggedMessageIds, setDraggedMessageIds] = useState<string[]>([]);
  const [originFolderIds, setOriginFolderIds] = useState<string[]>([]);

  const canDropOn = (targetFolderId: string) =>
    draggedMessageIds.length > 0 &&
    canDropMessages(folderId, targetFolderId, originFolderIds);

  const resetDrag = () => {
    setDraggedMessageIds([]);
    setOriginFolderIds([]);
  };

  const handleDragStart = ({ active }: DragStartEvent) => {
    const messageId = active.data.current?.messageId as string | undefined;
    if (!messageId) return;
    const ids = getDraggedMessageIds(messageId, selectedMessageIds);
    setDraggedMessageIds(ids);
    setOriginFolderIds([
      ...new Set(
        messages
          .filter((message) => ids.includes(message.id))
          .map((message) =>
            getMessageOriginFolderId(message, user?.userId ?? ''),
          ),
      ),
    ]);
  };

  const removeFromFolder = (ids: string[]) => {
    openModal({
      id: 'remove-from-folder-modal',
      header: t('remove.from.folder'),
      body: t('remove.from.folder.confirm'),
      okText: t('confirm'),
      koText: t('cancel'),
      onSuccess: () => {
        // Removed from their user's folder, messages go back to their own origin folder.
        moveMessage.mutate(
          { folderId: 'inbox', id: ids },
          {
            onSuccess: () => {
              toast.success(
                t(
                  ids.length > 1
                    ? 'messages.remove.from.folder'
                    : 'message.remove.from.folder',
                ),
              );
            },
          },
        );
      },
    });
  };

  const handleDragEnd = ({ over }: DragEndEvent) => {
    const ids = draggedMessageIds;
    const targetFolderId = over?.data.current?.id as string | undefined;
    const canDrop = !!targetFolderId && canDropOn(targetFolderId);
    resetDrag();
    if (!canDrop) return;

    if (targetFolderId === 'trash') {
      trashMessage.mutate({ id: ids });
      return;
    }

    if (ORIGIN_FOLDERS.includes(targetFolderId)) {
      removeFromFolder(ids);
      return;
    }

    moveMessage.mutate(
      { folderId: targetFolderId, id: ids },
      {
        onSuccess: () => {
          const folderName = getFolderNameById(targetFolderId);
          toast.success(
            ids.length > 1
              ? t('messages.move.folder', { count: ids.length, folderName })
              : t('message.move.folder', { folderName }),
          );
        },
      },
    );
  };

  return {
    draggedMessageIds,
    canDropOn,
    handleDragStart,
    handleDragEnd,
    handleDragCancel: resetDrag,
  };
}

import { DragEndEvent, DragStartEvent } from '@dnd-kit/core';
import { useToast } from '@edifice.io/react';
import { useState } from 'react';
import { useI18n } from '~/hooks/useI18n';
import { useSelectedFolder } from '~/hooks/useSelectedFolder';
import { useFolderUtils, useMoveMessage, useTrashMessage } from '~/services';
import { useActionsStore } from '~/store/actions';

const SYSTEM_FOLDERS = ['inbox', 'outbox', 'draft', 'trash'];

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
 * and put back in the inbox from a user's folder only.
 */
export function canDropMessages(
  sourceFolderId: string | undefined,
  targetFolderId: string,
) {
  if (!sourceFolderId || sourceFolderId === 'trash') return false;
  if (sourceFolderId === targetFolderId) return false;
  if (targetFolderId === 'trash') return true;
  if (targetFolderId === 'inbox')
    return !SYSTEM_FOLDERS.includes(sourceFolderId);
  if (SYSTEM_FOLDERS.includes(targetFolderId)) return false;
  return sourceFolderId !== 'draft';
}

/** Handlers of the drag & drop of messages onto the folders of the menu. */
export function useMessageDragAndDrop() {
  const { t } = useI18n();
  const toast = useToast();
  const { folderId } = useSelectedFolder();
  const selectedMessageIds = useActionsStore.use.selectedMessageIds();
  const moveMessage = useMoveMessage();
  const trashMessage = useTrashMessage();
  const { getFolderNameById } = useFolderUtils();
  const [draggedMessageIds, setDraggedMessageIds] = useState<string[]>([]);

  const handleDragStart = ({ active }: DragStartEvent) => {
    const messageId = active.data.current?.messageId as string | undefined;
    if (!messageId) return;
    setDraggedMessageIds(getDraggedMessageIds(messageId, selectedMessageIds));
  };

  const handleDragCancel = () => setDraggedMessageIds([]);

  const handleDragEnd = ({ over }: DragEndEvent) => {
    const ids = draggedMessageIds;
    setDraggedMessageIds([]);

    const targetFolderId = over?.data.current?.id as string | undefined;
    if (!ids.length || !targetFolderId) return;
    if (!canDropMessages(folderId, targetFolderId)) return;

    if (targetFolderId === 'trash') {
      trashMessage.mutate({ id: ids });
      return;
    }

    moveMessage.mutate(
      { folderId: targetFolderId, id: ids },
      {
        onSuccess: () => {
          if (targetFolderId === 'inbox') {
            toast.success(
              t(
                ids.length > 1
                  ? 'messages.remove.from.folder'
                  : 'message.remove.from.folder',
              ),
            );
            return;
          }
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
    handleDragStart,
    handleDragEnd,
    handleDragCancel,
  };
}

import { Announcements, Over } from '@dnd-kit/core';
import { useI18n } from '~/hooks/useI18n';
import { useFolderUtils } from '~/services';

const SYSTEM_FOLDERS = ['inbox', 'outbox', 'draft', 'trash'];

/**
 * Screen reader announcements of a drag of messages, translated and naming folders
 * (the default ones of @dnd-kit are in English and read the technical ids).
 * @param canDropOn tells whether the dragged messages can be dropped on a folder.
 */
export function useMessageDragAnnouncements(
  canDropOn: (folderId: string) => boolean,
): Announcements {
  const { t } = useI18n();
  const { getFolderNameById } = useFolderUtils();

  const getOverFolderId = (over: Over | null) =>
    over?.data.current?.id as string | undefined;

  const getFolderName = (folderId: string) =>
    SYSTEM_FOLDERS.includes(folderId)
      ? t(`${folderId}.folder`)
      : getFolderNameById(folderId);

  return {
    onDragStart: () => t('drag.announce.start'),
    onDragOver: ({ over }) => {
      const folderId = getOverFolderId(over);
      if (!folderId) return undefined;
      return t(
        canDropOn(folderId)
          ? 'drag.announce.over'
          : 'drag.announce.over.forbidden',
        { folderName: getFolderName(folderId) },
      );
    },
    onDragEnd: ({ over }) => {
      const folderId = getOverFolderId(over);
      if (!folderId || !canDropOn(folderId)) return t('drag.announce.cancel');
      return t('drag.announce.drop', { folderName: getFolderName(folderId) });
    },
    onDragCancel: () => t('drag.announce.cancel'),
  };
}

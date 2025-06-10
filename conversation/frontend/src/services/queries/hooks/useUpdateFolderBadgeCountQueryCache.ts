import { useQueryClient } from '@tanstack/react-query';
import { Folder } from '~/models';
import { searchFolder } from '~/services';

export const useUpdateFolderBadgeCountQueryCache = () => {
  const queryClient = useQueryClient();
  const updateFolderBadgeCountQueryCache = (
    folderId: string,
    countDelta: number,
  ) => {
    if (folderId === 'inbox') {
      // Update inbox count unread
      queryClient.setQueryData(
        ['folder', 'count', 'inbox', { unread: true }],
        ({ count }: { count: number }) => {
          return { count: count + countDelta };
        },
      );
    } else if (folderId === 'draft') {
      // Update draft count
      queryClient.setQueryData(
        ['folder', 'count', 'draft', null],
        ({ count }: { count: number }) => {
          return { count: count + countDelta };
        },
      );
    } else if (!['inbox', 'trash', 'draft', 'outbox'].includes(folderId)) {
      // Update custom folder count unread
      queryClient.setQueryData(['folder', 'tree'], (folders: Folder[]) => {
        // go trow the folder tree to find the folder to update
        const result = searchFolder(folderId, folders);
        if (!result?.parent) {
          return folders.map((folder) => {
            if (folder.id === folderId) {
              return { ...folder, nbUnread: folder.nbUnread + countDelta };
            }
            return folder;
          });
        } else if (result?.folder) {
          result.folder = {
            ...result.folder,
            nbUnread: result.folder.nbUnread + countDelta,
          };
          return [...folders];
        }
      });
    }
  };
  return { updateFolderBadgeCountQueryCache };
};

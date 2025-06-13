import { useQueryClient } from '@tanstack/react-query';
import { Folder } from '~/models';
import { folderQueryKeys, searchFolder } from '~/services';

export const useUpdateFolderBadgeCountQueryCache = () => {
  const queryClient = useQueryClient();
  const updateFolderBadgeCountQueryCache = (
    folderId: string,
    countDelta: number,
  ) => {
    if (folderId === 'inbox') {
      // Update inbox count unread
      queryClient.setQueryData(
        folderQueryKeys.count('inbox'),
        ({ count }: { count: number }) => {
          return { count: count + countDelta };
        },
      );
    } else if (folderId === 'draft') {
      // Update draft count
      queryClient.setQueryData(
        folderQueryKeys.count('draft'),
        ({ count }: { count: number }) => {
          return { count: count + countDelta };
        },
      );
    } else if (!['inbox', 'trash', 'draft', 'outbox'].includes(folderId)) {
      // Update custom folder count unread
      queryClient.setQueryData(folderQueryKeys.tree(), (folders: Folder[]) => {
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

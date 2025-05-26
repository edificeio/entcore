import { useParams } from 'react-router-dom';
import { Folder } from '~/models';
import { searchFolder, useFoldersTree } from '~/services';

/**
 * Hook to extract the selected folder ID from the URL,
 * and retrieve matching metadata from available loaders' queries.
 * @returns {
 * `folderId`: the folder ID extracted from the path. Undefined when not valid.
 * `userFolder`: the matching user's folder metadata. Undefined for system folders.
 * }
 */
export function useSelectedFolder(): {
  folderId?: string;
  userFolder?: Folder;
} {
  const { folderId } = useParams();
  const foldersTreeQuery = useFoldersTree();
  const foldersTree = foldersTreeQuery.data;

  switch (folderId?.toLowerCase()) {
    case 'inbox':
    case 'outbox':
    case 'draft':
    case 'trash':
      return { folderId: folderId.toLowerCase() };
    default:
      break;
  }

  if (!foldersTree || !folderId) return { folderId };

  const found = searchFolder(folderId, foldersTree);

  return {
    folderId,
    userFolder: found?.folder,
  };
}

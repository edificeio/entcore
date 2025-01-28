import { useParams } from 'react-router-dom';
import { Folder } from '~/models';
import { recursiveSearch } from '~/services';
import { useFoldersTree } from '~/store';

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
  const foldersTree = useFoldersTree();
  const { folderId } = useParams() as { folderId: string };

  if (!folderId) return {};

  switch (folderId.toLowerCase()) {
    case 'inbox':
    case 'outbox':
    case 'draft':
    case 'trash':
      return { folderId: folderId.toLowerCase() };
    default:
      break;
  }

  return {
    folderId,
    userFolder: recursiveSearch(folderId, foldersTree),
  };
}

import { useParams, useRouteLoaderData } from 'react-router-dom';
import { Folder } from '~/models';

/** Build a tree of TreeItems from Folders  */
function recursiveSearch(
  folderId: string,
  folders: Folder[],
): Folder | undefined {
  for (const f of folders) {
    if (f.id === folderId) return f;
    if (f.subFolders) {
      const folder = recursiveSearch(folderId, f.subFolders);
      if (folder) return folder;
    }
  }
}

export function useSelectedFolder(): string | Folder | undefined {
  // See `layout` loader
  const { foldersTree } = useRouteLoaderData('layout') as {
    foldersTree: Folder[];
  };
  const { folderId } = useParams() as { folderId: string };

  if (!folderId) return;

  switch (folderId.toLowerCase()) {
    case 'inbox':
    case 'outbox':
    case 'draft':
    case 'trash':
      return folderId.toLowerCase();
    default:
      break;
  }

  return recursiveSearch(folderId, foldersTree);
}

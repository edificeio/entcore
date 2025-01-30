import { Folder } from '~/models';

/** Search for a folder in a tree of Folders */
export function searchFolder(
  folderId: string,
  folders: Folder[],
): { folder: Folder; parent: Folder | undefined } | undefined {
  return recursiveSearch2(folderId, folders);
}

function recursiveSearch2(
  folderId: string,
  folders: Folder[],
  parent?: Folder,
): { folder: Folder; parent: Folder | undefined } | undefined {
  for (const folder of folders) {
    if (folder.id === folderId) return { folder, parent };
    if (folder.subFolders) {
      const found = recursiveSearch2(folderId, folder.subFolders, folder);
      if (found) return found;
    }
  }
}

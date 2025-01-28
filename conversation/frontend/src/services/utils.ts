import { Folder } from '~/models';

/** Search for a folder in a tree of Folders */
export function recursiveSearch(
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

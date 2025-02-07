import { TreeItem } from '@edifice.io/react';
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

/** Custom typing of a TreeItem exposing a user folder */
export type FolderTreeItem = TreeItem & { folder: Folder };

/**
 * Convert a tree of Folders to custom TreeItems
 * @param folders the folders tree to convert to TreeItem
 * @param maxDepth (optional) limit the depth of the resulting tree.
 * @return a tree of custom TreeItems.
 */
export function buildTree(folders: Folder[], maxDepth?: number) {
  return folders
    .sort((a, b) => (a.name < b.name ? -1 : a.name == b.name ? 0 : 1))
    .map((folder) => {
      const item = {
        id: folder.id,
        name: folder.name,
        folder,
      } as FolderTreeItem;
      if (
        folder.subFolders &&
        (typeof maxDepth === 'undefined' || folder.depth < maxDepth)
      ) {
        item.section = true;
        item.children = buildTree(folder.subFolders, maxDepth);
      }
      return item;
    });
}

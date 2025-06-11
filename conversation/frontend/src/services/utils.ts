import { TreeItem } from '@edifice.io/react';
import { InvalidateQueryFilters, QueryClient } from '@tanstack/react-query';
import {
  Attachment,
  Folder,
  Group,
  Message,
  MessageBase,
  User,
} from '~/models';

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
  return folders.map((folder) => {
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

/**
 * Check if a user is in recipient list of a message
 * @param message the message to check
 * @param userId the user id to check
 * @returns true if the user is in recipient list of a message
 */
export function isInRecipient(message: MessageBase, userId: string) {
  return [
    ...message.to.users,
    ...message.cc.users,
    ...(message.cci?.users ?? []),
  ].some((u) => u.id === userId);
}

// Must be a function to make a deep copy of the object
export function createDefaultMessage(signature?: string): Message {
  return {
    id: '',
    body: signature || '',
    language: 'fr',
    subject: '',
    from: {
      id: '',
      displayName: '',
      profile: '',
    },
    to: {
      users: new Array<User>(),
      groups: new Array<Group>(),
    },
    cc: {
      users: new Array<User>(),
      groups: new Array<Group>(),
    },
    cci: {
      users: new Array<User>(),
      groups: new Array<Group>(),
    },
    response: false,
    forwarded: false,
    state: 'DRAFT',
    attachments: new Array<Attachment>(),
    original_format_exists: false,
  };
}

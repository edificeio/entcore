import { odeServices } from 'edifice-ts-client';

import { Folder } from '~/models';

/**
 * Load folders tree.
 * @returns
 */
export function loadFoldersTree(depth: number) {
  return odeServices
    .http()
    .get<Folder[]>(`/conversation/api/folders?depth=${depth}`);
}

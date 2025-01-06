import { odeServices } from 'edifice-ts-client';

import { Folder, MessageMetadata } from '~/models';

/**
 * Creates a folder service with the specified base URL.
 *
 * @param baseURL The base URL for the folder service API.
 * @returns A service to interact with folders.
 */
export const createFolderService = (baseURL: string) => ({
  /**
   * Load folders tree.
   * @returns
   */
  getTree(depth: number) {
    return odeServices
      .http()
      .get<Folder[]>(`${baseURL}/api/folders?depth=${depth}`);
  },

  /**
   * Load paginated list of messages from a folder.
   */
  getMessages(
    folderId: string,
    options?: {
      /** (optional) Search string */
      search?: string;
      /** (optional) 0-based Page number */
      page?: number;
      /** (optional) Page size */
      pageSize?: number;
      /** (optional) Load un/read message only ? */
      unread?: boolean;
    },
  ) {
    return odeServices
      .http()
      .get<
        MessageMetadata[]
      >(`${baseURL}/api/folders/${folderId}/messages`, { queryParams: options });
  },

  create(payload: { name: string; parentId?: string }) {
    return odeServices
      .http()
      .post<{ id: string }>(`${baseURL}/conversation/folder`, payload);
  },

  rename(folderId: string, name: string) {
    return odeServices
      .http()
      .put<void>(`${baseURL}/conversation/folder/${folderId}`, { name });
  },

  trash(folderId: string) {
    return odeServices
      .http()
      .put<void>(`${baseURL}/conversation/folder/trash/${folderId}`);
  },
});

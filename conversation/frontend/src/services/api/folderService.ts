import { odeServices } from 'edifice-ts-client';

import { Folder, MessageMetadata, SYSTEM_FOLDER_IDS } from '~/models';

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
   * Count the number of messages in a system or user folder.
   * @param folderId system folder, or a user's folder ID.
   * @param unread (optional) truthy when only unread must be counted
   * @returns count of [unread] messages
   */
  getCount(folderId: string, unread?: boolean) {
    const queryParams = [] as string[];
    // `restrain` query parameter applies to users folders only.
    if (SYSTEM_FOLDER_IDS.findIndex((f) => f === folderId) === -1) {
      queryParams.push('restrain');
    }
    if (typeof unread === 'boolean') {
      queryParams.push(`unread=${unread}`);
    }
    return odeServices.http().get<{
      count: number;
    }>(`${baseURL}/count/${folderId}?${queryParams.join('&')}`);
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
      >(`${baseURL}/api/folders/${folderId}/messages`, { queryParams: { search: options?.search === '' ? undefined : options?.search, unread: options?.unread, page_size: options?.pageSize, page: options?.page } });
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

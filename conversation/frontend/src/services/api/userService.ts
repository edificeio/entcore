import { BookmarkWithDetails, odeServices } from '@edifice.io/client';
import { Visible } from '~/models/visible';

type VisibleData = { id: string; displayName: string };
export type VisibleGroupData = VisibleData & { nbUsers: number };
export type VisibleUserData = VisibleData & { profile: string };

/**
 * User service to manage user/group/bookmark.
 * This service is used to search for users/groups/bookmarks.
 * It uses the `odeServices` to make HTTP requests.
 * @returns an object with methods to search for users/groups/bookmarks
 * and get the list of bookmarks.
 * @example
 * const userService = createUserService();
 * userService.searchVisible('searchString').then((results) => {
 *   console.log(results);
 * });
 * userService.getBookmarks().then((bookmarks) => {
 *   console.log(bookmarks);
 * });
 * });
 */
export const createUserService = () => {
  // Locally define a function with multiple signatures and return types.
  function getVisibleById(id: string, type: 'User'): Promise<VisibleUserData>;
  function getVisibleById(id: string, type: 'Group'): Promise<VisibleGroupData>;
  function getVisibleById(
    id: string,
    type: 'User' | 'Group',
  ): Promise<VisibleGroupData | VisibleUserData> {
    switch (type) {
      case 'User':
        return odeServices
          .http()
          .get<{
            id: string;
            displayName: string;
            profiles: Array<string>;
          }>(`/directory/user/${id}`)
          .then(({ id, displayName, ...result }) => ({
            id,
            displayName,
            profile: result.profiles[0],
          }));

      case 'Group':
        return odeServices
          .http()
          .get<{
            id: string;
            name: string;
            nbUsers: number;
          }>(`/directory/group/${id}`)
          .then(({ id, nbUsers, ...result }) => ({
            id,
            displayName: result.name,
            nbUsers,
          }));
    }
  }

  return {
    /**
     * Search for user/group/bookmark.
     * @param search search string
     * @returns a list of Visible objects
     */
    searchVisible(search?: string) {
      return odeServices.http().get<Visible[]>(
        `/communication/visible/search`,
        typeof search === 'string'
          ? {
              queryParams: { query: search },
            }
          : undefined,
      );
    },

    /**
     * Search by ID for a visible user or group.
     */
    getVisibleById,

    /**
     * Get the list of bookmarks.
     * @returns a list of Visible objects
     */
    getBookmarks(): Promise<Visible[]> {
      return odeServices
        .directory()
        .getBookMarks()
        .then((bookmarks) => {
          return bookmarks.map(
            (bookmark): Visible => ({
              id: bookmark.id,
              displayName: bookmark.displayName,
              usedIn: ['TO', 'CC', 'CCI'],
              type: 'ShareBookmark',
            }),
          );
        });
    },

    getBookMarkById(id: string): Promise<BookmarkWithDetails> {
      return odeServices.directory().getBookMarkById(id);
    },
  };
};

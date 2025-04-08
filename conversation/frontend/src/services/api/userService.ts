import { odeServices } from 'edifice-ts-client';
import { Visible } from '~/models/visible';

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
export const createUserService = () => ({
  /**
   * Search for user/group/bookmark.
   * @param search search string
   * @returns a list of Visible objects
   */
  searchVisible(search: string) {
    return odeServices.http().get<Visible[]>(`/communication/visible/search`, {
      queryParams: { query: search },
    });
  },

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
});

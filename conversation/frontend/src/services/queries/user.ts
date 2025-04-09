import { queryOptions, useQuery, useQueryClient } from '@tanstack/react-query';
import { userService } from '~/services';

export const userQueryOptions = {
  base: ['user'] as const,

  /**
   * Get the list of visible users
   * @param search The search string to filter the users
   * @returns The query options for the search visible users
   */
  searchVisible(search: string) {
    return queryOptions({
      queryKey: [...userQueryOptions.base, search] as const,
      queryFn: () => userService.searchVisible(search),
    });
  },

  /**
   * Get the list of bookmarks
   * @returns The query options for the bookmarks
   */
  getBookmarks() {
    return queryOptions({
      queryKey: [...userQueryOptions.base, 'bookmarks'] as const,
      queryFn: () => userService.getBookmarks(),
    });
  },

  /**
   * Get the detail of bookmark
   * @param id The id of the bookmark
   * @returns The query options for the bookmark
   */
  getBookMarkById(id: string) {
    return queryOptions({
      queryKey: [...userQueryOptions.base, 'bookmark', id] as const,
      queryFn: () => userService.getBookMarkById(id),
    });
  },
};

/**
 * Hook to get the list of visible users
 * @param search The search string to filter the users
 * @returns list of visible users
 */
export const useSearchVisible = () => {
  const queryClient = useQueryClient();
  const searchVisible = (search: string) => {
    return queryClient.ensureQueryData(userQueryOptions.searchVisible(search));
  };

  return { searchVisible };
};

/**
 * Hook to get the list of bookmarks
 * @param search The search string to filter the bookmarks
 * @returns list of bookmarks
 */
export const useDefaultBookmark = () => {
  return useQuery(userQueryOptions.getBookmarks());
};

/**
 * Hook to get the bookmark by id
 * @param id The id of the bookmark
 * @returns the bookmark by id
 */
export const useBookmarkById = () => {
  const queryClient = useQueryClient();
  const getBookmarkById = (id: string) => {
    return queryClient.ensureQueryData(userQueryOptions.getBookMarkById(id));
  };

  return { getBookmarkById };
};

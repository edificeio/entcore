import { odeServices } from '@edifice.io/client';
import { useIsAdml } from '@edifice.io/react';
import { queryOptions, useQuery, useQueryClient } from '@tanstack/react-query';
import { Config } from '~/config';
import { Visible } from '~/models/visible';
import { userService } from '~/services';
import { useConfig } from '~/store';

export const userQueryOptions = {
  base: ['user'] as const,

  /**
   * Get a filtered list of visible users. Filtering is done by the *backend*.
   * @param search The search string to filter the users
   * @returns The query options for the search visible users
   */
  searchVisible(search?: string) {
    return queryOptions({
      queryKey: [
        ...userQueryOptions.base,
        'search',
        search ? { search } : undefined,
      ] as const,
      queryFn: () => userService.searchVisible(search),
      staleTime: Infinity, // This data will never change during user's session.
    });
  },

  /**
   * Get visible information about a user
   * @param id ID of the user
   * @returns The query options for the get visible by ID
   */
  getVisibleUserById(id: string) {
    return queryOptions({
      queryKey: [
        ...userQueryOptions.base,
        'get',
        { id, type: 'User' },
      ] as const,
      queryFn: () => userService.getVisibleById(id, 'User'),
      staleTime: Infinity, // This data will never change during user's session.
    });
  },

  /**
   * Get visible information about a group
   * @param id ID of the group
   * @param type Type of ID
   * @returns The query options for the get visible by ID
   */
  getVisibleGroupById(id: string) {
    return queryOptions({
      queryKey: [
        ...userQueryOptions.base,
        'get',
        { id, type: 'Group' },
      ] as const,
      queryFn: () => userService.getVisibleById(id, 'Group'),
      staleTime: Infinity, // This data will never change during user's session.
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
      staleTime: Infinity, // This data will never change during user's session.
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
      staleTime: Infinity, // This data will never change during user's session.
    });
  },
};

/** Apply filtering rules during visible searches. */
function applySearchRules(
  isAdml: boolean,
  getVisibleStrategy: Config['getVisibleStrategy'],
  search: string,
) {
  const backendFiltering = 'all-at-once' !== getVisibleStrategy || isAdml;
  const startText = backendFiltering ? search.substring(0, 3) : undefined;
  const frontendFiltering = !backendFiltering || search.length > 3;
  function computeFrontendFilter() {
    const removeAccents = odeServices.idiom().removeAccents;
    const searchTerm = removeAccents(search).toLowerCase();
    const testDisplayNames: string[] = [],
      testNameReverseds: string[] = [];

    function addSearchTerm(displayName: string): void {
      const testDisplayName = removeAccents(displayName).toLowerCase();
      testDisplayNames.push(testDisplayName);
      const split = testDisplayName.split(' ');
      testNameReverseds.push(
        split.length > 1 ? split[1] + ' ' + split[0] : testDisplayName,
      );
    }

    if (!frontendFiltering) return undefined;

    return (user: Visible) => {
      if (user.displayName) {
        addSearchTerm(user.displayName);
      }
      if (user.children) {
        user.children.forEach((child) => {
          addSearchTerm(child.displayName);
        });
      }
      if (user.relatives) {
        user.relatives.forEach((relative) => {
          addSearchTerm(relative.displayName);
        });
      }
      return (
        testDisplayNames.some((name) => name.indexOf(searchTerm) !== -1) ||
        testNameReverseds.some((name) => name.indexOf(searchTerm) !== -1)
      );
    };
  }

  return {
    triggerSearch: true,
    backendFilter: startText,
    frontendFilter: computeFrontendFilter(),
  };
}

/**
 * Hook to get the list of visible users
 * @param search The search string to filter the users
 * @returns list of visible users
 */
export const useSearchVisible = () => {
  const queryClient = useQueryClient();
  const { getVisibleStrategy } = useConfig();
  const { isAdml } = useIsAdml();

  const searchVisible = async (search: string) => {
    const { triggerSearch, backendFilter, frontendFilter } = applySearchRules(
      isAdml,
      getVisibleStrategy,
      search,
    );

    if (!triggerSearch) return Promise.resolve([]);

    const startVisible = await queryClient.ensureQueryData(
      userQueryOptions.searchVisible(backendFilter),
    );
    return frontendFilter ? startVisible.filter(frontendFilter) : startVisible;
  };

  const getVisibleUserById = (id: string) => {
    return queryClient.ensureQueryData(userQueryOptions.getVisibleUserById(id));
  };

  const getVisibleGroupById = (id: string) => {
    return queryClient.ensureQueryData(
      userQueryOptions.getVisibleGroupById(id),
    );
  };

  return { searchVisible, getVisibleUserById, getVisibleGroupById };
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

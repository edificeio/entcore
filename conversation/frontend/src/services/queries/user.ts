import { queryOptions, useQuery } from '@tanstack/react-query';
import { userService } from '~/services';

export const userQueryOptions = {
  base: ['user'] as const,
  searchVisible(search: string) {
    return queryOptions({
      queryKey: [...userQueryOptions.base, search] as const,
      queryFn: () => userService.searchVisible(search),
    });
  },

  getBookmarks() {
    return queryOptions({
      queryKey: [...userQueryOptions.base, 'bookmarks'] as const,
      queryFn: () => userService.getBookmarks(),
    });
  },
};

export const useSearchVisible = (search: string) => {
  return useQuery(userQueryOptions.searchVisible(search));
};

export const useDefaultBookmark = () => {
  return useQuery(userQueryOptions.getBookmarks());
};

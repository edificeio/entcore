import { queryOptions, useQuery } from '@tanstack/react-query';
import { customizeService } from '../api/customizeService';

/**
 * Customize Query Keys
 */
export const customizeQueryKeys = {
  all: () => ['customize'],
  languages: () => [...customizeQueryKeys.all(), 'languages'],
  fonts: () => [...customizeQueryKeys.all(), 'fonts'],
};

/**
 * Provides query options for fetching customization values.
 */
export const customizeQueryOptions = {
  /**
   * @returns Query options for fetching the list of available languages. The query is cached indefinitely since it is not expected to change.
   */
  getLanguages() {
    return queryOptions({
      queryKey: customizeQueryKeys.languages(),
      queryFn: () => customizeService.listLanguages(),
      staleTime: Infinity,
    });
  },
  /**
   * @returns Query options for fetching the list of available fonts. The query is cached indefinitely since it is not expected to change.
   */
  getFonts() {
    return queryOptions({
      queryKey: customizeQueryKeys.fonts(),
      queryFn: () => customizeService.listFonts(),
      staleTime: Infinity,
    });
  },
};

export const useLanguages = () =>
  useQuery(customizeQueryOptions.getLanguages());
export const useFonts = () => useQuery(customizeQueryOptions.getFonts());

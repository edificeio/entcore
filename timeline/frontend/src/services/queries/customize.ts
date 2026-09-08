import { queryOptions } from '@tanstack/react-query';
import { customizeService } from '../api/customizeService';

/**
 * Customize Query Keys
 */
export const customizeQueryKeys = {
  all: () => ['customize'],
  languages: () => [...customizeQueryKeys.all(), 'languages'],
  fonts: () => [...customizeQueryKeys.all(), 'fonts'],
  backgrounds: () => [...customizeQueryKeys.all(), 'backgrounds'],
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
  /**
   * @returns Query options for fetching the list of available backgrounds. The query is cached indefinitely since it is not expected to change.
   */
  getBackgrounds() {
    return queryOptions({
      queryKey: customizeQueryKeys.backgrounds(),
      queryFn: () => customizeService.listBackgrounds(),
      staleTime: Infinity,
    });
  },
};

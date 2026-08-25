import { useToast } from '@edifice.io/react';
import {
  queryOptions,
  useMutation,
  useQuery,
  useQueryClient,
} from '@tanstack/react-query';
import { useI18n } from '~/hooks/useI18n';
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

export const useSaveCustomization = () => {
  const queryClient = useQueryClient();
  const { t } = useI18n();
  const toast = useToast();

  return useMutation({
    mutationFn: async ({ lang }: { lang: string; font: string }) =>
      await Promise.all([customizeService.saveLanguagePreference(lang)]),
    onSuccess: () => {
      toast.success(t('homepage.customize.form.save.success'));
    },
  });
};

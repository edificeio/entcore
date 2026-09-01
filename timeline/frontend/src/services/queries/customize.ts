import { useToast } from '@edifice.io/react';
import { queryOptions, useMutation, useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useI18n } from '~/hooks/useI18n';
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

export const useCustomization = () => {
  const { t } = useI18n();
  const toast = useToast();

  const { data: languages, isError: isLanguagesError } = useQuery(
    customizeQueryOptions.getLanguages(),
  );
  const { data: fonts, isError: isFontsError } = useQuery(
    customizeQueryOptions.getFonts(),
  );
  const { data: backgrounds, isError: isBackgroundsError } = useQuery(
    customizeQueryOptions.getBackgrounds(),
  );

  const saveMutation = useMutation({
    mutationFn: async ({ lang }: { lang: string; font: string }) =>
      await Promise.all([customizeService.saveLanguagePreference(lang)]),
    onSuccess: () => {
      toast.success(t('homepage.customize.form.save.success'));
    },
    onError: () => {
      toast.error(t('homepage.customize.form.save.error'));
    },
  });

  useEffect(() => {
    if (isLanguagesError)
      toast.error(
        t('homepage.customize.form.load.error', { code: 'languages' }),
      );
    if (isFontsError)
      toast.error(t('homepage.customize.form.load.error', { code: 'fonts' }));
    if (isBackgroundsError)
      toast.error(
        t('homepage.customize.form.load.error', { code: 'backgrounds' }),
      );
  }, [isLanguagesError, isFontsError, isBackgroundsError]);

  return {
    languages,
    fonts,
    backgrounds,
    isError: isLanguagesError || isFontsError || isBackgroundsError,
    saveMutation,
  };
};

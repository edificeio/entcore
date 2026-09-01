import { useEdificeTheme, useToast } from '@edifice.io/react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { customizeService } from '~/services/api/customizeService';
import { customizeQueryOptions } from '~/services/queries/customize';
import { useI18n } from './useI18n';

export const useCustomization = () => {
  const { t } = useI18n();
  const toast = useToast();
  const { theme } = useEdificeTheme();

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
    mutationFn: async ({
      language,
      font,
      background,
    }: {
      language: string;
      font: string;
      background: string;
    }) => {
      if (!theme) throw 'Theme is undefined';

      return await Promise.all([
        customizeService.saveLanguagePreference(language),
        customizeService.saveFontPreference(theme.themeName, font),
        customizeService.saveBackgroundPreference(background),
      ]);
    },
    onSuccess: () => {
      toast.success(t('homepage.customize.form.save.success'));
    },
    onError: () => {
      toast.error(t('homepage.customize.form.save.error'));
    },
  });

  /** Display error toasts. */
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
    theme,
    isError: isLanguagesError || isFontsError || isBackgroundsError,
    saveMutation,
  };
};

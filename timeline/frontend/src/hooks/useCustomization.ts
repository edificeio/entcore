import {
  useEdificeTheme,
  useToast,
  useUserPreferences,
} from '@edifice.io/react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { Background, customizeService } from '~/services/api/customizeService';
import { customizeQueryOptions } from '~/services/queries/customize';
import { useI18n } from './useI18n';

export const useCustomization = () => {
  const { t } = useI18n();
  const toast = useToast();
  const { theme } = useEdificeTheme();
  const { preferences, isError: isPreferencesError } = useUserPreferences();

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
      background: Background;
    }) => {
      if (!theme) throw 'Theme is undefined';

      return customizeService.save({
        language,
        themeName: theme.themeName,
        font,
        background,
      });
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
    if (isPreferencesError)
      toast.error(
        t('homepage.customize.form.load.error', { code: 'preferences' }),
      );
  }, [isLanguagesError, isFontsError, isBackgroundsError, isPreferencesError]);

  return {
    languages,
    fonts,
    backgrounds,
    theme,
    background: (preferences?.background as Background) ?? 'default',
    isError:
      isLanguagesError ||
      isFontsError ||
      isBackgroundsError ||
      isPreferencesError,
    saveMutation,
  };
};

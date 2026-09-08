import { useToast, useUserPreferences } from '@edifice.io/react';
import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { Background } from '~/services/api/customizeService';
import { customizeQueryOptions } from '~/services/queries/customize';
import { useI18n } from './useI18n';

export type CustomizationPreferences = {
  background: Background;
  language: { 'default-domain': string };
};

export const useCustomization = () => {
  const { t } = useI18n();
  const toast = useToast();
  const {
    preferences,
    isError: isPreferencesError,
    savePreferences,
  } = useUserPreferences<CustomizationPreferences>();

  const { data: languages, isError: isLanguagesError } = useQuery(
    customizeQueryOptions.getLanguages(),
  );
  const { data: fonts, isError: isFontsError } = useQuery(
    customizeQueryOptions.getFonts(),
  );
  const { data: backgrounds, isError: isBackgroundsError } = useQuery(
    customizeQueryOptions.getBackgrounds(),
  );

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
  }, [
    isLanguagesError,
    isFontsError,
    isBackgroundsError,
    isPreferencesError,
    toast,
    t,
  ]);

  return {
    languages,
    fonts,
    backgrounds,
    background: (preferences?.background as Background) ?? 'default',
    isError:
      isLanguagesError ||
      isFontsError ||
      isBackgroundsError ||
      isPreferencesError,
    savePreferences,
  };
};

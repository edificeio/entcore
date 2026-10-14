import {
  Background,
  useToast,
  useUiOverride,
  useUserPreferences,
} from '@edifice.io/react';
import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { customizeQueryOptions } from '~/services/queries/customize';
import { useI18n } from './useI18n';

export type CustomizationPreferences = {
  background: Background;
  language: { 'default-domain': string };
};

export const useCustomization = () => {
  const { t } = useI18n();
  const toast = useToast();
  const { isError: isPreferencesError, savePreferences } =
    useUserPreferences<CustomizationPreferences>();

  const { data: languages, isError: isLanguagesError } = useQuery(
    customizeQueryOptions.getLanguages(),
  );
  const { data: fonts, isError: isFontsError } = useQuery(
    customizeQueryOptions.getFonts(),
  );
  // theme-conf variants are untyped strings: the platform config is trusted to set a Background.
  const platformBackground = useUiOverride('background.default')?.variant as
    | Background
    | undefined;
  const { data: backgrounds, isError: isBackgroundsError } = useQuery({
    ...customizeQueryOptions.getBackgrounds(),
    // Without a saved preference, the homepage shows the platform default: its tile must match.
    select: (list) =>
      platformBackground
        ? list.map((background) =>
            background === 'default' ? platformBackground : background,
          )
        : list,
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
    isError:
      isLanguagesError ||
      isFontsError ||
      isBackgroundsError ||
      isPreferencesError,
    savePreferences,
  };
};

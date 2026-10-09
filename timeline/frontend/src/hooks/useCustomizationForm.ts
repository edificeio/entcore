import {
  Background,
  useBackground,
  useEdificeClient,
  useEdificeTheme,
  useToast,
} from '@edifice.io/react';
import { useCallback, useEffect, useState } from 'react';
import i18n from '~/i18n';
import { customizeService } from '~/services';
import { useCustomization } from './useCustomization';
import { useI18n } from './useI18n';

const SAVE_SUCCESS_KEY = 'homepage.customize.save.success';

export function useCustomizationForm() {
  const {
    languages,
    backgrounds,
    fonts,
    isError: isLoadError,
    savePreferences,
  } = useCustomization();
  const { background } = useBackground();
  const { currentLanguage, sessionQuery } = useEdificeClient();
  const { theme } = useEdificeTheme();
  const { t } = useI18n();
  const toast = useToast();

  const [selectedLanguage, setSelectedLanguage] = useState(currentLanguage!);
  const [selectedBackground, setSelectedBackground] = useState(background);
  const [selectedFont, setSelectedFont] = useState(theme?.skinName);
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    if (currentLanguage) setSelectedLanguage(currentLanguage);
  }, [currentLanguage]);

  useEffect(() => {
    if (currentLanguage && i18n.language !== currentLanguage) {
      // Intentionally ignore the Promise; the effect only synchronizes the language.
      void i18n.changeLanguage(currentLanguage);
    }
  }, [currentLanguage]);

  useEffect(() => {
    if (!theme) return;
    setSelectedFont(theme.skinName);
  }, [theme]);

  useEffect(() => {
    setSelectedBackground(background);
  }, [background]);

  /** Show the success toast once the page has been reloaded after a save. */
  useEffect(() => {
    try {
      if (!sessionStorage.getItem(SAVE_SUCCESS_KEY)) return;
      sessionStorage.removeItem(SAVE_SUCCESS_KEY);
      toast.success(t('homepage.customize.form.save.success'));
    } catch {
      // Storage unavailable: the toast is not essential.
    }
  }, [toast, t]);

  const isDirty =
    selectedLanguage !== currentLanguage ||
    selectedBackground !== background ||
    selectedFont !== theme?.skinName;

  const saveChanges = useCallback(async () => {
    if (!selectedFont || !selectedLanguage || !selectedBackground || !theme) {
      return;
    }
    setIsSaving(true);
    try {
      await savePreferences({
        language: { 'default-domain': selectedLanguage },
        background: selectedBackground,
      });
      await i18n.changeLanguage(selectedLanguage);
      await sessionQuery.refetch();
      await customizeService.saveSkin(theme.themeName, selectedFont);
      try {
        sessionStorage.setItem(SAVE_SUCCESS_KEY, 'true');
      } catch {
        // Storage unavailable: the toast is not essential.
      }
      // Reload to apply the new preferences (skin, language, background) everywhere.
      window.location.reload();
    } catch {
      toast.error(t('homepage.customize.form.save.error'));
      setIsSaving(false);
    }
  }, [
    savePreferences,
    selectedBackground,
    selectedFont,
    selectedLanguage,
    sessionQuery,
    theme,
    toast,
    t,
  ]);

  return {
    isLoadError,
    languages,
    selectedLanguage,
    handleLanguageChange: (language: string) => setSelectedLanguage(language),
    backgrounds,
    selectedBackground,
    handleBackgroundChange: (background: Background) =>
      setSelectedBackground(background),
    fonts,
    selectedFont,
    handleFontChange: (font: string) => setSelectedFont(font),
    isDirty,
    saveChanges,
    isSaving,
  };
}

import { useEdificeClient, useEdificeTheme, useToast } from '@edifice.io/react';
import { useCallback, useEffect, useState } from 'react';
import i18n from '~/i18n';
import { Background, customizeService } from '~/services';
import { useCustomization } from './useCustomization';
import { useI18n } from './useI18n';

export function useCustomizationForm() {
  const {
    languages,
    backgrounds,
    fonts,
    isError: isLoadError,
    savePreferences,
    background,
  } = useCustomization();
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

  const resetChanges = useCallback(() => {
    if (theme) setSelectedFont(theme.skinName);
    if (currentLanguage) setSelectedLanguage(currentLanguage);
    if (background) setSelectedBackground(background);
  }, [theme, currentLanguage, background]);

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
      toast.success(t('homepage.customize.form.save.success'));
    } catch {
      toast.error(t('homepage.customize.form.save.error'));
    } finally {
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
    resetChanges,
    saveChanges,
    isSaving,
  };
}

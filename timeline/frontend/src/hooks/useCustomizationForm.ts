import { useEdificeClient, useEdificeTheme, useToast } from '@edifice.io/react';
import { useCallback, useEffect, useState } from 'react';
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
  const { currentLanguage } = useEdificeClient();
  const { theme } = useEdificeTheme();
  const { t } = useI18n();
  const toast = useToast();

  const [selectedLanguage, setSelectedLanguage] = useState(currentLanguage!);
  const [selectedBackground, setSelectedBackground] = useState(background);
  const [selectedFont, setSelectedFont] = useState(theme?.skinName);
  const [isSaving, setIsSaving] = useState(false);

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

  const saveChanges = useCallback(() => {
    async function saveAllPrefs() {
      if (selectedFont && selectedLanguage && selectedBackground && theme) {
        setIsSaving(true);
        try {
          await savePreferences({
            language: { 'default-domain': selectedLanguage },
            background: selectedBackground,
          });
          await customizeService.saveSkin(theme.themeName, selectedFont);
          toast.success(t('homepage.customize.form.save.success'));
        } catch {
          toast.error(t('homepage.customize.form.save.error'));
        } finally {
          setIsSaving(false);
        }
      }
    }
    saveAllPrefs();
  }, [savePreferences, selectedBackground, selectedFont, selectedLanguage]);

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

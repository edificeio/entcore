import { useEdificeClient } from '@edifice.io/react';
import { useCallback, useEffect, useState } from 'react';
import { Background } from '~/services';
import { useCustomization } from './useCustomization';

export function useCustomizationForm() {
  const {
    languages,
    backgrounds,
    fonts,
    isError: isLoadError,
    saveMutation,
    theme,
    background,
  } = useCustomization();
  const { currentLanguage } = useEdificeClient();

  const [selectedLanguage, setSelectedLanguage] = useState(currentLanguage!);
  const [selectedBackground, setSelectedBackground] = useState(background);
  const [selectedFont, setSelectedFont] = useState(theme?.skinName);

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
    if (selectedFont && selectedLanguage && selectedBackground) {
      saveMutation.mutateAsync({
        language: selectedLanguage,
        font: selectedFont,
        background: selectedBackground,
      });
    }
  }, [saveMutation, selectedBackground, selectedFont, selectedLanguage]);

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
    isPending: saveMutation.isPending,
  };
}

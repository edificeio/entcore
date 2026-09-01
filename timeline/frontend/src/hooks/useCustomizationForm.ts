import { useEdificeClient } from '@edifice.io/react';
import { useCallback, useEffect, useState } from 'react';
import { useCustomization } from './useCustomization';

export function useCustomizationForm() {
  const {
    languages,
    backgrounds,
    fonts,
    isError: isLoadError,
    saveMutation,
    theme,
  } = useCustomization();
  const { currentLanguage } = useEdificeClient();
  const background = 'TODO';

  const [selectedLanguage, setSelectedLanguage] = useState(currentLanguage!);
  const [selectedBackground, setSelectedBackground] = useState(background);
  const [selectedFont, setSelectedFont] = useState(theme?.skinName);

  useEffect(() => {
    if (!theme) return;
    setSelectedFont(theme.skinName);
  }, [theme]);

  const resetChanges = useCallback(() => {
    if (theme) setSelectedFont(theme.skinName);
    if (currentLanguage) setSelectedLanguage(currentLanguage);
  }, [theme, currentLanguage]);

  const saveChanges = useCallback(() => {
    if (selectedFont && selectedLanguage && selectedBackground) {
      saveMutation.mutate({
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
    handleBackgroundChange: (background: string) =>
      setSelectedBackground(background),
    fonts,
    selectedFont,
    handleFontChange: (font: string) => setSelectedFont(font),
    resetChanges,
    saveChanges,
    isPending: saveMutation.isPending,
  };
}

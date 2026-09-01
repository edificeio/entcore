import { useEdificeClient, useEdificeTheme } from '@edifice.io/react';
import { useCallback, useEffect, useState } from 'react';
import { useCustomization } from '~/services/queries/customize';

export function useCustomizationForm() {
  const {
    languages,
    backgrounds,
    fonts,
    isError: isLoadError,
    saveMutation,
  } = useCustomization();
  const { currentLanguage } = useEdificeClient();
  const { theme } = useEdificeTheme();
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
    if (selectedFont && selectedLanguage) {
      alert('todo : saveMutation ' + saveMutation.isIdle);
    }
  }, [selectedFont, selectedLanguage]);

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
  };
}

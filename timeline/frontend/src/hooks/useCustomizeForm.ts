import { useEdificeClient, useEdificeTheme } from '@edifice.io/react';
import { useCallback, useEffect, useState } from 'react';
import { useFonts, useLanguages } from '~/services/queries/customize';

export function useCustomizationForm() {
  const languagesQuery = useLanguages();
  const fontsQuery = useFonts();
  const { currentLanguage } = useEdificeClient();
  const { theme } = useEdificeTheme();

  const [selectedFont, setSelectedFont] = useState(theme?.skinName);
  const [selectedLanguage, setSelectedLanguage] = useState(currentLanguage!);

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
      alert('todo : query');
    }
  }, [selectedFont, selectedLanguage]);

  return {
    fonts: fontsQuery.data,
    selectedFont,
    handleFontChange: (font: string) => setSelectedFont(font),
    languages: languagesQuery.data,
    selectedLanguage,
    handleLanguageChange: (language: string) => setSelectedLanguage(language),
    resetChanges,
    saveChanges,
  };
}

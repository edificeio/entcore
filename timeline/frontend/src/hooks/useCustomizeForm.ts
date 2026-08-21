import { useEdificeClient } from '@edifice.io/react';
import { useState } from 'react';
import { useLanguages } from '~/services/queries/customize';

export function useCustomizeForm() {
  const languagesQuery = useLanguages();
  const { currentLanguage } = useEdificeClient();

  const [selectedLanguage, setSelectedLanguage] = useState(currentLanguage!);

  return {
    languages: languagesQuery.data,
    selectedLanguage,
    onLanguageChange: (language: string) => setSelectedLanguage(language),
  };
}

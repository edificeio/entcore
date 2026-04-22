import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  getLanguagePreference,
  saveLanguagePreference,
} from '~/services/api/language.api';
import { languagesQueryOptions } from '~/services/queries/language.queries';

const LANGUAGE_DISPLAY: Record<string, { label: string; countryCode: string }> =
  {
    en: { label: 'English', countryCode: 'gb' },
    fr: { label: 'Français', countryCode: 'fr' },
    es: { label: 'Espagnol', countryCode: 'es' },
    de: { label: 'Deutsch', countryCode: 'de' },
    pt: { label: 'Português', countryCode: 'pt' },
    it: { label: 'Italiano', countryCode: 'it' },
  };

export function useLanguagePreference() {
  const { i18n } = useTranslation();
  const [currentLang, setCurrentLang] = useState(i18n.language ?? 'fr');

  const { data: codes = [] } = useQuery(languagesQueryOptions());

  useEffect(() => {
    getLanguagePreference().then((lang) => {
      if (lang) {
        setCurrentLang(lang);
        i18n.changeLanguage(lang);
      }
    });
  }, [i18n]);

  const setLanguage = async (lang: string) => {
    setCurrentLang(lang);
    await saveLanguagePreference(lang);
    await i18n.changeLanguage(lang);
  };

  const languages = codes
    .filter((c) => LANGUAGE_DISPLAY[c])
    .map((c) => ({ code: c, ...LANGUAGE_DISPLAY[c] }));

  return { languages, currentLang, setLanguage };
}

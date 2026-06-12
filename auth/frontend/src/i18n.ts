import i18n from 'i18next';
import Backend from 'i18next-http-backend';
import { initReactI18next } from 'react-i18next';

// Languages for which the ENT provides localized content (UI + editorial message).
const SUPPORTED_LANGUAGES = ['fr', 'en', 'es', 'de', 'it', 'pt'];
const FALLBACK_LANGUAGE = 'fr';

/**
 * Resolves the language from the browser one (e.g. `en-US` → `en`),
 * falling back to French when it is not supported.
 * Mirrors the backend behavior, which relies on the `Accept-Language`
 * header (see ENABLING-892).
 */
export function detectBrowserLanguage(): string {
  const browserLang =
    (typeof navigator !== 'undefined' &&
      (navigator.language || navigator.languages?.[0])) ||
    FALLBACK_LANGUAGE;
  const base = browserLang.toLowerCase().split('-')[0];
  return SUPPORTED_LANGUAGES.includes(base) ? base : FALLBACK_LANGUAGE;
}

i18n
  .use(Backend)
  .use(initReactI18next)
  .init({
    backend: {
      loadPath: (_lngs: string[], namespaces: string[]) => {
        const urls = namespaces.map((namespace: string) => {
          if (namespace === 'common') {
            return `/i18n`;
          }
          return `/${namespace}/i18n`;
        });
        return urls;
      },
      parse: function (data: string) {
        return JSON.parse(data);
      },
    },
    defaultNS: 'common',
    // you can add name of the app directly in the ns array
    ns: ['common', 'auth'],
    fallbackLng: FALLBACK_LANGUAGE,
    lng: detectBrowserLanguage(),
    interpolation: {
      escapeValue: false,
      prefix: '[[',
      suffix: ']]',
    },
    debug: false,
  });

export default i18n;

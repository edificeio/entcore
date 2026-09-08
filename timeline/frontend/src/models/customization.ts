/** `_id` of the dyslexic font, as returned by GET /themes (see `useCustomization`). */
export const DYSLEXIC_FONT_ID = 'dyslexic';

type CustomizationPreviewLanguage =
  | 'fr'
  | 'en'
  | 'es'
  | 'de'
  | 'pt'
  | 'it'
  | 'co';

type CustomizationPreviewTexts = {
  greetingText: string;
  lastInfosText: string;
};

/**
 * Greeting / last infos preview texts, hardcoded per language (see GET
 * /languages). No translation-fetch mechanism exists yet for these two
 * homepage labels (see #IMPULS-6022): these are best-effort translations,
 * not pulled from the app's official i18n bundles.
 */
const CUSTOMIZATION_PREVIEW_TEXTS: Record<
  CustomizationPreviewLanguage,
  CustomizationPreviewTexts
> = {
  fr: { greetingText: 'Bonjour', lastInfosText: 'Dernières actualités' },
  en: { greetingText: 'Hello', lastInfosText: 'Latest news' },
  es: { greetingText: 'Hola', lastInfosText: 'Últimas noticias' },
  de: { greetingText: 'Hallo', lastInfosText: 'Neueste Nachrichten' },
  pt: { greetingText: 'Olá', lastInfosText: 'Últimas notícias' },
  it: { greetingText: 'Ciao', lastInfosText: 'Ultime notizie' },
  co: { greetingText: 'Bonghjornu', lastInfosText: 'Ultime nutizie' },
};

const DEFAULT_CUSTOMIZATION_PREVIEW_LANGUAGE: CustomizationPreviewLanguage =
  'fr';

/** Preview texts for `language` (a `_id` from GET /languages), falling back to French. */
export function getCustomizationPreviewTexts(
  language: string,
): CustomizationPreviewTexts {
  return (
    (CUSTOMIZATION_PREVIEW_TEXTS as Record<string, CustomizationPreviewTexts>)[
      language
    ] ?? CUSTOMIZATION_PREVIEW_TEXTS[DEFAULT_CUSTOMIZATION_PREVIEW_LANGUAGE]
  );
}

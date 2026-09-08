import { DYSLEXIC_FONT_ID } from '~/models/customization';

/**
 * Fixtures for GET /languages and GET /themes (page /customize).
 */
export const mockLanguages = ['fr', 'en', 'es', 'de', 'pt', 'it', 'co'];

export const mockThemes = [
  { _id: 'default', displayName: 'Défaut', path: '/assets/themes/default' },
  {
    _id: DYSLEXIC_FONT_ID,
    displayName: 'Dyslexique',
    path: '/assets/themes/dyslexic',
  },
];

import { HttpResponse, http } from 'msw';

/**
 * i18next backend endpoints (`src/i18n.ts`). Test-only: under vitest there is
 * no Vite dev server to serve them, unlike in the browser where
 * `serveLocalI18nPlugin` (vite.config.ts) already reads the real JSON files
 * from disk — see ../../README.md.
 */
export const handlers = [
  http.get('/i18n', () => HttpResponse.json({})),
  http.get('/timeline/i18n', () => HttpResponse.json({})),
];

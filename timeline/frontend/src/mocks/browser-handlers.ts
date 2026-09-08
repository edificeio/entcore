import { applyDelayMiddleware } from './middleware';
import { platformBootstrapHandlers, platformDataHandlers } from './platform';

/**
 * Browser-only aggregate consumed by browser.ts (setupWorker).
 *
 * No i18n handler here: in dev, serveLocalI18nPlugin (vite.config.ts) already
 * serves /i18n and /timeline/i18n from the real JSON files on disk, so MSW
 * lets those requests pass through.
 *
 * Only the data handlers get the artificial network delay — delaying the
 * bootstrap chain (session/theme/conf) would just slow down every dev
 * reload for no benefit.
 */
export const browserHandlers = [
  ...applyDelayMiddleware([...platformDataHandlers]),
  ...platformBootstrapHandlers,
];

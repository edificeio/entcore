import { appTestHandlers } from './app';
import { platformBootstrapHandlers, platformDataHandlers } from './platform';

/**
 * Test-only aggregate consumed by server.ts (setupServer). No artificial
 * delay — tests should stay fast and deterministic. i18n handlers are
 * required here since there is no Vite dev server under vitest.
 */
export const testHandlers = [
  ...platformBootstrapHandlers,
  ...platformDataHandlers,
  ...appTestHandlers,
];

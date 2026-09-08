/**
 * Everything in this folder is specific to the timeline front — see
 * ../README.md for the platform/ vs app/ rule. Nothing here is a candidate
 * for mutualisation in the framework.
 */
export * from './handlers';

import { i18nHandlers } from './handlers';

/** Test-only handlers (no Vite dev server under vitest). */
export const appTestHandlers = [...i18nHandlers];

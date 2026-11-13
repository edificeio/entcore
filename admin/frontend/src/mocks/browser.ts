import { setupWorker } from 'msw/browser';

import { moderationHandlers } from './moderation/handlers';

/**
 * Browser worker used by `pnpm dev:mock` only, for APIs that are not available yet.
 * Requests without handler go through to the Vite proxy.
 */
export const worker = setupWorker(...moderationHandlers);

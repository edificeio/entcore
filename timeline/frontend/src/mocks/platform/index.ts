/**
 * Everything in this folder mocks a platform / `@edifice.io/*` endpoint —
 * nothing here is specific to the timeline front. See ../README.md for the
 * platform/ vs app/ rule. This is the folder to move wholesale into
 * `@edifice.io/react/mocks` once that subpath exists (see plan §7).
 */
export * from './data';
export * from './handlers';

import {
  actualitesHandlers,
  authHandlers,
  commonHandlers,
  directoryHandlers,
  themeHandlers,
  timelineHandlers,
  userbookHandlers,
  workspaceHandlers,
} from './handlers';

/** Bootstrap chain (session + conf) — kept delay-free even in browser mode. */
export const platformBootstrapHandlers = [
  ...authHandlers,
  ...themeHandlers,
  ...commonHandlers,
  ...userbookHandlers,
  ...directoryHandlers,
  ...workspaceHandlers,
];

/** Homepage data — gets the artificial network delay in browser mode. */
export const platformDataHandlers = [
  ...timelineHandlers,
  ...actualitesHandlers,
];

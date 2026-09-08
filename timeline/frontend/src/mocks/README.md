# Mocks

Two aggregates, never crossed: `browser-handlers.ts` (consumed by `browser.ts` /
`setupWorker`, used by `pnpm dev:mock`) and `test-handlers.ts` (consumed by
`server.ts` / `setupServer`, used by vitest via `setup.ts`). Both are built
from the same `platform/` + `app/` handlers and fixtures — only the
transport, the artificial network delay (browser only), and the i18n
handlers (test only) differ.

## `platform/` vs `app/`

**A handler goes in `platform/` if it mocks a platform endpoint, or an
endpoint consumed by an `@edifice.io/*` component** (session/theme/conf
bootstrap, notifications, flash messages, last infos, userbook preferences,
children...). It goes in `app/` **only if its content or its URL has no
meaning outside the timeline front**.

In practice `app/` currently holds a single thing: the i18n handlers
(`/i18n`, `/timeline/i18n`), and only for tests — in the browser,
`serveLocalI18nPlugin` (`vite.config.ts`) already serves those routes from
the real JSON files on disk, so MSW lets them pass through. Everything else,
including endpoints that look timeline-specific
(`/timeline/lastNotifications`, `/timeline/types`, `/timeline/flashmsg/*`,
`/userbook/preference/timeline`) or app-specific (`/languages`, `/themes`),
is consumed by containers/services living in `@edifice.io/react` or
`@edifice.io/client`, not by code in this repo — so it belongs to
`platform/`.

`platform/`'s file layout deliberately mirrors
`edifice-frontend-framework/packages/config/src/msw` (same file names, same
`mockXxx` naming convention, same `export const handlers = [...]` shape).
That package isn't consumable today (`@edifice.io/config` is `private: true`
and never published), so `platform/` is a local stand-in. The day a public
subpath exists (tracked separately, not in this task's scope), the swap is
mechanical: delete `platform/`, and in `browser-handlers.ts` /
`test-handlers.ts` replace the `./platform` import with the framework's —
no other file changes.

## Files

- `platform/data/`, `platform/handlers/` — fixtures and handlers reusable by
  any Edifice front.
- `app/handlers/` — handlers specific to this repo.
- `middleware.ts` — `applyDelayMiddleware`, adds a 500ms delay to data
  handlers in browser mode only. A repo-local dev utility, not a mock — it
  never moves to `platform/`.
- `browser.ts` / `server.ts` — the two transports.
- `setup.ts`, `renderWithRouter.tsx` — vitest wiring, unrelated to the
  platform/app split.

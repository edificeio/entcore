import { existsSync, readFileSync } from 'node:fs';
import type { Plugin } from 'vite';

export type ServeLocalI18nOptions = {
  /** Dev-server route to intercept, e.g. `/boilerplate/i18n`. */
  route: string;
  /** Absolute path to the local i18n JSON file to serve. */
  filePath: string;
};

/**
 * Dev-only middleware that serves a local i18n JSON file on the given route,
 * instead of letting the request hit the dev proxy (the remote backend).
 *
 * Useful to work on the module translations without a running backend. When
 * the local file does not exist, the request falls through to the next
 * middleware (typically the proxy), so a fresh clone keeps working as-is.
 */
export function serveLocalI18n({
  route,
  filePath,
}: ServeLocalI18nOptions): Plugin {
  return {
    name: 'serve-local-i18n',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use(route, (_req, res, next) => {
        if (!existsSync(filePath)) {
          next();
          return;
        }
        res.setHeader('Content-Type', 'application/json; charset=utf-8');
        res.end(readFileSync(filePath, 'utf-8'));
      });
    },
  };
}

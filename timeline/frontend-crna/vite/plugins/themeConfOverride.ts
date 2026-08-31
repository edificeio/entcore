import { runInNewContext } from 'node:vm';
import type { Plugin } from 'vite';

type ThemeConf = {
  overriding?: Array<{ uiOverrides?: Record<string, unknown> }>;
};

export type ThemeConfOverrideOptions = {
  /** Platform serving the original `/assets/theme-conf.js` (VITE_RECETTE). */
  target?: string;
  /** JSON object merged into every theme's `uiOverrides` (VITE_UI_OVERRIDES). */
  overrides?: string;
};

/**
 * Dev only: serves the platform `/assets/theme-conf.js` with extra
 * `uiOverrides`, to try theme-conf driven variants without touching the
 * platform. Falls through to the regular proxy when disabled or on error.
 */
export function themeConfOverridePlugin({
  target,
  overrides,
}: ThemeConfOverrideOptions): Plugin {
  return {
    name: 'theme-conf-override',
    apply: 'serve',
    configureServer(server) {
      if (!target || !overrides) return;
      const uiOverrides = JSON.parse(overrides) as Record<string, unknown>;

      server.middlewares.use(
        '/assets/theme-conf.js',
        async (_req, res, next) => {
          try {
            const response = await fetch(
              new URL('/assets/theme-conf.js', target),
            );
            if (!response.ok) throw new Error(`HTTP ${response.status}`);

            const sandbox: { exports: { conf?: ThemeConf } } = { exports: {} };
            runInNewContext(await response.text(), sandbox);
            const conf = sandbox.exports.conf ?? {};
            conf.overriding?.forEach((theme) => {
              theme.uiOverrides = { ...theme.uiOverrides, ...uiOverrides };
            });

            res.setHeader('Content-Type', 'application/javascript');
            res.setHeader('Cache-Control', 'no-store');
            res.end(`exports.conf = ${JSON.stringify(conf, null, 2)};\n`);
          } catch (error) {
            server.config.logger.warn(
              `[theme-conf-override] ${String(error)}, falling back to proxy`,
            );
            next();
          }
        },
      );
    },
  };
}

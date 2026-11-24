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
      // Last successfully overridden file, served again when the platform is
      // unreachable. Never fall through to the proxy: its raw response is sent
      // with `max-age=14400` and would stick in the browser cache for hours.
      let lastGood: string | undefined;

      server.middlewares.use('/assets/theme-conf.js', async (_req, res) => {
        res.setHeader('Content-Type', 'application/javascript');
        res.setHeader('Cache-Control', 'no-store');
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

          lastGood = `exports.conf = ${JSON.stringify(conf, null, 2)};\n`;
          res.end(lastGood);
        } catch (error) {
          server.config.logger.warn(
            `[theme-conf-override] ${String(error)}, ${lastGood ? 'serving the last overridden version' : 'no version to serve yet'}`,
          );
          if (lastGood) {
            res.end(lastGood);
          } else {
            res.statusCode = 503;
            res.end('// theme-conf unavailable, retry later\n');
          }
        }
      });
    },
  };
}

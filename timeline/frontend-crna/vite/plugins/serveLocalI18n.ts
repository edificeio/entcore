import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { Plugin } from 'vite';

type ServeLocalI18nRoute = {
  routePath: string;
  filePath: string;
};

type ServeLocalI18nPluginOptions = {
  routes: ServeLocalI18nRoute[];
  rootDir?: string;
};

export const serveLocalI18nPlugin = ({
  routes,
  rootDir,
}: ServeLocalI18nPluginOptions): Plugin => {
  return {
    name: `serve-local-i18n`,
    configureServer(server) {
      const baseDir = rootDir ?? server.config.root ?? process.cwd();
      const resolvedRoutes = routes.map(({ routePath, filePath }) => ({
        routePath,
        filePath: resolve(baseDir, filePath),
      }));

      server.middlewares.use((req, res, next) => {
        const matchingRoute = resolvedRoutes.find(({ routePath }) =>
          req.url?.startsWith(routePath),
        );

        if (matchingRoute) {
          try {
            const fileContents = readFileSync(matchingRoute.filePath, 'utf-8');
            res.setHeader('Content-Type', 'application/json; charset=utf-8');
            res.end(fileContents);
            return;
          } catch (err) {
            next(err);
            return;
          }
        }
        next();
      });
    },
  };
};

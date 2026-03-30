import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { Plugin } from 'vite';

type ServeLocalJsonPluginOptions = {
  routePath: string;
  filePath: string;
  rootDir?: string;
};

export const serveLocalI18nPlugin = ({
  routePath,
  filePath,
  rootDir,
}: ServeLocalJsonPluginOptions): Plugin => {
  return {
    name: `serve-local-i18n`,
    configureServer(server) {
      const resolvedFilePath = resolve(
        rootDir ?? server.config.root ?? process.cwd(),
        filePath,
      );
      server.middlewares.use((req, res, next) => {
        if (req.url?.startsWith(routePath)) {
          try {
            const fileContents = readFileSync(resolvedFilePath, 'utf-8');
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

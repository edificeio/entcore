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
}: ServeLocalJsonPluginOptions): Plugin => {
  const resolvedFilePath = resolve(process.cwd(), filePath);
  return {
    name: `serve-local-i18n`,
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        if (req.url?.startsWith(routePath)) {
          res.setHeader('Content-Type', 'application/json; charset=utf-8');
          res.end(readFileSync(resolvedFilePath, 'utf-8'));
          return;
        }
        next();
      });
    },
  };
};

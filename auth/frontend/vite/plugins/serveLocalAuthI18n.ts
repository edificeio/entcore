import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { Plugin } from 'vite';

export function serveLocalAuthI18n(rootDir: string): Plugin {
  return {
    name: 'serve-local-auth-i18n',
    configureServer(server) {
      server.middlewares.use('/auth/i18n', (_req, res) => {
        const filePath = resolve(rootDir, '../src/main/resources/i18n/fr.json');
        res.setHeader('Content-Type', 'application/json; charset=utf-8');
        res.end(readFileSync(filePath, 'utf-8'));
      });
    },
  };
}

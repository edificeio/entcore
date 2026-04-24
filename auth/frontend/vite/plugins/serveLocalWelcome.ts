import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { Plugin } from 'vite';

export function serveLocalWelcome(rootDir: string): Plugin {
  return {
    name: 'serve-local-welcome',
    configureServer(server) {
      server.middlewares.use('/auth/configure/welcome', (_req, res) => {
        const filePath = resolve(rootDir, 'src/mocks/welcome.json');
        res.setHeader('Content-Type', 'application/json; charset=utf-8');
        res.end(readFileSync(filePath, 'utf-8'));
      });
    },
  };
}

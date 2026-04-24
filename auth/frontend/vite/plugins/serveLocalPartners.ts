import { createReadStream, existsSync } from 'node:fs';
import { extname, resolve } from 'node:path';
import type { Plugin } from 'vite';

const MIME: Record<string, string> = {
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.gif': 'image/gif',
  '.webp': 'image/webp',
};

export function serveLocalPartners(rootDir: string): Plugin {
  return {
    name: 'serve-local-partners',
    configureServer(server) {
      server.middlewares.use('/assets/partners', (req, res, next) => {
        const filePath = resolve(
          rootDir,
          'src/assets/partners',
          req.url?.replace(/^\//, '') ?? '',
        );
        if (existsSync(filePath)) {
          res.setHeader('Content-Type', MIME[extname(filePath)] ?? 'application/octet-stream');
          createReadStream(filePath).pipe(res);
        } else {
          next();
        }
      });
    },
  };
}

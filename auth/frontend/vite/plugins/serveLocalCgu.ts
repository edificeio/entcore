import { createReadStream, existsSync } from 'node:fs';
import { extname, resolve } from 'node:path';
import type { Plugin } from 'vite';

const MIME: Record<string, string> = {
  '.html': 'text/html',
  '.htm': 'text/html',
  '.pdf': 'application/pdf',
  '.txt': 'text/plain',
};

export function serveLocalCgu(rootDir: string): Plugin {
  return {
    name: 'serve-local-cgu',
    configureServer(server) {
      server.middlewares.use('/assets/cgu', (req, res, next) => {
        const filePath = resolve(
          rootDir,
          'src/assets/cgu',
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

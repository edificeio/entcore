import { createHash } from 'node:crypto';
import { Plugin } from 'vite';

export function hashEdificeBootstrap({ hash }: { hash: string }): Plugin {
  return {
    name: 'vite-plugin-edifice',
    apply: 'build',
    transformIndexHtml(html) {
      return html.replace(
        '/assets/themes/edifice-bootstrap/index.css',
        `/assets/themes/edifice-bootstrap/index.css?${hash}`,
      );
    },
  };
}

const hash = createHash('md5')
  .update(Date.now().toString())
  .digest('hex')
  .substring(0, 8);

export const queryHashVersion = `v=${hash}`;

import type { Plugin } from 'vite';

export function rewriteWayfV2(): Plugin {
  return {
    name: 'rewrite-wayfv2',
    configureServer(server) {
      server.middlewares.use((req, _res, next) => {
        if (req.url === '/' || req.url?.startsWith('/saml/wayf')) {
          req.url = '/wayfv2.html';
        }
        next();
      });
    },
  };
}

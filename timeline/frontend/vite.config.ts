import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vitest/config';
import { createDevProxyConfig } from './vite/plugins/devProxy';
import { serveLocalI18nPlugin } from './vite/plugins/serveLocalI18n';

export default ({ mode }: { mode: string }) => {
  const { headers, proxy } = createDevProxyConfig({
    mode,
    routes: [
      '/conf/public',
      '^/(?=applications-list)',
      '^/(?=assets)',
      '^/(?=theme|locale|i18n|skin)',
      '^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)',
      '^/calendar/(?!public/)',
    ],
  });

  return defineConfig({
    base: mode === 'production' ? '/timeline' : '',
    root: __dirname,
    cacheDir: './node_modules/.vite/timeline',

    resolve: {
      alias: {
        '@images': resolve(
          __dirname,
          'node_modules/@edifice.io/bootstrap/dist/images',
        ),
      },
    },

    server: {
      port: 4200,
      host: 'localhost',
      headers,
      proxy,
    },

    preview: {
      port: 4300,
      headers,
      host: 'localhost',
    },

    plugins: [
      serveLocalI18nPlugin({
        routes: [
          {
            routePath: '/timeline/i18n',
            filePath: '../src/main/resources/i18n/timeline/fr.json',
          },
          {
            routePath: '/i18n',
            filePath: '../../portal/backend/src/main/resources/i18n/fr.json',
          },
        ],
        rootDir: __dirname,
      }),
      {
        name: 'rewrite-homepage',
        configureServer(server) {
          server.middlewares.use((req, _res, next) => {
            if (req.url === '/') {
              req.url = '/homepage.html';
            }
            next();
          });
        },
      },
      react(),
      tsconfigPaths(),
    ],

    build: {
      outDir: './dist',
      emptyOutDir: true,
      reportCompressedSize: true,
      commonjsOptions: {
        transformMixedEsModules: true,
      },
      assetsDir: 'public/homepage',
      chunkSizeWarningLimit: 500,
      rollupOptions: {
        input: {
          main: resolve(__dirname, 'homepage.html'),
        },
      },
    },

    test: {
      environment: 'jsdom',
      globals: true,
      include: ['src/**/*.test.{ts,tsx}'],
      setupFiles: ['./src/mocks/setup.ts'],
      watch: false,
      clearMocks: true,
      restoreMocks: true,
      reporters: ['default'],
      coverage: {
        reportsDirectory: './coverage/timeline',
        provider: 'v8',
      },
      server: {
        deps: {
          inline: ['@edifice.io/react'],
        },
      },
    },
  });
};

import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { loadEnv } from 'vite';
import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vitest/config';
import { createDevProxyConfig } from './vite/plugins/devProxy';
import { serveLocalI18nPlugin } from './vite/plugins/serveLocalI18n';
import { themeConfOverridePlugin } from './vite/plugins/themeConfOverride';
import { widgetMocksPlugin } from './vite/plugins/widgetMocks';

export default ({ mode }: { mode: string }) => {
  const env = loadEnv(mode, process.cwd());
  const { headers, proxy } = createDevProxyConfig({
    mode,
    routes: [
      '/conf/public',
      '^/(?=applications-list)',
      '^/(?=assets)',
      '^/(?=theme|locale|i18n|skin)',
      '^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)',
      '^/calendar/(?!public/)',
      '/sso/pronote',
      '/mediacentre',
      '/actualites',
      '/bookmark',
      '/themes',
      '/languages',
      '/community',
      '/communities',
      '/edt',
      '/viescolaire',
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
      dedupe: [
        'react',
        'react-dom',
        '@tanstack/react-query',
        'react-i18next',
        'react-hook-form',
        '@react-spring/web',
      ],
    },

    server: {
      port: 4200,
      host: 'localhost',
      // devProxy sends `max-age=300` on every response, which makes the
      // browser run stale modules (and mix two React copies) after a restart.
      headers: { ...headers, 'Cache-Control': 'no-cache' },
      proxy,
      fs: {
        allow: [
          '.',
          resolve(__dirname, '../../../edifice-frontend-framework/packages'),
        ],
      },
    },

    preview: {
      port: 4300,
      headers,
      host: 'localhost',
    },

    plugins: [
      themeConfOverridePlugin({
        target: env.VITE_RECETTE,
        overrides: env.VITE_UI_OVERRIDES,
      }),
      widgetMocksPlugin({ enabled: env.VITE_MOCK_WIDGETS === 'true' }),
      serveLocalI18nPlugin({
        routes: [
          {
            routePath: '/timeline/i18n',
            filePath: '../src/main/resources/i18n/fr.json',
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
              req.url = '/homepage-crna.html';
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
      assetsDir: 'public/homepage-crna',
      chunkSizeWarningLimit: 500,
      rollupOptions: {
        input: {
          main: resolve(__dirname, 'homepage-crna.html'),
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

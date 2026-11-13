import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { defineConfig } from 'vitest/config';
import { createDevProxyConfig } from './vite/plugins/devProxy.ts';
import { serveLocalI18n } from './vite/plugins/serveLocalI18n.ts';

// https://vitejs.dev/config/
export default ({ mode }: { mode: string }) => {
  const { headers, proxy } = createDevProxyConfig({
    mode,
    routes: [
      '/applications-list',
      '/conf/public',
      '^/(?=help-1d|help-2d)',
      '^/(?=assets)',
      '^/(?=theme|locale|i18n|skin)',
      '^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)',
      '/explorer',
      '/admin',
      '/moderation',
    ],
  });

  return defineConfig({
    base: mode === 'production' ? '/admin' : '',
    root: import.meta.dirname,
    // Own cache for mock mode, so `dev` and `dev:mock` can run side by side
    cacheDir:
      mode === 'mock'
        ? './node_modules/.vite/admin-mock'
        : './node_modules/.vite/admin',

    // msw/browser is lazily imported in mock mode: pre-bundle it to avoid a late re-optimization
    optimizeDeps: mode === 'mock' ? { include: ['msw/browser'] } : undefined,

    resolve: {
      tsconfigPaths: true,
      dedupe: [
        'react',
        'react-dom',
        '@edifice.io/react',
        '@edifice.io/client',
        '@tanstack/react-query',
        'react-hook-form',
        'react-i18next',
      ],
      alias: {
        '@images': resolve(
          import.meta.dirname,
          'node_modules/@edifice.io/bootstrap/dist/images',
        ),
      },
    },

    server: {
      fs: {
        /**
         * Allow the server to access the node_modules folder (for the images)
         * This is a solution to allow the server to access the images and fonts of the bootstrap package for 1D theme
         */
        allow: ['../../'],
      },
      proxy,
      // 4200 is taken by the legacy Angular admin (ng serve, src/main/ts)
      port: 4201,
      strictPort: true,
      headers,
      host: 'localhost',
    },

    preview: {
      port: 4300,
      headers,
      host: 'localhost',
    },

    plugins: [
      serveLocalI18n({
        route: '/admin/i18n',
        filePath: resolve(
          import.meta.dirname,
          '../src/main/resources/i18n/fr.json',
        ),
      }),
      react(),
    ],

    build: {
      outDir: './dist',
      emptyOutDir: true,
      reportCompressedSize: true,
      commonjsOptions: {
        transformMixedEsModules: true,
      },
      assetsDir: 'public',
      chunkSizeWarningLimit: 4000,
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
        reportsDirectory: './coverage/admin',
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

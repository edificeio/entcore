import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vitest/config';
import { createDevProxyConfig } from './vite/plugins/devProxy';
import { rewriteWayfV2 } from './vite/plugins/rewriteWayfV2';
import { serveLocalAuthI18n } from './vite/plugins/serveLocalAuthI18n';
import { serveLocalPartners } from './vite/plugins/serveLocalPartners';
import { serveLocalWelcome } from './vite/plugins/serveLocalWelcome';

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
      '^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|workspace|infra)',
      '/explorer',
      '/auth',
    ],
  });

  return defineConfig({
    base: mode === 'production' ? '/auth' : '',
    root: __dirname,
    cacheDir: './node_modules/.vite/auth',

    resolve: {
      alias: {
        '@images': resolve(
          __dirname,
          'node_modules/@edifice.io/bootstrap/dist/images',
        ),
      },
    },

    server: {
      fs: {
        allow: ['../../'],
      },
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
      serveLocalAuthI18n(__dirname),
      serveLocalWelcome(__dirname),
      serveLocalPartners(__dirname),
      rewriteWayfV2(),
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
      assetsDir: 'public/wayfv2',
      chunkSizeWarningLimit: 500,
      rollupOptions: {
        input: {
          main: resolve(__dirname, 'wayfv2.html'),
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
        reportsDirectory: './coverage/auth',
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

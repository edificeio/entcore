/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { defineConfig, loadEnv } from 'vite';
import tsconfigPaths from 'vite-tsconfig-paths';
import {
  hashEdificeBootstrap,
  queryHashVersion,
} from './plugins/vite-plugin-edifice';

// https://vitejs.dev/config/
export default ({ mode }: { mode: string }) => {
  // Checking environement files
  const envFile = loadEnv(mode, process.cwd());
  const envs = { ...process.env, ...envFile };
  const hasEnvFile = Object.keys(envFile).length;

  // Proxy variables
  const headers = hasEnvFile
    ? {
        'set-cookie': [
          `oneSessionId=${envs.VITE_ONE_SESSION_ID}`,
          `XSRF-TOKEN=${envs.VITE_XSRF_TOKEN}`,
        ],
        'Cache-Control': 'public, max-age=300',
      }
    : {};

  const proxyObj = hasEnvFile
    ? {
        target: envs.VITE_RECETTE,
        changeOrigin: true,
        headers: {
          cookie: `oneSessionId=${envs.VITE_ONE_SESSION_ID};authenticated=true; XSRF-TOKEN=${envs.VITE_XSRF_TOKEN}`,
        },
      }
    : {
        target: 'http://localhost:8090',
        changeOrigin: false,
      };

  return defineConfig({
    base: mode === 'production' ? '/conversation' : '',
    root: __dirname,
    cacheDir: './node_modules/.vite/conversation',

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
        /**
         * Allow the server to access the node_modules folder (for the images)
         * This is a solution to allow the server to access the images and fonts of the bootstrap package for 1D theme
         */
        allow: ['../../'],
      },
      proxy: {
        '/applications-list': proxyObj,
        '/conf/public': proxyObj,
        '^/(?=help-1d|help-2d|assets|theme|locale|i18n|skin|explorer)':
          proxyObj,
        '^/(?=auth|appregistry|cas|userbook|directory|communication|portal|session|timeline|workspace|infra)':
          proxyObj,
        '^/conversation/(?=api/|messages/|message|folders/|folder$|folder/trash|trash$|restore$|delete$|move$|move/|count/|toggleUnread|i18n|draft|draft/|conf/public|emptyTrash|max-depth)':
          proxyObj,
      },
      port: 4200,
      headers,
      host: 'localhost',
    },

    preview: {
      port: 4300,
      headers,
      host: 'localhost',
    },

    plugins: [
      react(),
      tsconfigPaths(),
      hashEdificeBootstrap({
        hash: queryHashVersion,
      }),
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
      rollupOptions: {
        external: ['edifice-ts-client'],
        output: {
          inlineDynamicImports: true,
          paths: {
            'edifice-ts-client': `/assets/js/edifice-ts-client/index.js?${queryHashVersion}`,
          },
        },
      },
    },

    test: {
      watch: false,
      globals: true,
      environment: 'jsdom',
      include: ['src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}'],
      setupFiles: ['./src/mocks/setup.ts'],
      reporters: ['default'],
      coverage: {
        reportsDirectory: './coverage/conversation',
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

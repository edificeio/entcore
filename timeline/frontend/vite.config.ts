import react from '@vitejs/plugin-react';
import { resolve } from 'node:path';
import { loadEnv, ProxyOptions } from 'vite';
import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vitest/config';

// https://vitejs.dev/config/
export default ({ mode }: { mode: string }) => {
  // Checking environment files
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

  const proxyObj: ProxyOptions = hasEnvFile
    ? {
        target: envs.VITE_RECETTE,
        changeOrigin: true,
        headers: {
          cookie: `oneSessionId=${envs.VITE_ONE_SESSION_ID};authenticated=true; XSRF-TOKEN=${envs.VITE_XSRF_TOKEN}`,
        },
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setHeader('X-XSRF-TOKEN', envs.VITE_XSRF_TOKEN || '');
          });
        },
      }
    : {
        target: 'http://localhost:8090',
        changeOrigin: false,
      };

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
        '^/(?=help-1d|help-2d)': proxyObj,
        '^/(?=assets)': proxyObj,
        '^/(?=theme|locale|i18n|skin)': proxyObj,
        '^/(?=auth|appregistry|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace|infra)':
          proxyObj,
        '/explorer': proxyObj,
        '/timeline': proxyObj,
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

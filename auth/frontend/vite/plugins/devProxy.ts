import { loadEnv, type ProxyOptions } from 'vite';

type EnvValues = Record<string, string | undefined>;

export type CreateDevProxyOptions = {
  /** Paths/regex-like keys to proxy with the same proxy target/options. */
  routes: string[];
  /** Vite mode used to load env files. */
  mode: string;
  /** Fallback target when env file is missing. */
  defaultTarget?: string;
};

export type DevProxyConfig = {
  headers: Record<string, string | string[]>;
  proxyObj: ProxyOptions;
  proxy: Record<string, ProxyOptions>;
};

/** Creates reusable dev proxy configuration for Entcore modules. */
export function createDevProxyConfig({
  mode,
  routes,
  defaultTarget = 'http://localhost:8090',
}: CreateDevProxyOptions): DevProxyConfig {
  const envFile = loadEnv(mode, process.cwd());
  const envs: EnvValues = { ...process.env, ...envFile };
  const hasEnvFile = Object.keys(envFile).length > 0;

  const setCookie = [
    `oneSessionId=${envs.VITE_ONE_SESSION_ID ?? ''}`,
    `XSRF-TOKEN=${envs.VITE_XSRF_TOKEN ?? ''}`,
    'authenticated=true',
  ];

  const headers: Record<string, string | string[]> = hasEnvFile
    ? {
        'set-cookie': setCookie,
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
        target: defaultTarget,
        changeOrigin: false,
      };

  const proxy = routes.reduce<Record<string, ProxyOptions>>((acc, route) => {
    acc[route] = proxyObj;
    return acc;
  }, {});

  return { headers, proxyObj, proxy };
}

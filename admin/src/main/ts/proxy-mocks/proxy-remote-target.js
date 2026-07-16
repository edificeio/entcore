// If VITE_RECETTE is set, override the proxy target to point at a remote
// server instead of localhost:8090, forwarding the recette session/XSRF
// cookies so authenticated requests work against that environment.
const applyRemoteTarget = (env, proxies) => {
  const { VITE_RECETTE, VITE_XSRF_TOKEN, VITE_ONE_SESSION_ID } = env;
  if (!VITE_RECETTE) {
    return;
  }

  console.log("Using remote proxy configuration target: ", VITE_RECETTE);
  proxies.forEach((proxy) => {
    proxy.target = VITE_RECETTE;
  });

  if (VITE_ONE_SESSION_ID && VITE_XSRF_TOKEN) {
    const cookie = `oneSessionId=${VITE_ONE_SESSION_ID}; authenticated=true; XSRF-TOKEN=${VITE_XSRF_TOKEN}`;
    const onProxyRes = (proxyRes, req, res) => {
      proxyRes.headers["set-cookie"] = [
        `oneSessionId=${VITE_ONE_SESSION_ID}`,
        `XSRF-TOKEN=${VITE_XSRF_TOKEN}`,
      ];
    };
    proxies.forEach((proxy) => {
      proxy.headers = { cookie };
      proxy.onProxyRes = onProxyRes;
    });
  }
};

module.exports = { applyRemoteTarget };

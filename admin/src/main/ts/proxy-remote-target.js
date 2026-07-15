// If VITE_RECETTE is set, override the proxy target to point at a remote
// server instead of localhost:8090, forwarding the recette session/XSRF
// cookies so authenticated requests work against that environment.
const applyRemoteTarget = (env, PROXY_CONFIG, PROXY_FAVICO) => {
  const { VITE_RECETTE, VITE_XSRF_TOKEN, VITE_ONE_SESSION_ID } = env;
  if (!VITE_RECETTE) {
    return;
  }

  console.log("Using remote proxy configuration target: ", VITE_RECETTE);
  PROXY_CONFIG.target = VITE_RECETTE;
  PROXY_FAVICO.target = VITE_RECETTE;

  if (VITE_ONE_SESSION_ID && VITE_XSRF_TOKEN) {
    PROXY_CONFIG.headers = {
      cookie: `oneSessionId=${VITE_ONE_SESSION_ID}; authenticated=true; XSRF-TOKEN=${VITE_XSRF_TOKEN}`,
    };
    PROXY_CONFIG.onProxyRes = (proxyRes, req, res) => {
      proxyRes.headers["set-cookie"] = [
        `oneSessionId=${VITE_ONE_SESSION_ID}`,
        `XSRF-TOKEN=${VITE_XSRF_TOKEN}`,
      ];
    };
  }
};

module.exports = { applyRemoteTarget };

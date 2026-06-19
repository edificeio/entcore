const { on } = require("events");
const fs = require("fs");

const PROXY_CONFIG = {
  context: [
    "/admin/api",
    "/appregistry",
    "/auth",
    "/communication",
    "/directory",
    "/timeline",
    "/workspace",
    "/cas",
    "/admin/conf/public",
    "/admin/public/dist/assets/trumbowyg",
    "/admin/i18n",
    "/i18n",
    "/languages",
    "/zendeskGuide",
    "/theme",
  ],
  target: "http://localhost:8090",
  secure: false,
  logLevel: "debug",
  changeOrigin: true,
};

const PROXY_FAVICO = {
  context: "/assets/themes/**/*.ico",
  target: "http://localhost:8090",
  secure: false,
  logLevel: "debug",
  changeOrigin: true,
};

// This function parses a .env file and returns an object with key-value pairs.
// This is used to uniformize the parsing of the .env file and to avoid using a third-party library.
const parseEnvFile = (content) => {
  const result = {};
  for (const line of content.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const eqIndex = trimmed.indexOf("=");
    if (eqIndex === -1) continue;
    const key = trimmed.slice(0, eqIndex).trim();
    let value = trimmed.slice(eqIndex + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }
    result[key] = value;
  }
  return result;
};

if (fs.existsSync("./.env")) {
  const env = parseEnvFile(fs.readFileSync("./.env", "utf-8"));
  const target = env.VITE_RECETTE;
  const xsrfToken = env.VITE_XSRF_TOKEN;
  const oneSessionId = env.VITE_ONE_SESSION_ID;
  if (target) {
    console.log("Using remote proxy configuration target: ", target);
    PROXY_CONFIG.target = target;
    PROXY_FAVICO.target = target;
    if (oneSessionId && xsrfToken) {
      PROXY_CONFIG.headers = {
        cookie: `oneSessionId=${oneSessionId}; authenticated=true; XSRF-TOKEN=${xsrfToken}`,
      };
      PROXY_CONFIG.onProxyRes = (proxyRes, req, res) => {
        proxyRes.headers["set-cookie"] = [
          `oneSessionId=${oneSessionId}`,
          `XSRF-TOKEN=${xsrfToken}`,
        ];
      };
    }
  }
}

module.exports = [PROXY_CONFIG, PROXY_FAVICO];

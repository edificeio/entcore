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

if (fs.existsSync("./.proxyRemoteConfig.js")) {
  const config = require("./.proxyRemoteConfig.js");
  console.log("Using remote proxy configuration target: ", config.target);
  PROXY_CONFIG.target = config.target;
  PROXY_FAVICO.target = config.target;
  if (config.oneSessionId && config.xsrfToken) {
    PROXY_CONFIG.headers = {
      cookie: `oneSessionId=${config.oneSessionId}; authenticated=true; XSRF-TOKEN=${config.xsrfToken}`,
    };
    PROXY_CONFIG.onProxyRes = (proxyRes, req, res) => {
      proxyRes.headers["set-cookie"] = [
        `oneSessionId=${config.oneSessionId}`,
        `XSRF-TOKEN=${config.xsrfToken}`,
      ];
    };
  }

}

module.exports = [PROXY_CONFIG, PROXY_FAVICO];

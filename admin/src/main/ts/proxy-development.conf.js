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
  ],
  target: "http://localhost:8090",
  secure: false,
  logLevel: "debug",
  changeOrigin: true,
};

if (fs.existsSync("./.proxyRemoteConfig.js")) {
  console.log("Using remote proxy configuration");
  const config = require("./.proxyRemoteConfig.js");
  PROXY_CONFIG.target = config.target;
  if (config.oneSessionId && config.xsrfToken) {
    PROXY_CONFIG.headers = {
      cookie: `oneSessionId=${config.oneSessionId}; authenticated=true; XSRF-TOKEN=${config.xsrfToken}`,
    };
  }
}

module.exports = [PROXY_CONFIG];

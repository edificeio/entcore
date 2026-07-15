const fs = require("node:fs");
const { applyRemoteTarget } = require("./proxy-remote-target");
const { createLocalI18nFrMock } = require("./proxy-mocks/local-i18n-fr.mock");
const { createScreebAppIdMock } = require("./proxy-mocks/screeb-app-id.mock");

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

// Combines several proxy bypass handlers: tries each in order, the first to
// return a truthy result wins, otherwise the request proxies normally.
const composeBypass = (handlers) => (req, res) => {
  for (const handler of handlers) {
    const result = handler(req, res);
    if (result) {
      return result;
    }
  }
  return null;
};

// Each mock below contributes at most one bypass handler.
const bypassHandlers = [createLocalI18nFrMock()].filter(Boolean);

if (fs.existsSync("./.env")) {
  const env = parseEnvFile(fs.readFileSync("./.env", "utf-8"));

  applyRemoteTarget(env, PROXY_CONFIG, PROXY_FAVICO);

  const screebAppIdMock = createScreebAppIdMock(env);
  if (screebAppIdMock) {
    bypassHandlers.push(screebAppIdMock);
  }
}

if (bypassHandlers.length > 0) {
  PROXY_CONFIG.bypass = composeBypass(bypassHandlers);
}

module.exports = [PROXY_CONFIG, PROXY_FAVICO];

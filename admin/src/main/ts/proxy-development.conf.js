const { on } = require("events");
const fs = require("fs");

const { applyRemoteTarget } = require("./proxy-mocks/proxy-remote-target");
const { PROXY_ADMIN_I18N, registerLocalI18nFrMock } = require("./proxy-mocks/local-i18n-fr.mock");
const { PROXY_ADMIN_CONF_PUBLIC, registerScreebAppIdMock } = require("./proxy-mocks/screeb-app-id.mock");
const { startMockServer } = require("./proxy-mocks/mock-server");

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
    "/admin/public/dist/assets/trumbowyg",
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

// Each mock module owns its proxy entry (instead of sharing PROXY_CONFIG's
// context), so that only the entry whose mock is actually enabled gets its
// `target` redirected to the local mock server (see
// proxy-mocks/mock-server.js) — the bulk of routes (e.g. /directory) stay
// untouched.
const ALL_PROXIES = [PROXY_CONFIG, PROXY_FAVICO, PROXY_ADMIN_CONF_PUBLIC, PROXY_ADMIN_I18N];

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
  applyRemoteTarget(env, ALL_PROXIES);
  registerScreebAppIdMock(env.SCREEB_APP_ID_DEV);
}

registerLocalI18nFrMock();
startMockServer();

module.exports = ALL_PROXIES;

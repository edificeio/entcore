const fs = require("node:fs");
const path = require("node:path");
const { registerMockRoute, MOCK_SERVER_TARGET } = require("./mock-server");

const LOCAL_FR_I18N_PATH = path.resolve(__dirname, "../../resources/i18n/fr.json");

const PROXY_ADMIN_I18N = {
  context: "/admin/i18n",
  target: "http://localhost:8090",
  secure: false,
  logLevel: "debug",
  changeOrigin: true,
};

// Dev-only mock: serve the local i18n/fr.json file directly instead of
// proxying to the backend, so translation edits are reflected immediately
// without rebuilding/redeploying the backend.
const registerLocalI18nFrMock = () => {
  if (!fs.existsSync(LOCAL_FR_I18N_PATH)) {
    return;
  }
  console.log("Mocking /admin/i18n with local i18n/fr.json");
  registerMockRoute("/admin/i18n", (req, res) => {
    res.setHeader("Content-Type", "application/json; charset=utf-8");
    try {
      res.end(fs.readFileSync(LOCAL_FR_I18N_PATH, "utf-8"));
    } catch (e) {
      console.error("Failed to read local i18n file:", e);
      res.statusCode = 500;
      res.end(JSON.stringify({ error: "Failed to read local i18n file" }));
    }
  });
  PROXY_ADMIN_I18N.target = MOCK_SERVER_TARGET;
};

module.exports = { PROXY_ADMIN_I18N, registerLocalI18nFrMock };

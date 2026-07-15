const fs = require("node:fs");
const path = require("node:path");

const LOCAL_FR_I18N_PATH = path.resolve(__dirname, "../../resources/i18n/fr.json");

// Dev-only mock: serve the local i18n/fr.json file directly instead of
// proxying to the backend, so translation edits are reflected immediately
// without rebuilding/redeploying the backend.
const createLocalI18nFrMock = () => {
  if (!fs.existsSync(LOCAL_FR_I18N_PATH)) {
    return null;
  }
  console.log("Mocking /admin/i18n with local i18n/fr.json");
  return (req, res) => {
    if (req.url?.split("?")[0] === "/admin/i18n") {
      res.setHeader("Content-Type", "application/json; charset=utf-8");
      try {
        res.end(fs.readFileSync(LOCAL_FR_I18N_PATH, "utf-8"));
      } catch (e) {
        console.error("Failed to read local i18n file:", e);
        res.statusCode = 500;
        res.end(JSON.stringify({ error: "Failed to read local i18n file" }));
      }
      return true; // response already sent, skip proxying
    }
    return null; // proxy normally
  };
};

module.exports = { createLocalI18nFrMock };

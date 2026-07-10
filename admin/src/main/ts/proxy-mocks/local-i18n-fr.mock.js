const fs = require("fs");
const path = require("path");

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
      res.setHeader("Content-Type", "application/json");
      res.end(fs.readFileSync(LOCAL_FR_I18N_PATH, "utf-8"));
      return true; // response already sent, skip proxying
    }
    return null; // proxy normally
  };
};

module.exports = { createLocalI18nFrMock };

const { registerMockRoute, MOCK_SERVER_TARGET } = require("./mock-server");

const PROXY_ADMIN_CONF_PUBLIC = {
  context: "/admin/conf/public",
  target: "http://localhost:8090",
  secure: false,
  logLevel: "debug",
  changeOrigin: true,
};

// Dev-only mock: the platform won't serve the Screeb key on /admin/conf/public
// until the backend template.j2 change is deployed. Set SCREEB_APP_ID_DEV in
// .env to test the Screeb integration locally.
const registerScreebAppIdMock = (screebAppIdDev) => {
  if (!screebAppIdDev) {
    return;
  }
  console.log("Mocking /admin/conf/public with SCREEB_APP_ID_DEV");
  registerMockRoute("/admin/conf/public", (req, res) => {
    res.setHeader("Content-Type", "application/json");
    res.end(JSON.stringify({ "screeb-app-id": screebAppIdDev }));
  });
  PROXY_ADMIN_CONF_PUBLIC.target = MOCK_SERVER_TARGET;
};

module.exports = { PROXY_ADMIN_CONF_PUBLIC, registerScreebAppIdMock };

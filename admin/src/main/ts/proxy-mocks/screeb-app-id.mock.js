// Dev-only mock: the platform won't serve the Screeb key on /admin/conf/public
// until the backend template.j2 change is deployed. Set SCREEB_APP_ID_DEV in
// .env to test the Screeb integration locally.
const createScreebAppIdMock = (env) => {
  const { SCREEB_APP_ID_DEV } = env;
  if (!SCREEB_APP_ID_DEV) {
    return null;
  }
  console.log("Mocking /admin/conf/public with SCREEB_APP_ID_DEV");
  return (req, res) => {
    if (req.url?.split("?")[0] === "/admin/conf/public") {
      res.setHeader("Content-Type", "application/json");
      res.end(JSON.stringify({ "screeb-app-id": SCREEB_APP_ID_DEV }));
      return true; // response already sent, skip proxying
    }
    return null; // proxy normally
  };
};

module.exports = { createScreebAppIdMock };

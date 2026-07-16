const http = require("node:http");

// Dev-only mock routing: path -> (req, res) => void, served by a tiny local
// HTTP server instead of webpack-dev-server's proxy `bypass` option.
//
// `bypass` looked like the natural tool for per-route mocks, but
// webpack-dev-server has a bug (lib/Server.js, the per-proxy-entry
// `handler`): whenever a proxy entry's `bypass` returns a falsy-but-not-null
// value, that entry's own `proxyMiddleware` still gets invoked on the
// current request even if the request's URL doesn't match that entry's
// `context`. So merely having a `bypass` defined anywhere in the proxy
// array causes stray createProxyMiddleware invocations on *every* request,
// which corrupts body streaming for unrelated PUT/POST/PATCH requests (e.g.
// to /directory). Pointing a proxy's `target` at this local server instead
// avoids that code path entirely — it's a plain proxy pass-through, same as
// any other target.

// Fixed rather than ephemeral so mocks can build their target URL
// synchronously, before ng serve finishes reading the proxy config.
const MOCK_SERVER_PORT = 4299;
const MOCK_SERVER_TARGET = `http://localhost:${MOCK_SERVER_PORT}`;

const routes = new Map();

const registerMockRoute = (urlPath, handler) => {
  routes.set(urlPath, handler);
};

const startMockServer = () => {
  if (routes.size === 0) {
    return;
  }
  const server = http.createServer((req, res) => {
    const handler = routes.get(req.url?.split("?")[0]);
    if (handler) {
      return handler(req, res);
    }
    res.statusCode = 404;
    res.end();
  });

  server.on("error", (err) => {
    if (err && err.code === "EADDRINUSE") {
      console.warn(
        `Dev mock server port ${MOCK_SERVER_PORT} already in use; skipping start.`,
      );
      return;
    }
    throw err;
  });

  server.listen(MOCK_SERVER_PORT);
  console.log(
    `Dev mock server listening on ${MOCK_SERVER_TARGET} for: ${[...routes.keys()].join(", ")}`,
  );

module.exports = { registerMockRoute, startMockServer, MOCK_SERVER_TARGET };

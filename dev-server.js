// Local dev server for the AngularJS legacy modules built by gulpfile.js
// (archive, directory, workspace, and the legacy part of auth / timeline).
// React fronts (`<module>/frontend`) and admin have their own dev servers.
//
// Serves the target module's public/ folder from disk and proxies everything
// else (server views, ng-app.js, theme, API, /assets/version...) to a remote
// recette, with the session from .env. Browser auto-reloads on rebuild.
//
// Usage: npm run dev:directory (see package.json), or
//   MODULE=directory START_PATH=/userbook/mon-compte npm run legacy:dev
const { exec } = require("child_process");
const fs = require("fs");
const path = require("path");
const httpProxy = require("http-proxy");
const browserSync = require("browser-sync").create();

const MODULES = ["archive", "auth", "directory", "timeline", "workspace"];
const target = process.env.MODULE || "directory";
if (MODULES.indexOf(target) === -1) {
  console.error(`MODULE inconnu : "${target}". Valeurs possibles : ${MODULES.join(", ")}`);
  process.exit(1);
}

const APP_ADDRESS = `/${target}`;
const START_PATH = process.env.START_PATH || APP_ADDRESS;
const PORT = Number(process.env.PORT) || 3000;
const moduleResources = path.resolve(__dirname, target, "src/main/resources");
const publicDir = path.join(moduleResources, "public");
// Local i18n keys are merged over the recette's ones, so new keys show up
// without a deployment.
const localI18nPath = path.join(moduleResources, "i18n/fr.json");

// Minimal .env parser (no dotenv dependency), same pattern as admin.
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

const envPath = path.resolve(__dirname, ".env");
if (!fs.existsSync(envPath)) {
  console.error(
    "Fichier .env manquant. Copiez .env.template en .env puis lancez `dev-auth-fetcher connect` (ou le skill auth-user-frontend)."
  );
  process.exit(1);
}
const { VITE_RECETTE, VITE_XSRF_TOKEN, VITE_ONE_SESSION_ID } = parseEnvFile(
  fs.readFileSync(envPath, "utf-8")
);
if (!VITE_RECETTE) {
  console.error("VITE_RECETTE manquant dans .env");
  process.exit(1);
}

// dev-auth-fetcher sometimes writes a trailing slash, which would produce
// `//directory` upstream requests (404).
const recette = VITE_RECETTE.replace(/\/+$/, "");
const cookie = `oneSessionId=${VITE_ONE_SESSION_ID}; authenticated=true; XSRF-TOKEN=${VITE_XSRF_TOKEN}`;

const proxy = httpProxy.createProxyServer({
  target: recette,
  changeOrigin: true,
  secure: true,
  // HTML documents and i18n are rewritten by hand, so http-proxy must not
  // touch responses. Side effect: autoRewrite/protocolRewrite would be inert,
  // redirects are rewritten in rewriteRedirectLocation instead.
  selfHandleResponse: true,
});

// Only navigation documents get the browser-sync client: Angular partial
// templates are text/html too and must stay untouched.
const wantsHtmlDocument = (req) => (req.headers.accept || "").indexOf("text/html") !== -1;
const isI18nRequest = (req) => req.method === "GET" && req.url.split("?")[0] === `${APP_ADDRESS}/i18n`;

proxy.on("proxyReq", (proxyReq, req) => {
  proxyReq.setHeader("cookie", cookie);
  proxyReq.setHeader("X-XSRF-TOKEN", VITE_XSRF_TOKEN || "");
  // Uncompressed body for the responses we rewrite
  if (wantsHtmlDocument(req) || isI18nRequest(req)) proxyReq.setHeader("accept-encoding", "identity");
});

// Local server is http, recette is https: without this, a redirect (typically
// /auth/login on expired session) sends the browser to https://localhost.
// Only redirects to the recette itself are rewritten, never an external SSO.
const REDIRECT_STATUS = /^201|30(1|2|7|8)$/;
function rewriteRedirectLocation(proxyRes, req) {
  const rawLocation = proxyRes.headers["location"];
  if (!REDIRECT_STATUS.test(proxyRes.statusCode) || !rawLocation) return;
  const redirect = new URL(rawLocation, recette);
  if (redirect.host !== new URL(recette).host) return;
  redirect.protocol = "http:";
  redirect.host = req.headers["host"];
  proxyRes.headers["location"] = redirect.toString();
}

function injectInHtml(body) {
  // Without `server` or `proxy` options browser-sync runs in snippet mode and
  // injects its client nowhere: without this, `files` would have no client
  // to send reload orders to.
  const snippet = browserSync.getOption("snippet") || "";
  return body.indexOf("</head>") !== -1 ? body.replace("</head>", snippet + "</head>") : body + snippet;
}

function mergeLocalI18n(body) {
  if (!fs.existsSync(localI18nPath)) return body;
  try {
    const local = JSON.parse(fs.readFileSync(localI18nPath, "utf-8"));
    return JSON.stringify(Object.assign(JSON.parse(body), local));
  } catch (err) {
    console.error("i18n local ignoré :", err.message);
    return body;
  }
}

let sessionExpiredLogged = false;
proxy.on("proxyRes", (proxyRes, req, res) => {
  rewriteRedirectLocation(proxyRes, req);

  const location = proxyRes.headers["location"];
  if (!sessionExpiredLogged && location && location.indexOf("/auth/login") !== -1) {
    sessionExpiredLogged = true;
    console.error(
      "\nSession recette expirée (redirection vers /auth/login).\n" +
        "Régénérer le .env avec `dev-auth-fetcher connect`, puis relancer.\n"
    );
  }
  // Pin the session cookies: prevents session rotation by the recette
  proxyRes.headers["set-cookie"] = [
    `oneSessionId=${VITE_ONE_SESSION_ID}`,
    `XSRF-TOKEN=${VITE_XSRF_TOKEN}`,
    "authenticated=true",
  ];

  const contentType = proxyRes.headers["content-type"] || "";
  const uncompressedOk = proxyRes.statusCode === 200 && !proxyRes.headers["content-encoding"];
  let rewrite = null;
  if (wantsHtmlDocument(req) && contentType.indexOf("text/html") !== -1 && !proxyRes.headers["content-encoding"]) {
    rewrite = injectInHtml;
  } else if (isI18nRequest(req) && uncompressedOk) {
    rewrite = mergeLocalI18n;
  }

  if (!rewrite) {
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res);
    return;
  }

  const chunks = [];
  proxyRes.on("data", (chunk) => chunks.push(chunk));
  proxyRes.on("end", () => {
    const body = rewrite(Buffer.concat(chunks).toString("utf8"));
    const headers = Object.assign({}, proxyRes.headers);
    // Sent in one block: keeping transfer-encoding with a new content-length
    // makes an ambiguous response (RFC 7230 3.3.3).
    delete headers["transfer-encoding"];
    delete headers["etag"];
    headers["content-length"] = Buffer.byteLength(body);
    res.writeHead(proxyRes.statusCode, headers);
    res.end(body);
  });
});

proxy.on("error", (err, req, res) => {
  console.error("Erreur proxy vers la recette :", err.message);
  if (!res.headersSent) res.writeHead(502, { "Content-Type": "text/plain" });
  res.end("Erreur de proxy vers la recette : " + err.message);
});

const relativePublic = path.relative(__dirname, publicDir);

browserSync.init(
  {
    port: PORT,
    // The proxy injects a real session: listen on the local machine only.
    listen: "localhost",
    // In snippet mode `open: true` is silently ignored: opened by hand below.
    open: false,
    ui: false,
    ghostMode: false,
    notify: false,
    // Serves dist/, template/, js/... of the target module only; a local 404
    // falls through to the proxy. serve-static options must sit on the entry
    // itself: the global `serveStaticOptions` is ignored for object entries.
    serveStatic: [
      {
        route: `${APP_ADDRESS}/public`,
        dir: publicDir,
        options: {
          cacheControl: false,
          setHeaders: (res) => res.setHeader("Cache-Control", "no-store"),
        },
      },
    ],
    middleware: [(req, res) => proxy.web(req, res)],
    files: [
      `${relativePublic}/dist/*.js`,
      `${relativePublic}/js/behaviours.js`,
      `${relativePublic}/template/**/*.html`,
      path.relative(__dirname, localI18nPath),
    ],
    watchEvents: ["change", "add"],
    // The hoisted fsevents is 1.x (legacy webpack 1 toolchain), incompatible
    // with browser-sync's chokidar 3: fall back to fs.watch.
    watchOptions: { useFsEvents: false },
    reloadDebounce: 500,
  },
  () => {
    const appUrl = `http://localhost:${browserSync.getOption("port")}${START_PATH}`;
    console.log("\n  Recette    : " + recette);
    console.log("  Module     : " + target);
    console.log("  Application: " + appUrl + "\n");
    const opener =
      process.platform === "darwin" ? "open" : process.platform === "win32" ? 'start ""' : "xdg-open";
    exec(`${opener} "${appUrl}"`, (err) => {
      if (err) console.log("Ouvrir manuellement : " + appUrl);
    });
  }
);

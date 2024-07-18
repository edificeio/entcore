const PROXY_CONFIG = [
  {
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
  },
];

module.exports = PROXY_CONFIG;

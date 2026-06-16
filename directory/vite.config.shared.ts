import path = require("path");
import type { UserConfig } from "vite";

export default {
  root: path.resolve(__dirname, "./src/main/resources/public/"),
  build: {
    outDir: ".",
    sourcemap: true,
    minify: false,
    emptyOutDir: false,
    rollupOptions: {
      output: {
        entryFileNames: "dist/[name].js",
        format: "umd",
        globals: {
          entcore: "entcore",
          "entcore/entcore": "entcore",
          moment: "entcore",
          underscore: "entcore",
          jquery: "entcore",
          angular: "angular",
        },
      },
      external: [
        "entcore/entcore",
        "entcore",
        "moment",
        "underscore",
        "jquery",
        "angular",
      ],
    },
  },
  resolve: {
    extensions: [".ts", ".js"],
  },
  optimizeDeps: {
    exclude: ["entcore", "entcore-toolkit"],
  },
} satisfies UserConfig;

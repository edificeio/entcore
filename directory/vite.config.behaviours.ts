import path = require("path");
import { mergeConfig, type UserConfig } from "vite";
import sharedConfig from "./vite.config.shared";

export default mergeConfig(sharedConfig, {
  build: {
    rollupOptions: {
      input: {
        behaviours: path.resolve(
          __dirname,
          "./src/main/resources/public/ts/behaviours.ts"
        ),
      },
      output: {
        entryFileNames: "js/[name].js",
        format: "umd",
        globals: {
          entcore: "entcore",
        },
      },
    },
  },
} satisfies UserConfig);

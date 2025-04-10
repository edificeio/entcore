# Edifice Rich Text Editor Extensions

![npm](https://img.shields.io/npm/v/@edifice.io/tiptap-extensions?style=flat-square)
![bundlephobia](https://img.shields.io/bundlephobia/min/@edifice.io/tiptap-extensions?style=flat-square)

Extensions based on [Tiptap Editor](https://tiptap.dev/). Extends functionalities of the editor.

## Prerequisites

- `pnpm: >= 9`
- `node: >= 20`

## Getting Started

### Install

```bash
pnpm add @edifice.io/tiptap-extensions
```

### Imports

#### Global

```bash
import { Alert, Video } from "@edifice.io/tiptap-extensions"
```

#### Sub-imports

```bash
import { Alert } from "@edifice.io/tiptap-extensions/alert"
```

## New extension

To create a new extension, please do as follow :

- Create a subfolder in `src` with the name of the extension (e.g: `my-extension`)

```
my-extension
```

- Create two files inside the new folder:
  - `index.ts`
  - `my-extension.ts`

```
my-extension
└── my-extension.ts
└── index.ts
```

- Check one existing extension or refer to the official [documentation](https://tiptap.dev/docs/editor/extensions/custom-extensions) to develop an extension.
- Then, add your extension in the `package.json` sub-exports in alphabetical order.

```
"./my-extension": {
  "import": "./dist/my-extension/my-extension.js",
  "require": "./dist/my-extension/my-extension.cjs"
}
```

- Run `pnpm run build` to generate a bundle
- Test your extension before committing and pushing to remote

# Open Digital Education Explorer

Explorer App

## Getting Started

### Install

Without Docker, you need to generate a `package.json`

```bash
node scripts/package.cjs
```

Install all dependencies.

```bash
pnpm install
```

## Dev

```bash
pnpm dev
```

### [Server Options](https://vitejs.dev/config/server-options.html)

You can configure Vite Proxy with backend routes needed for development inside `vite.config.ts`

```bash
const proxy = {
  "/example": proxyObj,
}
```

### Absolute Imports

You should use absolute imports in your app

```bash
Replace ../components/* by ~/components/*
```

Configure your paths `tsconfig.json`:

> Telling TypeScript how to resolve import path:

```bash
"paths": {
  "~/*": ["./src/*"],
  "~app/*": ["./src/app/*"]
}
```

### Lint

Detect ESlint issues

```bash
pnpm lint
```

### Prettier

Format code

```bash
pnpm format
```

### Lighthouse

> LHCI will check if your app respect at least 90% of these categories: performance, a11y, Best practices and seo

```bash
pnpm lighthouse
```

### Pre-commit

When committing your work, `pre-commit` will start `pnpm lint-staged`:

> lint-staged starts lint + prettier

```bash
pnpm pre-commit
```

## Build

TypeScript check + Vite Build

```bash
pnpm build
```

## Preview

```bash
pnpm preview
```

## License

This project is licensed under the AGPL-3.0 license.

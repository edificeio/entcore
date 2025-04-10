# Edifice React Components

![npm](https://img.shields.io/npm/v/@edifice-ui/react?style=flat-square)
![bundlephobia](https://img.shields.io/bundlephobia/min/@edifice-ui/react?style=flat-square)

## Getting Started

We follow [WAI ARIA](https://www.w3.org/WAI/ARIA/apg/patterns/) rules and [Bootstrap 5](https://getbootstrap.com/docs/5.0/components/accordion/) guidelines when making our components

### Build

```bash
pnpm run build
```

### Lint

```bash
pnpm run lint
```

If `pnpm run lint` shows issues, run this command to fix them.

```bash
pnpm run fix
```

### Prettier

```bash
pnpm run format
```

## Structure

### Component Folder

- Folder name always in PascalCase: `Button`
- Component file in PascalCase: `Button.tsx`
- Export types & interfaces inside Component file
- Stories file in PascalCase + `*.stories.tsx` : `Button.stories.tsx`

```bash
src
  -- ComponentFolder
    -- Component.tsx
    -- Component.stories.tsx
    -- index.ts
```

- Re-export the Component inside his own `index` file: `index.ts`
- Export everything if Component has types & interfaces

```jsx
export { default as Component } from "./Component";
export * from "./Component";
```

### Component Guideline

- Always document basic guideline of Component with JSDoc format. Used by Storybook to generate documentation.

```jsx
/**
 * Primary UI component for user interaction
 */
```

### Interface description

- Always document typescript types and interface with JSDoc syntax. Used by Storybook to generate documentation.

```jsx
// Interface description (e.g: TreeViewProps.tsx)
export interface ButtonProps {
  /**
   * Is this the principal call to action on the page?
   */
  primary?: boolean;
  /**
   * What background color to use
   */
  backgroundColor?: string;
  /**
   * How large should the button be?
   */
  size?: "small" | "medium" | "large";
  /**
   * Button contents
   */
  label: string;
  /**
   * Optional click handler
   */
  onClick?: () => void;
}
```

### Index file inside `src` folder

- Entry point of this React Library.
- Import your component inside `index.ts` file.

```jsx
export * from "./Button";
```

## Dev

You can build your component using `Storybook`. See [README](../../docs//README.md)

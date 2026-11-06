import React, { StrictMode } from 'react';

import { createRoot } from 'react-dom/client';

import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import { RouterProvider } from 'react-router-dom';
import theme from './theme';

import './i18n';
import { Providers, queryClient } from './providers';
import { router } from './routes';

const rootElement = document.getElementById('root');
const root = createRoot(rootElement!);

if (import.meta.env.DEV) {
  import('@axe-core/react').then((axe) => {
    axe.default(React, root, 1000);
  });
}

/** Mock not-yet-available APIs with `pnpm dev:mock`, see src/mocks/browser.ts */
async function enableMocking() {
  if (import.meta.env.MODE !== 'mock') return;
  const { worker } = await import('./mocks/browser');
  await worker.start({ onUnhandledRequest: 'bypass' });
}

enableMocking().then(() =>
  root.render(
    <StrictMode>
      <Providers>
        <ThemeProvider theme={theme}>
          <CssBaseline />
          <RouterProvider router={router(queryClient)} />
        </ThemeProvider>
      </Providers>
    </StrictMode>,
  ),
);

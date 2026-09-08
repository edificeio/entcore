import React, { StrictMode } from 'react';

import { EdificeThemeProvider } from '@edifice.io/react';
import { createRoot } from 'react-dom/client';

import { RouterProvider } from 'react-router-dom';
import { Providers, queryClient } from './providers';
import { router } from './routes';

import '@edifice.io/bootstrap/dist/index.css';
import './index.css';

const rootElement = document.getElementById('root');
const root = createRoot(rootElement!);

async function deferRender() {
  if (import.meta.env.PROD) {
    await import('./i18n');
    return;
  }

  if (import.meta.env.VITE_MOCK === 'true') {
    const { worker } = await import('./mocks/browser');
    await worker.start({ onUnhandledRequest: 'warn' });
  }

  await import('./i18n');
  const axe = await import('@axe-core/react');
  return axe.default(React, root, 1000);
}

deferRender().then(() => {
  root.render(
    <StrictMode>
      <Providers>
        <EdificeThemeProvider>
          <RouterProvider router={router(queryClient)} />
        </EdificeThemeProvider>
      </Providers>
    </StrictMode>,
  );
});

import React, { StrictMode } from 'react';

import { EdificeThemeProvider } from '@edifice.io/react';
import { createRoot } from 'react-dom/client';

import { RouterProvider } from 'react-router-dom';
import './i18n';
import { Providers, queryClient } from './providers';
import { router } from './routes';

import '@edifice.io/bootstrap/dist/index.css';

const rootElement = document.getElementById('root');
const root = createRoot(rootElement!);

if (process.env.NODE_ENV !== 'production') {
  import('@axe-core/react').then((axe) => {
    axe.default(React, root, 1000);
  });
}

root.render(
  <StrictMode>
    <Providers>
      <EdificeThemeProvider>
        <RouterProvider router={router(queryClient)} />
      </EdificeThemeProvider>
    </Providers>
  </StrictMode>,
);

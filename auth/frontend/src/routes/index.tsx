import { QueryClient } from '@tanstack/react-query';
import { RouteObject, createBrowserRouter } from 'react-router-dom';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';

// Mark `queryClient` as used to satisfy TypeScript's unused-parameter check
const routes = (queryClient: QueryClient): RouteObject[] => {
  void queryClient;
  return [
    /* Main route */
    {
      path: '/saml/wayf',
      async lazy() {
        const { WayfPage: Component } = await import('~/routes/pages/Wayf');
        return {
          Component,
        };
      },
      errorElement: <PageError />,
    },
    /* 404 Page */
    {
      path: '*',
      element: <NotFound />,
    },
  ];
};

export const basename = import.meta.env.PROD ? '/auth' : '';

export const router = (queryClient: QueryClient) => {
  return createBrowserRouter(routes(queryClient), {
    basename,
  });
};

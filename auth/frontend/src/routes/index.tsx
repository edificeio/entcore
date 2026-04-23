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
        const { loader, Root: Component } = await import('~/routes/root');
        return {
          loader,
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

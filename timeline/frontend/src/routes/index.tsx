import { QueryClient } from '@tanstack/react-query';
import { RouteObject, createBrowserRouter } from 'react-router-dom';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';
import { manageRedirections } from './redirections';

const routes = (_queryClient: QueryClient): RouteObject[] => [
  /* Main route */
  {
    path: '/',
    async lazy() {
      const { loader, Root: Component } = await import('~/routes/root');
      return {
        loader,
        Component,
      };
    },
    children: [
      /* 404 Page */
      {
        path: '*',
        element: <NotFound />,
      },
    ],
  },
];

export const basename = import.meta.env.PROD ? '/timeline/timeline' : '/';

export const router = (queryClient: QueryClient) => {
  const redirectPath = manageRedirections();

  if (redirectPath !== null) {
    // If the redirect path is the root, we need to remove the trailing slash to match with /timeline/timeline
    const normalizedRedirectPath = redirectPath === '/' ? '' : redirectPath;
    const newUrl =
      window.location.origin +
      basename.replace(/\/$/g, '') +
      normalizedRedirectPath;
    window.history.replaceState(null, '', newUrl);
  }
  return createBrowserRouter(routes(queryClient), {
    basename,
  });
};

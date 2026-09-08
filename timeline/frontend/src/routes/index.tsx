import { QueryClient } from '@tanstack/react-query';
import { RouteObject, createBrowserRouter } from 'react-router-dom';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';
import { manageRedirections } from './redirections';

const routes = (queryClient: QueryClient): RouteObject[] => {
  void queryClient; // Mark `queryClient` as used to satisfy TypeScript's unused-parameter check
  return [
    /* Customization page */
    {
      path: '/customize',
      lazy: async () => await import('~/routes/pages/customize'),
      errorElement: <PageError />,
    },
    /* Main route */
    {
      path: import.meta.env.PROD ? '/timeline' : '/',
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
export const basename = import.meta.env.PROD ? '/timeline' : '/';

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

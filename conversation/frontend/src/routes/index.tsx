import { QueryClient } from '@tanstack/react-query';
import { Outlet, RouteObject, createBrowserRouter } from 'react-router-dom';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';

const routes = (_queryClient: QueryClient): RouteObject[] => [
  // Manages redirections whatever the final routing.
  {
    async lazy() {
      const { loader } = await import('~/routes/root');
      return {
        loader,
        element: <Outlet></Outlet>,
      };
    },
    children: [
      /* Main route */
      // Manages user's access rights and redirections.
      {
        path: '/',
        async lazy() {
          const { loader, Root: Component } = await import('~/routes/root');
          return {
            loader,
            Component,
          };
        },
        errorElement: <PageError />,
        children: [
          /* Layout = route without a path */
          // Manages folders tree and occupied space.
          {
            async lazy() {
              const { Component, loader } = await import('~/routes/layout');
              return {
                loader: loader(_queryClient),
                Component,
              };
            },
            /* Pages */
            children: [
              // Display messages from selected folder.
              {
                path: 'id/:folderId',
                async lazy() {
                  const { Component, loader } = await import(
                    '~/routes/pages/folder-display'
                  );
                  return {
                    loader: loader(_queryClient),
                    Component,
                  };
                },
              },
              // Displays selected message.
              {
                path: 'id/:folderId/:messageId',
                async lazy() {
                  const { Component, loader } = await import(
                    '~/routes/pages/message-display'
                  );
                  return {
                    loader: loader(_queryClient),
                    Component,
                  };
                },
              },
              // Displays a new blank message in edit mode.
              {
                path: 'id/:folderId/create',
                async lazy() {
                  const { Component, loader } = await import(
                    '~/routes/pages/message-create'
                  );
                  return {
                    loader: loader(_queryClient),
                    Component,
                  };
                },
              },
            ],
          },
        ],
      },
      /* 404 Page */
      {
        path: '*',
        element: <NotFound />,
      },
    ],
  },
];

export const basename = import.meta.env.PROD ? '/conversation' : '/';

export const router = (queryClient: QueryClient) =>
  createBrowserRouter(routes(queryClient), {
    basename,
  });

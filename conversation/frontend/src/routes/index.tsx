import { QueryClient } from '@tanstack/react-query';
import { Outlet, RouteObject, createBrowserRouter } from 'react-router-dom';
import { manageRedirections } from '~/routes/redirections';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';

const routes = (_queryClient: QueryClient): RouteObject[] => [
  {
    // Manage redirections before resolving to the final route.
    loader: manageRedirections,
    element: <Outlet></Outlet>,
    children: [
      /* Main route */
      {
        path: '/',
        async lazy() {
          const { loader, Component } = await import('~/routes/root');
          return {
            loader: loader(_queryClient),
            Component,
          };
        },
        errorElement: <PageError />,
        /* Pages */
        children: [
          {
            // Display messages from /inbox, /outbox, /trash, /draft
            path: ':folderId',
            children: [
              {
                path: '',
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
                path: 'message/:messageId',
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
            ],
          },
          {
            // Display messages from /folder/anyUserFolderID
            path: 'folder/:folderId',
            children: [
              {
                path: '',
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
                path: 'message/:messageId',
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
            ],
          }, // Displays a new blank message in edit mode.
          {
            path: 'draft/create',
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

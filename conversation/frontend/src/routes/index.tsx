import { QueryClient } from '@tanstack/react-query';
import { Outlet, RouteObject, createBrowserRouter } from 'react-router-dom';
import { manageRedirections } from '~/routes/redirections';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';

const routes = (_queryClient: QueryClient): RouteObject[] => [
  {
    element: <Outlet />,
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
                    '~/routes/pages/Folder'
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
                    '~/routes/pages/Message'
                  );
                  return {
                    loader: loader(_queryClient),
                    Component,
                  };
                },
              },
              // Create new message
              {
                path: 'create',
                async lazy() {
                  const { Component, loader } = await import(
                    '~/routes/pages/Message'
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
                    '~/routes/pages/Folder'
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
                    '~/routes/pages/Message'
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
      {
        path: 'print/:messageId',
        async lazy() {
          const { Component, loader } = await import('~/routes/pages/Message');
          return {
            loader: loader(_queryClient, true),
            Component,
          };
        },
      },
      {
        path: 'oldformat/:messageId',
        async lazy() {
          const { Component, loader } = await import(
            '~/routes/pages/OldFormat'
          );
          return {
            loader: loader(_queryClient),
            Component,
          };
        },
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

export const router = (queryClient: QueryClient) => {
  const redirectPath = manageRedirections();

  if (redirectPath) {
    const newUrl =
      window.location.origin + basename.replace(/\/$/g, '') + redirectPath;
    window.history.replaceState(null, '', newUrl);
  }
  return createBrowserRouter(routes(queryClient), {
    basename,
  });
};

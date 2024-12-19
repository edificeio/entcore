import { QueryClient } from '@tanstack/react-query';
import { RouteObject, createBrowserRouter } from 'react-router-dom';

import { NotFound } from './errors/not-found';
import { PageError } from './errors/page-error';

const routes = (_queryClient: QueryClient): RouteObject[] => [
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
      // The loader of this page loads folders tree.
      {
        index: true,
        async lazy() {
          const { Component, loader } = await import('~/routes/folders-tree');
          return {
            loader: loader(_queryClient),
            Component,
          };
        },
      },
      // This page lists messages in a folder.
      {
        path: ':folderId',
        async lazy() {
          const { Component, loader } = await import('~/routes/folder-display');
          return {
            loader: loader(_queryClient),
            Component,
          };
        },
        children: [
          // This page displays a message.
          {
            path: ':folderId/:messageId',
            async lazy() {
              const { Component, loader } = await import(
                '~/routes/message-display'
              );
              return {
                loader: loader(_queryClient),
                Component,
              };
            },
          },
          // This page displays a new blank message in edit mode.
          {
            path: ':folderId/create',
            async lazy() {
              const { Component, loader } = await import(
                '~/routes/message-create'
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
];

export const basename = import.meta.env.PROD ? '/conversation' : '/';

export const router = (queryClient: QueryClient) =>
  createBrowserRouter(routes(queryClient), {
    basename,
  });

import { ERROR_CODE } from '@edifice.io/client';
import { EdificeClientProvider } from '@edifice.io/react';
import {
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ReactNode } from 'react';
import { EdificeScreebProvider } from './EdificeScreebProvider';

export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error) => {
      if (typeof error === 'string') {
        if (error === ERROR_CODE.NOT_LOGGED_IN)
          window.location.replace('/auth/login');
      }
    },
  }),
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
      staleTime: 1000 * 60 * 2,
    },
  },
});

export const Providers = ({ children }: { children: ReactNode }) => {
  return (
    <QueryClientProvider client={queryClient}>
      <EdificeClientProvider
        params={{
          app: 'actualites',
        }}
      >
        <EdificeScreebProvider>{children}</EdificeScreebProvider>
      </EdificeClientProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  );
};

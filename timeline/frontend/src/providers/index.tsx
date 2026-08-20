import { ERROR_CODE } from '@edifice.io/client';
import { EdificeClientProvider } from '@edifice.io/react';
import {
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from '@tanstack/react-query';
import { ReactNode, Suspense, lazy } from 'react';

const ReactQueryDevtools = import.meta.env.DEV
  ? lazy(async () => {
      const module = await import('@tanstack/react-query-devtools');
      return { default: module.ReactQueryDevtools };
    })
  : null;

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
          app: 'timeline',
        }}
      >
        {children}
      </EdificeClientProvider>
      {import.meta.env.DEV && ReactQueryDevtools ? (
        <Suspense fallback={null}>
          <ReactQueryDevtools initialIsOpen={false} />
        </Suspense>
      ) : null}
    </QueryClientProvider>
  );
};

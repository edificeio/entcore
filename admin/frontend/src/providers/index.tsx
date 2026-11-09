import { ERROR_CODE } from '@edifice.io/client';
import { EdificeClientProvider } from '@edifice.io/react';
import {
  QueryCache,
  QueryClient,
  QueryClientProvider,
} from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ReactNode } from 'react';

import { ThemeProvider } from '@mui/material/styles';

import theme from '~/theme';

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
          app: 'admin',
        }}
      >
        {/* Here rather than in main.tsx so component tests get the app theme */}
        <ThemeProvider theme={theme}>{children}</ThemeProvider>
      </EdificeClientProvider>
      {import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
    </QueryClientProvider>
  );
};

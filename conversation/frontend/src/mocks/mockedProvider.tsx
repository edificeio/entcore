import { EdificeClientProvider } from '@edifice.io/react';
import { QueryClientProvider } from '@tanstack/react-query';
import { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { queryClient } from '~/providers';

export interface MockedProvidersProps {
  initialEntries?: string[];
  children?: ReactNode;
}

export const MockedProviders = ({
  initialEntries,
  children,
}: MockedProvidersProps) => {
  return (
    <QueryClientProvider client={queryClient}>
      <EdificeClientProvider
        params={{
          app: 'conversation',
        }}
      >
        <MemoryRouter initialEntries={initialEntries || ['']}>
          {children}
        </MemoryRouter>
      </EdificeClientProvider>
    </QueryClientProvider>
  );
};

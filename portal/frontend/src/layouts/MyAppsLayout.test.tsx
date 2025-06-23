import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { describe, expect, vi, it } from 'vitest';
import { MyAppLayout } from './MyAppsLayout';
import mockData from '~/mocks/mockApplications.json';
import { EdificeClientProvider } from '@edifice.io/react'; // adapte selon ton alias

const mockApps = mockData.apps;

vi.mock('~/store/userPreferencesStore', () => ({
  useUserPreferencesStore: () => ({ bookmarks: [] }),
}));

vi.mock('~/store/categoryStore', () => ({
  useCategoryStore: () => ({ activeCategory: 'all' }),
}));

vi.mock('~/hooks/useHydrateUserPreferences', () => ({
  useHydrateUserPreferences: () => ({ isHydrated: true }),
}));

vi.mock('~/services', async () => {
  const actual = await vi.importActual('~/services');
  return {
    ...actual,
    useApplications: () => ({
      applications: mockApps,
      isLoading: false,
      isError: false,
    }),
  };
});

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string) => key,
  }),
}));

const queryClient = new QueryClient();

describe('MyAppLayout', () => {
  it('Show applications once everything is ready', () => {
    render(
      <QueryClientProvider client={queryClient}>
        <EdificeClientProvider params={{
          app: 'portal',
        }}>
            <MyAppLayout theme="1d" />
        </EdificeClientProvider>
      </QueryClientProvider>,
    );

    expect(screen.getByText('navbar.applications')).toBeInTheDocument();
    expect(screen.getByText('Présences')).toBeInTheDocument();
  });
});

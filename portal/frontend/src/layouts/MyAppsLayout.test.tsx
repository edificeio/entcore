import { render, screen, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeAll, describe, expect, vi, it } from 'vitest';
import { MyAppLayout } from './MyAppsLayout';
import mockData from '~/mocks/mockApplications.json';
import { EdificeClientProvider } from '@edifice.io/react'; // adapte selon ton alias
import { Application } from '~/models/application';
import { getAppName } from '~/utils/get-app-name';

// useApplications computes appName for every application: mock what it returns,
// not the raw API data (most mocked applications have no appName).
const mockApps = mockData.apps.map((app) => ({
  ...app,
  appName: app.appName ?? getAppName(app as Application, (key) => key),
}));

// The stores are mocked with their whole state (the components destructure
// their setters too), with stable functions so that effects do not re-run.
const stores = vi.hoisted(() => {
  const userPreferences = {
    applications: [],
    bookmarks: [],
    isHydrated: true,
    toggleBookmark: vi.fn(),
    setBookmarks: vi.fn(),
    setPreferences: vi.fn(),
  };
  const useUserPreferencesStore = Object.assign(() => userPreferences, {
    getState: () => userPreferences,
  });
  const category = { activeCategory: 'all', setActiveCategory: vi.fn() };
  return { useUserPreferencesStore, useCategoryStore: () => category };
});

vi.mock('~/store/userPreferencesStore', () => ({
  useUserPreferencesStore: stores.useUserPreferencesStore,
}));

vi.mock('~/store/categoryStore', () => ({
  useCategoryStore: stores.useCategoryStore,
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

// jsdom has neither window.matchMedia, used by the Toolbar of @edifice.io/react
// (useBreakpoint), nor IntersectionObserver, used to lazy load the application
// icons (the titles render anyway). Mocked here: src/mocks/setup.ts must not be
// modified.
beforeAll(() => {
  class IntersectionObserverMock {
    root = null;
    rootMargin = '';
    thresholds = [];
    observe = vi.fn();
    unobserve = vi.fn();
    disconnect = vi.fn();
    takeRecords = vi.fn(() => []);
  }
  Object.defineProperty(window, 'IntersectionObserver', {
    writable: true,
    value: IntersectionObserverMock,
  });
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(), // deprecated
      removeListener: vi.fn(), // deprecated
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })),
  });
});

// No retry: a failed request must not keep the test running in the background.
const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: false } },
});

describe('MyAppLayout', () => {
  it('Show applications once everything is ready', async () => {
    render(
      <QueryClientProvider client={queryClient}>
        <EdificeClientProvider
          params={{
            app: 'portal',
          }}
        >
          <MyAppLayout theme="1d" />
        </EdificeClientProvider>
      </QueryClientProvider>,
    );

    expect(screen.getByText('navbar.applications')).toBeInTheDocument();
    expect(screen.getByText('Présences')).toBeInTheDocument();

    // Let EdificeClientProvider finish its session requests (answered by the
    // MSW handlers): torn down mid-flight, they reject after the test, which
    // vitest reports as an unhandled error and fails the run.
    await waitFor(() => expect(queryClient.isFetching()).toBe(0));
  });
});

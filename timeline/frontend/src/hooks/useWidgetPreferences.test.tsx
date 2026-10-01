import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { IWidgetModel } from '@edifice.io/client';
import { useWidgetPreferences } from './useWidgetPreferences';

const widget = (overrides: Partial<IWidgetModel>): IWidgetModel => ({
  id: 'w',
  js: '',
  path: '',
  i18n: 'homepage.widget.title',
  mandatory: false,
  name: 'agenda-widget',
  ...overrides,
});

const mocks = vi.hoisted(() => ({ useUser: vi.fn() }));

vi.mock('@edifice.io/react', async () => {
  const actual =
    await vi.importActual<typeof import('@edifice.io/react')>(
      '@edifice.io/react',
    );
  return { ...actual, useUser: mocks.useUser };
});

const wrapper = ({ children }: { children: React.ReactNode }) => {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

describe('useWidgetPreferences', () => {
  beforeEach(() => {
    mocks.useUser.mockReturnValue({
      user: {
        widgets: [
          widget({ name: 'agenda-widget', mandatory: false }),
          widget({ name: 'carnet-de-bord', mandatory: true }),
        ],
      },
      avatar: '',
      userDescription: {},
    });
  });

  it('exposes the widgets deployed for the current user, from the session', () => {
    const { result } = renderHook(() => useWidgetPreferences(), { wrapper });

    expect(result.current.widgets.map((w) => w.name)).toEqual([
      'agenda-widget',
      'carnet-de-bord',
    ]);
  });

  it('defaults isVisible to true for a widget with no recorded preference', async () => {
    const { result } = renderHook(() => useWidgetPreferences(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.isVisible('agenda-widget')).toBe(true);
  });

  it('isLocked reflects the mandatory flag of the matching widget in the catalog', () => {
    const { result } = renderHook(() => useWidgetPreferences(), { wrapper });

    expect(result.current.isLocked('carnet-de-bord')).toBe(true);
    expect(result.current.isLocked('agenda-widget')).toBe(false);
    expect(result.current.isLocked('unknown-widget')).toBe(false);
  });

  it('toggleWidget flips the show flag and persists it', async () => {
    const { result } = renderHook(() => useWidgetPreferences(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.isVisible('carnet-de-bord')).toBe(true);

    result.current.toggleWidget('carnet-de-bord');

    await waitFor(() =>
      expect(result.current.isVisible('carnet-de-bord')).toBe(false),
    );
  });
});

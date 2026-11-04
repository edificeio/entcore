import { HttpResponse, http } from 'msw';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { server } from '~/mocks/server';
import { WIDGETS_ORDER_URL } from '~/services';
import widgetsDefaultOrder from '~/config/widgets.json';
import { useWidgetsOrder } from './useWidgetsOrder';

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <QueryClientProvider
    client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
  >
    {children}
  </QueryClientProvider>
);

const renderOrder = async () => {
  const hook = renderHook(() => useWidgetsOrder(), { wrapper });
  await waitFor(() => expect(hook.result.current.isLoading).toBe(false));
  return hook.result.current.order;
};

describe('useWidgetsOrder', () => {
  it('uses the order provided by the project', async () => {
    server.use(
      http.get(WIDGETS_ORDER_URL, () =>
        HttpResponse.json([{ name: 'cantine-widget', fullWidth: true }]),
      ),
    );

    expect(await renderOrder()).toEqual([
      { name: 'cantine-widget', fullWidth: true },
    ]);
  });

  it('falls back to the default order when the project provides none', async () => {
    server.use(
      http.get(
        WIDGETS_ORDER_URL,
        () => new HttpResponse(null, { status: 404 }),
      ),
    );

    expect(await renderOrder()).toEqual(widgetsDefaultOrder);
  });

  it('falls back to the default order when the file is invalid', async () => {
    server.use(http.get(WIDGETS_ORDER_URL, () => HttpResponse.json({ a: 1 })));

    expect(await renderOrder()).toEqual(widgetsDefaultOrder);
  });

  it('keeps the profiles a widget is displayed for by default', async () => {
    server.use(
      http.get(WIDGETS_ORDER_URL, () =>
        HttpResponse.json([
          { name: 'communities-widget', defaultVisibleFor: ['Teacher'] },
        ]),
      ),
    );

    expect(await renderOrder()).toEqual([
      { name: 'communities-widget', defaultVisibleFor: ['Teacher'] },
    ]);
  });

  it('falls back to the default order when the profiles are invalid', async () => {
    server.use(
      http.get(WIDGETS_ORDER_URL, () =>
        HttpResponse.json([
          { name: 'communities-widget', defaultVisibleFor: 'Teacher' },
        ]),
      ),
    );

    expect(await renderOrder()).toEqual(widgetsDefaultOrder);
  });
});

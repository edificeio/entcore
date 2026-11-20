import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { fireEvent, render, screen, waitFor } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { Component } from './Reports';

const renderPage = (url: string) => {
  const router = createMemoryRouter(
    [{ path: 'reports/:structureid', element: <Component /> }],
    { initialEntries: [url] },
  );
  return { router, ...render(<RouterProvider router={router} />) };
};

describe('Reports page', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  it('opens the processed tab from the URL', async () => {
    renderPage('/reports/structure-1?tab=settled');

    expect(await screen.findByTestId('reports-tab-settle')).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  it('falls back to the pending tab on a missing or unknown tab', async () => {
    renderPage('/reports/structure-1?tab=nope');

    expect(await screen.findByTestId('reports-tab-pending')).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });

  it('writes the selected tab in the URL', async () => {
    const { router } = renderPage('/reports/structure-1');

    fireEvent.click(await screen.findByTestId('reports-tab-settle'));

    await waitFor(() =>
      expect(router.state.location.search).toBe('?tab=settled'),
    );
    expect(screen.getByTestId('reports-tab-settle')).toHaveAttribute(
      'aria-selected',
      'true',
    );
  });
});

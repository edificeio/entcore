import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { render, screen, waitFor } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { Component } from './ReportCase';

const CASE_ID = 'case-0000-4000-8000-000000000001';

const renderPage = () => {
  const router = createMemoryRouter(
    [{ path: 'reports/:structureid/cases/:caseId', element: <Component /> }],
    { initialEntries: [`/reports/structure-1/cases/${CASE_ID}`] },
  );
  return render(<RouterProvider router={router} />);
};

describe('ReportCase page', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  it('links back to the reports list of the structure', async () => {
    renderPage();

    expect(
      await screen.findByRole('link', {
        name: 'Retour à la liste des signalements',
      }),
    ).toHaveAttribute('href', '/reports/structure-1');
  });

  it('loads the case from the URL and names the page after it', async () => {
    renderPage();

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Signalement - blog - Retour sur la kermesse #1',
      }),
    ).toBeVisible();
    await waitFor(() =>
      expect(document.title).toMatch(/^Retour sur la kermesse #1 - /),
    );
  });
});

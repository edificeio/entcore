import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { render, screen, waitFor } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { Component } from './ReportCaseProcess';

const CASE_ID = 'case-0000-4000-8000-000000000001';

const renderPage = () => {
  const router = createMemoryRouter(
    [
      {
        path: 'reports/:structureid/cases/:caseId/process',
        element: <Component />,
      },
    ],
    { initialEntries: [`/reports/structure-1/cases/${CASE_ID}/process`] },
  );
  return render(<RouterProvider router={router} />);
};

describe('ReportCaseProcess page', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  it('names the page and its level 1 heading after the processing', async () => {
    renderPage();

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Traiter le signalement',
      }),
    ).toBeVisible();
    // The case card comes below, as a level 2 heading
    await screen.findByRole('heading', {
      level: 2,
      name: 'Signalement - blog - Retour sur la kermesse #1',
    });
    expect(screen.getAllByRole('heading', { level: 1 })).toHaveLength(1);
    await waitFor(() =>
      expect(document.title).toMatch(
        /^Retour sur la kermesse #1 - Traiter le signalement - /,
      ),
    );
  });
});

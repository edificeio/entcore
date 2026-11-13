import { beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { render, screen } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { ReportCaseDetails } from './ReportCaseDetails';

/** Mock case n°1: "Retour sur la kermesse #1" (blog), reported twice. */
const CASE_ID = 'case-0000-4000-8000-000000000001';

const renderDetails = (caseId = CASE_ID) =>
  render(<ReportCaseDetails caseId={caseId} structureId="structure-1" />);

describe('ReportCaseDetails', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  it('shows the case card as the page heading, without its consult button', async () => {
    renderDetails();

    expect(
      await screen.findByRole('heading', {
        level: 1,
        name: 'Signalement - blog - Retour sur la kermesse #1',
      }),
    ).toBeVisible();
    expect(screen.getByTestId('reports-case-card')).toBeVisible();
    expect(
      screen.queryByTestId('reports-case-consult-button'),
    ).not.toBeInTheDocument();
  });

  it('opens the received reports tab by default, with its count', async () => {
    renderDetails();

    const tabs = await screen.findAllByRole('tab');
    expect(tabs.map((tab) => tab.textContent)).toEqual([
      'Signalements reçus (2)',
      'Copie du contenu',
      'Historique',
    ]);
    expect(tabs[0]).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tabpanel')).toHaveAttribute(
      'aria-labelledby',
      tabs[0].id,
    );
  });

  it('switches to another tab on click', async () => {
    renderDetails();

    const history = await screen.findByRole('tab', { name: 'Historique' });
    history.click();

    expect(
      await screen.findByRole('tab', { name: 'Historique', selected: true }),
    ).toBeVisible();
    expect(screen.getByRole('tabpanel')).toHaveAttribute(
      'aria-labelledby',
      history.id,
    );
  });

  it('shows an error when the case cannot be loaded', async () => {
    renderDetails('unknown-case');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Impossible de charger le signalement.',
    );
    expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
  });
});

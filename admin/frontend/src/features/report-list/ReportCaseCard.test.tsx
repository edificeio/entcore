import { describe, expect, it } from 'vitest';

import { type Case } from '~/models';
import { render, screen } from '~/mocks/setup';
import { createMockCases } from '~/mocks/moderation/data';
import { ReportCaseCard } from './ReportCaseCard';

const TITLE_ID = 'case-title';

const buildCase = (overrides: Partial<Case> = {}): Case => ({
  ...createMockCases(1)[0],
  title: 'Retour sur la kermesse',
  status: 'OPEN',
  authorDisplayName: 'GROUDER Lucas',
  reports: [
    {
      id: 'r1',
      reporterId: 'u1',
      displayName: 'NOKA Louis',
      reason: 'HARASSMENT',
      comment: '',
      createdAt: '2026-11-05T10:00:00.000Z',
    },
    {
      id: 'r2',
      reporterId: 'u2',
      displayName: 'ROUX Léa',
      reason: 'HATRED',
      comment: '',
      createdAt: '2026-11-05T11:00:00.000Z',
    },
    {
      id: 'r3',
      reporterId: 'u1',
      displayName: 'NOKA Louis',
      reason: 'HARASSMENT',
      comment: '',
      createdAt: '2026-11-05T12:00:00.000Z',
    },
  ],
  ...overrides,
});

describe('ReportCaseCard', () => {
  it('renders the case title as a heading labelled by the given id', async () => {
    render(<ReportCaseCard reportCase={buildCase()} titleId={TITLE_ID} />);

    const heading = await screen.findByRole('heading', {
      name: 'Retour sur la kermesse',
    });
    expect(heading).toHaveAttribute('id', TITLE_ID);
  });

  it('lists each reason and each reporter once', async () => {
    render(<ReportCaseCard reportCase={buildCase()} titleId={TITLE_ID} />);

    expect(
      await screen.findByText('Harcèlement, Incitation à la haine'),
    ).toBeVisible();
    expect(screen.getByText('NOKA Louis, ROUX Léa')).toBeVisible();
    expect(screen.getByText('GROUDER Lucas')).toBeVisible();
  });

  it('shows the processing state of the case', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({ status: 'RESOURCE_KEPT' })}
        titleId={TITLE_ID}
      />,
    );

    expect(await screen.findByTestId('reports-case-status')).toHaveTextContent(
      'Contenu conservé',
    );
  });

  it('describes the consult button with the case title', async () => {
    render(<ReportCaseCard reportCase={buildCase()} titleId={TITLE_ID} />);

    expect(
      await screen.findByRole('button', { name: 'Consulter le signalement' }),
    ).toHaveAttribute('aria-describedby', TITLE_ID);
  });
});

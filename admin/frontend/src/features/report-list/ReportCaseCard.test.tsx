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

  it('still renders a case whose status and reason are unknown to the front', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({
          status: 'ARCHIVED' as Case['status'],
          reports: [
            {
              id: 'r1',
              reporterId: 'u1',
              displayName: 'NOKA Louis',
              reason: 'SPAM' as Case['reason'],
              comment: '',
              createdAt: '2026-11-05T10:00:00.000Z',
            },
          ],
        })}
        titleId={TITLE_ID}
      />,
    );

    expect(await screen.findByTestId('reports-case-status')).toHaveTextContent(
      'ARCHIVED',
    );
    expect(screen.getByText('SPAM')).toBeVisible();
  });

  it('keeps homonyms apart and skips reporters without a name', async () => {
    const report = buildCase().reports[0];
    render(
      <ReportCaseCard
        reportCase={buildCase({
          reports: [
            {
              ...report,
              id: 'r1',
              reporterId: 'u1',
              displayName: 'NOKA Louis',
            },
            {
              ...report,
              id: 'r2',
              reporterId: 'u2',
              displayName: 'NOKA Louis',
            },
            { ...report, id: 'r3', reporterId: 'u3', displayName: '' },
          ],
        })}
        titleId={TITLE_ID}
      />,
    );

    expect(await screen.findByText('NOKA Louis, NOKA Louis')).toBeVisible();
  });

  it('shows a placeholder when the case has no report', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({ reports: [] })}
        titleId={TITLE_ID}
      />,
    );

    expect(await screen.findAllByText('—')).toHaveLength(2);
  });

  it('describes the consult button with the case title', async () => {
    render(<ReportCaseCard reportCase={buildCase()} titleId={TITLE_ID} />);

    expect(
      await screen.findByRole('button', { name: 'Consulter le signalement' }),
    ).toHaveAttribute('aria-describedby', TITLE_ID);
  });
});

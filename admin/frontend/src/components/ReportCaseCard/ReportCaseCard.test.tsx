import { describe, expect, it } from 'vitest';

import { type Case } from '~/models';
import { render, screen } from '~/mocks/setup';
import { createMockCases } from '~/mocks/moderation/data';
import { ReportCaseCard } from './ReportCaseCard';

const TITLE_ID = 'case-title';

const buildCase = (overrides: Partial<Case> = {}): Case => ({
  ...createMockCases(1)[0],
  module: 'conversation',
  title: 'Retour sur la kermesse',
  authorId: 'author-1',
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
  it('titles the card with the application and the content title', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase()}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    const heading = await screen.findByRole('heading', {
      // The platform common bundle is not served in tests: the app name stays its key
      name: 'Signalement - conversation - Retour sur la kermesse',
    });
    expect(heading).toHaveAttribute('id', TITLE_ID);
  });

  it('lists each reason and each reporter once', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase()}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    expect(
      await screen.findByText('Harcèlement, Incitation à la haine'),
    ).toBeVisible();
    expect(
      screen
        .getAllByTestId('reports-case-reporter-link')
        .map((link) => link.textContent),
    ).toEqual(['NOKA Louis', 'ROUX Léa']);
  });

  it('links the author to the admin console and reporters to the directory', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase()}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    expect(
      await screen.findByRole('link', { name: 'GROUDER Lucas' }),
    ).toHaveAttribute('href', '/admin/structure-1/users/list/author-1/details');
    const [firstReporter] = screen.getAllByTestId('reports-case-reporter-link');
    expect(firstReporter.getAttribute('href')).toContain('u1');
  });

  it('shows the processing state of the case', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({ status: 'RESOURCE_KEPT' })}
        titleId={TITLE_ID}
        structureId="structure-1"
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
        structureId="structure-1"
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
        structureId="structure-1"
      />,
    );

    const reporters = await screen.findAllByTestId(
      'reports-case-reporter-link',
    );
    expect(reporters.map((link) => link.textContent)).toEqual([
      'NOKA Louis',
      'NOKA Louis',
    ]);
  });

  it('keeps every reporter available to screen readers when the list is cut', async () => {
    const report = buildCase().reports[0];
    const reports = Array.from({ length: 12 }, (_, i) => ({
      ...report,
      id: `r${i}`,
      reporterId: `u${i}`,
      displayName: `REPORTER ${i}`,
    }));
    render(
      <ReportCaseCard
        reportCase={buildCase({ reports })}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    // The cut is visual only (line clamp): no reporter leaves the accessibility tree
    expect(
      await screen.findByRole('link', { name: 'REPORTER 11' }),
    ).toBeInTheDocument();
    expect(screen.getAllByTestId('reports-case-reporter-link')).toHaveLength(
      12,
    );
  });

  it('shows a placeholder when the case has no report', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({ reports: [] })}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    expect(await screen.findAllByText('—')).toHaveLength(2);
  });

  const decisionBy = (
    status: Case['status'],
    createdAt = '2026-11-06T14:30:00',
  ): NonNullable<Case['decisions']>[number] => ({
    id: `d-${status}`,
    userId: 'adml-1',
    displayName: 'MARTIN Paul',
    status,
    comment: '',
    createdAt,
  });

  it.each([
    ['QUARANTINE', 'mis en quarantaine'],
    ['RESOURCE_KEPT', 'conservé'],
    ['RESOURCE_DELETED', 'supprimé'],
  ] as const)(
    'tells when and by whom the %s action was taken',
    async (status, action) => {
      render(
        <ReportCaseCard
          reportCase={buildCase({ status, decisions: [decisionBy(status)] })}
          titleId={TITLE_ID}
          structureId="structure-1"
        />,
      );

      expect(
        await screen.findByTestId('reports-case-decision'),
      ).toHaveTextContent(
        `Contenu ${action} le 06/11/2026 à 14:30 par MARTIN Paul.`,
      );
    },
  );

  it('tells the decision behind the current status, not an earlier one', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({
          status: 'RESOURCE_KEPT',
          decisions: [
            decisionBy('QUARANTINE', '2026-11-06T14:30:00'),
            decisionBy('RESOURCE_KEPT', '2026-11-07T09:15:00'),
          ],
        })}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    expect(
      await screen.findByTestId('reports-case-decision'),
    ).toHaveTextContent(
      'Contenu conservé le 07/11/2026 à 09:15 par MARTIN Paul.',
    );
  });

  it('follows the latest decision by date, whatever their order', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({
          status: 'RESOURCE_DELETED',
          decisions: [
            decisionBy('RESOURCE_DELETED', '2026-11-08T16:45:00'),
            decisionBy('QUARANTINE', '2026-11-06T14:30:00'),
          ],
        })}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    expect(
      await screen.findByTestId('reports-case-decision'),
    ).toHaveTextContent(
      'Contenu supprimé le 08/11/2026 à 16:45 par MARTIN Paul.',
    );
    expect(screen.getByTestId('reports-case-status')).toHaveTextContent(
      'Contenu supprimé',
    );
  });

  it('shows the status of the latest decision on the chip', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({
          // Case status not yet updated after the deletion
          status: 'QUARANTINE',
          decisions: [
            decisionBy('QUARANTINE', '2026-11-06T14:30:00'),
            decisionBy('RESOURCE_DELETED', '2026-11-08T16:45:00'),
          ],
        })}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    expect(await screen.findByTestId('reports-case-status')).toHaveTextContent(
      'Contenu supprimé',
    );
    expect(screen.getByTestId('reports-case-decision')).toHaveTextContent(
      'Contenu supprimé le 08/11/2026 à 16:45 par MARTIN Paul.',
    );
  });

  it('has no decision line while no action was taken', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase({
          status: 'PENDING',
          decisions: [decisionBy('PENDING')],
        })}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    await screen.findByTestId('reports-case-status');
    expect(
      screen.queryByTestId('reports-case-decision'),
    ).not.toBeInTheDocument();
  });

  it('describes the consult button with the case title', async () => {
    render(
      <ReportCaseCard
        reportCase={buildCase()}
        titleId={TITLE_ID}
        structureId="structure-1"
      />,
    );

    expect(
      await screen.findByRole('button', { name: 'Consulter le signalement' }),
    ).toHaveAttribute('aria-describedby', TITLE_ID);
  });
});

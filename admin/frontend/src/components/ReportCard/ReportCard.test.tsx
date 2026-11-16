import { describe, expect, it } from 'vitest';

import { render, screen } from '~/mocks/setup';
import { type Reason, type Report } from '~/models';
import { ReportCard } from './ReportCard';

const buildReport = (overrides: Partial<Report> = {}): Report => ({
  id: 'r1',
  reporterId: 'u1',
  displayName: 'MARTIN Léa',
  reason: 'VIOLENT_SCORNFUL',
  comment: 'Ligne 1\nLigne 2',
  createdAt: '2026-09-28T14:02:00',
  ...overrides,
});

describe('ReportCard', () => {
  it('links the reporter name to the directory', async () => {
    render(<ReportCard report={buildReport()} />);

    const link = await screen.findByRole('link', { name: 'MARTIN Léa' });
    expect(link.getAttribute('href')).toContain('u1');
    expect(link.getAttribute('href')).not.toContain('/admin/');
  });

  it('shows the report date, time and reason', async () => {
    render(<ReportCard report={buildReport()} />);

    const card = await screen.findByTestId('reports-report-card');
    expect(card).toHaveTextContent('Signalé le : 28/09/2026 à 14:02');
    expect(card).toHaveTextContent(
      'Motif du signalement : Contenu violent ou méprisant',
    );
  });

  it('keeps the line breaks of the comment', async () => {
    render(<ReportCard report={buildReport()} />);

    const comment = await screen.findByTestId('reports-report-comment');
    expect(comment.textContent).toBe('Ligne 1\nLigne 2');
    expect(comment).toHaveStyle({ whiteSpace: 'pre-line' });
  });

  it('shows a dash for an empty comment', async () => {
    render(<ReportCard report={buildReport({ comment: '' })} />);

    expect(
      await screen.findByTestId('reports-report-comment'),
    ).toHaveTextContent('—');
  });

  it('shows a dash without link for a reporter without name', async () => {
    render(<ReportCard report={buildReport({ displayName: '' })} />);

    expect(
      await screen.findByTestId('reports-report-reporter'),
    ).toHaveTextContent('—');
    expect(
      screen.queryByTestId('reports-report-reporter-link'),
    ).not.toBeInTheDocument();
  });

  it('puts the comment under its label', async () => {
    render(<ReportCard report={buildReport()} />);

    const comment = await screen.findByTestId('reports-report-comment');
    const label = comment.previousElementSibling;
    expect(label).toHaveTextContent('Commentaire :');
    // Both are blocks: the comment starts on the line after its label
    expect(label).not.toHaveStyle({ display: 'inline' });
    expect(comment).not.toHaveStyle({ display: 'inline' });
  });

  it('draws a horizontal separator on small screens', async () => {
    // No matchMedia in jsdom: the card renders as on screens below md
    render(<ReportCard report={buildReport()} />);

    expect(await screen.findByRole('separator')).not.toHaveAttribute(
      'aria-orientation',
      'vertical',
    );
  });

  it('shows an unknown reason as is', async () => {
    render(<ReportCard report={buildReport({ reason: 'SPAM' as Reason })} />);

    expect(await screen.findByTestId('reports-report-card')).toHaveTextContent(
      'Motif du signalement : SPAM',
    );
  });
});

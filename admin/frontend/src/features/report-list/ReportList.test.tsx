import { describe, expect, it } from 'vitest';

import { render, screen } from '~/mocks/setup';
import { type Report } from '~/models';
import { ReportList } from './ReportList';

const report = (
  id: string,
  displayName: string,
  createdAt: string,
): Report => ({
  id,
  reporterId: `user-${id}`,
  displayName,
  reason: 'HATRED',
  comment: '',
  createdAt,
});

describe('ReportList', () => {
  it('lists the reports from the most recent', async () => {
    render(
      <ReportList
        reports={[
          report('1', 'A', '2026-09-28T10:00:00'),
          report('2', 'B', '2026-09-28T12:00:00'),
          report('3', 'C', '2026-09-28T11:00:00'),
        ]}
      />,
    );

    const links = await screen.findAllByTestId('reports-report-reporter-link');
    expect(links.map((link) => link.textContent)).toEqual(['B', 'C', 'A']);
    expect(screen.getAllByRole('listitem')).toHaveLength(3);
  });

  it('keeps an explicit list role on the unstyled list', async () => {
    render(<ReportList reports={[report('1', 'A', '2026-09-28T10:00:00')]} />);

    // Safari drops the list semantics of a list without bullets
    expect(await screen.findByRole('list')).toHaveAttribute('role', 'list');
  });
});

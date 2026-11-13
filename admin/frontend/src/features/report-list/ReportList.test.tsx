import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { server } from '~/mocks/server';
import { fireEvent, render, screen, waitFor } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { moderationBaseUrl } from '~/services/api';
import { ReportList } from './ReportList';

describe('ReportList', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  it('renders the first page of cases as a feed', async () => {
    render(<ReportList workflowState="PENDING" structureId="structure-1" />);

    const articles = await screen.findAllByRole('article');
    expect(articles).toHaveLength(20);
    expect(articles[0]).toHaveAttribute('aria-posinset', '1');
    expect(articles[0]).toHaveAttribute('aria-setsize', '31');
  });

  it('renders the empty state when there is no case', async () => {
    server.use(
      http.post(`${moderationBaseUrl}/cases/_search`, () =>
        HttpResponse.json({ totalCount: 0, page: 0, size: 20, items: [] }),
      ),
    );

    render(<ReportList workflowState="SETTLE" structureId="structure-1" />);

    expect(await screen.findByTestId('reports-empty-state')).toBeVisible();
    expect(screen.queryByRole('feed')).not.toBeInTheDocument();
  });

  it('loads the next page when keyboard focus reaches the end of the list', async () => {
    render(<ReportList workflowState="PENDING" structureId="structure-1" />);
    const articles = await screen.findAllByRole('article');

    articles[articles.length - 1].focus();

    await waitFor(() =>
      expect(screen.getAllByRole('article')).toHaveLength(31),
    );
  });

  it('moves focus between cards with Page Down / Page Up', async () => {
    render(<ReportList workflowState="PENDING" structureId="structure-1" />);
    const articles = await screen.findAllByRole('article');

    articles[0].focus();
    fireEvent.keyDown(articles[0], { key: 'PageDown' });
    expect(articles[1]).toHaveFocus();

    fireEvent.keyDown(articles[1], { key: 'PageUp' });
    expect(articles[0]).toHaveFocus();
  });
});

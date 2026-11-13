import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';

import { resetModerationMocks } from '~/mocks/moderation/handlers';
import { renderWithRouter } from '~/mocks/renderWithRouter';
import { server } from '~/mocks/server';
import { fireEvent, screen, waitFor } from '~/mocks/setup';
import { type WorkflowState } from '~/models';
import { queryClient } from '~/providers';
import { moderationBaseUrl } from '~/services/api';
import { ReportCaseList } from './ReportCaseList';

/** The cards link to the case detail page: the list needs a router around it. */
const renderList = (workflowState: WorkflowState) =>
  renderWithRouter(
    '/',
    <ReportCaseList workflowState={workflowState} structureId="structure-1" />,
  );

describe('ReportCaseList', () => {
  beforeEach(() => {
    resetModerationMocks();
    queryClient.clear();
  });

  it('renders the first page of cases as a feed', async () => {
    renderList('PENDING');

    const articles = await screen.findAllByRole('article');
    expect(articles).toHaveLength(20);
    expect(articles[0]).toHaveAttribute('aria-posinset', '1');
    expect(articles[0]).toHaveAttribute('aria-setsize', '31');
  });

  it('links each card to the case detail page', async () => {
    renderList('PENDING');

    const [firstConsult] = await screen.findAllByRole('link', {
      name: 'Consulter le signalement',
    });
    expect(firstConsult).toHaveAttribute(
      'href',
      '/reports/structure-1/cases/case-0000-4000-8000-000000000001',
    );
  });

  it('renders the empty state when there is no case', async () => {
    server.use(
      http.post(`${moderationBaseUrl}/cases/_search`, () =>
        HttpResponse.json({ totalCount: 0, page: 0, size: 20, items: [] }),
      ),
    );

    renderList('SETTLE');

    expect(await screen.findByTestId('reports-empty-state')).toBeVisible();
    expect(screen.queryByRole('feed')).not.toBeInTheDocument();
  });

  it('loads the next page when keyboard focus reaches the end of the list', async () => {
    renderList('PENDING');
    const articles = await screen.findAllByRole('article');

    articles[articles.length - 1].focus();

    await waitFor(() =>
      expect(screen.getAllByRole('article')).toHaveLength(31),
    );
  });

  it('moves focus between cards with Page Down / Page Up', async () => {
    renderList('PENDING');
    const articles = await screen.findAllByRole('article');

    articles[0].focus();
    fireEvent.keyDown(articles[0], { key: 'PageDown' });
    expect(articles[1]).toHaveFocus();

    fireEvent.keyDown(articles[1], { key: 'PageUp' });
    expect(articles[0]).toHaveFocus();
  });
});

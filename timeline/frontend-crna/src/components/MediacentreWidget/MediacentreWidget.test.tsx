import userEvent from '@testing-library/user-event';
import { HttpResponse, http } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { server } from '~/mocks/server';
import { render, screen } from '~/mocks/setup';
import { queryClient } from '~/providers';
import { MediacentreWidget } from './MediacentreWidget';

// The app QueryClient is a singleton shared by every test: drop cached responses between tests.
beforeEach(() => queryClient.clear());

const mockMediacentre = () => {
  let signetsRequestCount = 0;
  server.use(
    http.get('/mediacentre/favorites', () =>
      HttpResponse.json({ status: 'ok', data: [] }),
    ),
    http.get('/mediacentre/signets', () => {
      signetsRequestCount++;
      return HttpResponse.json({
        status: 'ok',
        data: {
          signets: {
            source: 'fr.openent.mediacentre.source.Signet',
            resources: [
              {
                id: '42',
                title: 'Onisep',
                image: '',
                link: 'https://www.onisep.fr',
                plain_text: [],
                document_types: ['Orientation'],
              },
              {
                id: '43',
                title: 'Not orientation',
                image: '',
                link: 'https://example.com',
                plain_text: [],
                document_types: ['Signet'],
              },
            ],
          },
        },
      });
    }),
  );
  return { getSignetsRequestCount: () => signetsRequestCount };
};

describe('MediacentreWidget — orientation section', () => {
  it('fetches and lists orientation resources only once the tab is opened', async () => {
    const { getSignetsRequestCount } = mockMediacentre();
    render(<MediacentreWidget />);

    const orientationTab = await screen.findByRole('button', {
      name: 'Orientation',
    });
    expect(getSignetsRequestCount()).toBe(0);

    await userEvent.click(orientationTab);

    expect(await screen.findByRole('link', { name: /Onisep/ })).toHaveAttribute(
      'href',
      'https://www.onisep.fr',
    );
    expect(screen.queryByText('Not orientation')).not.toBeInTheDocument();
    expect(getSignetsRequestCount()).toBe(1);
  });

  it('shows an empty state when there is no orientation resource', async () => {
    server.use(
      http.get('/mediacentre/favorites', () =>
        HttpResponse.json({ status: 'ok', data: [] }),
      ),
      http.get('/mediacentre/signets', () =>
        HttpResponse.json({ status: 'ok', data: {} }),
      ),
    );
    render(<MediacentreWidget />);

    await userEvent.click(
      await screen.findByRole('button', { name: 'Orientation' }),
    );

    expect(
      await screen.findByText("Aucune ressource d'orientation"),
    ).toBeInTheDocument();
  });
});

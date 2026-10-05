import { HttpResponse, http, type JsonBodyType } from 'msw';
import { describe, expect, it } from 'vitest';
import type { MediacentrePublishedSignet } from '~/models/mediacentre';
import { server } from '~/mocks/server';
import '~/mocks/setup';
import { fetchMediacentreOrientation } from './mediacentre.api';

const buildSignet = (
  overrides: Partial<MediacentrePublishedSignet>,
): MediacentrePublishedSignet => ({
  id: '1',
  title: 'Signet',
  image: '/workspace/document/fake-image',
  link: 'https://example.com/signet',
  plain_text: [],
  document_types: ['Signet'],
  ...overrides,
});

const mockSignetsResponse = (body: JsonBodyType) =>
  server.use(http.get('/mediacentre/signets', () => HttpResponse.json(body)));

describe('fetchMediacentreOrientation', () => {
  it('returns only orientation signets, mapped to list items', async () => {
    mockSignetsResponse({
      status: 'ok',
      data: {
        signets: {
          source: 'fr.openent.mediacentre.source.Signet',
          resources: [
            buildSignet({
              id: '42',
              title: 'Onisep',
              link: 'https://www.onisep.fr',
              plain_text: ['métiers', 'formations'],
              document_types: ['Orientation'],
            }),
            buildSignet({ id: '43', title: 'Not orientation' }),
          ],
        },
      },
    });

    await expect(fetchMediacentreOrientation()).resolves.toEqual([
      {
        id: '42',
        label: 'Onisep',
        sublabel: 'métiers, formations',
        href: 'https://www.onisep.fr',
        imageUrl: '/workspace/document/fake-image',
      },
    ]);
  });

  it('returns an empty list when no signet is published', async () => {
    mockSignetsResponse({ status: 'ok', data: {} });

    await expect(fetchMediacentreOrientation()).resolves.toEqual([]);
  });

  it('throws when Mediacentre answers with an error status', async () => {
    mockSignetsResponse({ status: 'ko', data: {} });

    await expect(fetchMediacentreOrientation()).rejects.toThrow(
      'mediacentre.widget.orientation.fetch.error',
    );
  });
});

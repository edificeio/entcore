import { HttpResponse, http } from 'msw';
import { mockLanguages, mockThemes } from '../data/customize';

export const handlers = [
  http.get('/locale', () => HttpResponse.json({ locale: 'fr' })),

  http.get('/applications-list', () =>
    HttpResponse.json({
      apps: [
        {
          name: 'FakeApp',
          address: '/fake',
          icon: 'fake-large',
          target: '',
          displayName: 'fake',
          display: true,
          prefix: '/fake',
          casType: null,
          scope: [''],
          isExternal: false,
        },
      ],
    }),
  ),

  http.get('/languages', () => HttpResponse.json(mockLanguages)),

  http.get('/themes', () => HttpResponse.json(mockThemes)),
];

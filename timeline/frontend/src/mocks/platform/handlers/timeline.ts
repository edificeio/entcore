import { HttpResponse, http } from 'msw';
import { mockNotifications, mockNotificationTypes } from '../data/notification';
import { mockFlashMessages } from '../data/flashMessage';

const PAGE_SIZE = 10;

export const handlers = [
  http.get('/timeline/lastNotifications', ({ request }) => {
    const url = new URL(request.url);
    const page = Number(url.searchParams.get('page') ?? '0');
    const types = url.searchParams.getAll('type');

    const filtered = types.length
      ? mockNotifications.filter((n) => types.includes(n.type))
      : mockNotifications;

    const start = page * PAGE_SIZE;
    const results =
      start < filtered.length ? filtered.slice(start, start + PAGE_SIZE) : [];

    return HttpResponse.json({
      status: 'ok',
      number: results.length,
      results,
    });
  }),

  http.get('/timeline/types', () => HttpResponse.json(mockNotificationTypes)),

  http.put('/timeline/:id/report', () => HttpResponse.json({ status: 'ok' })),

  http.put('/timeline/:id', () => HttpResponse.json({ status: 'ok' })),

  http.get('/timeline/flashmsg/listuser', () =>
    HttpResponse.json(mockFlashMessages),
  ),

  http.put('/timeline/flashmsg/:id/markasread', () =>
    HttpResponse.json({ status: 'ok' }),
  ),
];

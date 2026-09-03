import { HttpResponse, http } from 'msw';
import { mockUserId } from '../data/userinfo';
import { getPreference, setPreference } from '../data/preferences';

const FAKE_AVATAR =
  'data:image/svg+xml;base64,' +
  btoa(
    '<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100"><rect width="100" height="100" fill="#ccc"/></svg>',
  );

export const handlers = [
  http.get('/userbook/api/person', () =>
    HttpResponse.json({
      status: 'ok',
      result: [
        {
          id: mockUserId,
          login: 'fake.user',
          displayName: 'Fake User',
          type: ['Personnel'],
          visibleInfos: [],
          schools: [
            {
              exports: null,
              classes: [],
              name: 'Fake School',
              id: 'd4c3b2a1',
              UAI: null,
            },
          ],
          relatedName: null,
          relatedId: null,
          relatedType: null,
          userId: mockUserId,
          motto: 'Always Learning',
          photo: `/userbook/avatar/${mockUserId}`,
          mood: 'happy',
          health: 'good',
          address: '123 Fake Street',
          email: 'fake.user@example.com',
          tel: '1234567890',
          mobile: '0987654321',
          birthdate: '1990-01-01',
          hobbies: ['reading', 'coding'],
        },
      ],
    }),
  ),

  http.get('/userbook/avatar/:id', () =>
    HttpResponse.text(FAKE_AVATAR, {
      headers: { 'Content-Type': 'image/svg+xml' },
    }),
  ),

  // Generic userbook preference store: GET returns the current value, PUT
  // overwrites it — same key ('apps', 'language', 'rgpdCookies', 'timeline',
  // 'widgets'...) so the notification filter and widget layout persist
  // across refetches, exactly like the real backend.
  http.get('/userbook/preference/:key', ({ params }) => {
    const { key } = params as { key: string };
    return HttpResponse.json({
      preference: JSON.stringify(getPreference(key)),
    });
  }),

  http.put('/userbook/preference/:key', async ({ params, request }) => {
    const { key } = params as { key: string };
    const body = await request.json();
    setPreference(key, body);
    return HttpResponse.json({ status: 'ok' });
  }),

  http.put('/userbook/api/preferences', () =>
    HttpResponse.json({ status: 'ok' }),
  ),
];

import { HttpResponse, http } from 'msw';
import { baseUrl } from '~/services';
import { mockFullMessage, mockSentMessage } from '..';

/**
 * MSW Handlers
 * Mock HTTP methods for folder service
 */
export const messageHandlers = [
  // Message service
  http.get(`${baseUrl}/api/messages/:messageId`, () => {
    // This also covers the ?originalFormat=true query parameter
    return HttpResponse.json(mockFullMessage, { status: 200 });
  }),
  http.post<
    object,
    {
      id: string[];
      unread: boolean;
    }
  >(`${baseUrl}/toggleUnread`, async ({ request }) => {
    const payload = await request.json();
    if (
      !payload ||
      !Array.isArray(payload.id) ||
      typeof payload.unread !== 'boolean'
    ) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.put<
    object,
    {
      id: string[];
    }
  >(`${baseUrl}/restore`, async ({ request }) => {
    const payload = await request.json();
    if (!payload || !Array.isArray(payload.id)) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.put<
    object,
    {
      id: string[];
    }
  >(`${baseUrl}/delete`, async ({ request }) => {
    const payload = await request.json();
    if (!payload || !Array.isArray(payload.id)) {
      return HttpResponse.text('Bad Request', { status: 400 });
    }
    return HttpResponse.text('', { status: 200 });
  }),
  http.post(`${baseUrl}/draft`, () => {
    return HttpResponse.json({ id: 'message_draft' }, { status: 201 });
  }),
  http.put(`${baseUrl}/draft/:draftId`, () => {
    return HttpResponse.text('', { status: 200 });
  }),
  http.post(`${baseUrl}/send`, async ({ request }) => {
    const url = new URL(request.url);
    const id = url.searchParams.get('id');
    const payload = await request.json();
    if (!id) {
      return HttpResponse.text('Bad Request', {
        status: 400,
      });
    }
    return HttpResponse.json(Object.assign({ id }, mockSentMessage, payload), {
      status: 200,
    });
  }),
];
